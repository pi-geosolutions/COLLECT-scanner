package scanner.dao;

import java.sql.Types;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.LocaleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import scanner.dao.utils.ResultsetMetadataExtractor;
import scanner.dao.utils.SQLColumn;
import scanner.dao.utils.UpsertResult;

@Component
public class PostgresqlPublisher implements DbPublisher {
	private static final Logger logger = LoggerFactory.getLogger(PostgresqlPublisher.class);

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Value("${parsing.locale:en_US}")
	private String parsing_locale;

	@Value("${db.locale:en_US}")
	private String db_locale;

	@Value("${db.fields.ignorecase:false}")
	private boolean ignorefieldscase;

	@Value("${db.schema:collect}")
	private String db_schema;

	private Map<String, SQLColumn> fieldsMapper;
	private List<String> headers;

	@Override
	@Transactional
	public int publish(String tablename, List<String> headers, List<List<String>> data) throws ParseException {
		int results = 0;

		// Load table metadata (for proper field parsing)
		this.fieldsMapper = this.loadTableMetadata(tablename);
		this.headers = headers;

		// build complete tablename
		tablename = getFullTablename(tablename);
		for (List<String> row : data) {
			String req = buildInsertReq(tablename, headers, row);

			logger.debug(req);
			int result = jdbcTemplate.update(req);
			results += result;

		}
		logger.debug("Inserted " + results + " new row(s)");

		return results;

	}

	@Override
	@Transactional
	public UpsertResult publish(String tablename, List<String> primarykeys, List<String> headers,
			List<List<String>> data, boolean forceUpdate) throws ParseException {
		// applying new postgresql 9.5+ "upsert" model
		//
		int i;
		UpsertResult results = new UpsertResult();

		// Load table metadata (for proper field parsing)
		this.fieldsMapper = this.loadTableMetadata(tablename);
		this.headers = headers;

		// build complete tablename
		tablename = getFullTablename(tablename);
		for (List<String> row : data) {
			// builds primary key tuple
			String pkeys_tuple = "";
			for (String pkey : primarykeys) {
				i = headers.indexOf(pkey);
				pkeys_tuple += "\"" + pkey + "\",";
			}
			pkeys_tuple = trimLastChar(pkeys_tuple);

			// builds update clause
			String update_fields = "";
			for (String header : headers) {
				if (!primarykeys.contains(header)) {
					update_fields += quote(header) + "=EXCLUDED." + quote(header) + ",";
				}
			}
			update_fields = trimLastChar(update_fields);

			// first get standard INSERT request
			String req = buildInsertReq(tablename, headers, row);
			// drop last semicolon in order to append the rest
			req = trimLastChar(req);
			if (forceUpdate) {
				// add UPSERT specific part
				req += " ON CONFLICT (" + pkeys_tuple + ") DO UPDATE SET " + update_fields + ";";
			} else {
				req += " ON CONFLICT (" + pkeys_tuple + ") DO NOTHING;";
			}

			logger.debug(req);
			int result = jdbcTemplate.update(req);
			results.addResult(result);
			logger.debug("upsert result (0=update, 1=insert): " + result);
		}

		return results;
	}

	private String buildInsertReq(String tablename, List<String> headers, List<String> row) throws ParseException {
		String req = " INSERT INTO " + tablename + "(";
		for (String header : headers) {
			req += "\"" + header + "\", ";
		}
		req = req.substring(0, req.length() - 2); // drop the last ,
		req += ")" + " VALUES ( ";
		for (int idx = 0; idx < row.size(); idx++) {
			req += this.getValue(row.get(idx), idx) + ", ";
		}
		req = req.substring(0, req.length() - 2); // drop the last ,
		req += ");";
		return req;
	}

	private String getValue(String value, int index) throws ParseException {
		value = value.trim();
		if (value.isEmpty()) {
			return null;
		}
		if (this.fieldsMapper != null) {
			int type = this.getValueType(value, index);
			if (type != 0) {
				value = formatValue(value, type);
			}
		}
		return value;
	}

	private String formatValue(String value, int valuetype) throws ParseException {
		switch (valuetype) {
		// switch upon java.sql.Types values
		case Types.INTEGER:
		case Types.BIT:
		case Types.SMALLINT:
		case Types.BIGINT:
		case Types.NUMERIC:
		case Types.DECIMAL:
			logger.debug("Parsing " + value + " as integer value");
			break;
		case Types.REAL:
		case Types.FLOAT:
		case Types.DOUBLE:
			Locale locale = Locale.US;
			if (!this.parsing_locale.isEmpty()) {
				locale = LocaleUtils.toLocale(parsing_locale);
			}
			Locale dblocale = Locale.US;
			if (!this.db_locale.isEmpty()) {
				dblocale = LocaleUtils.toLocale(db_locale);
			}
			logger.debug("Parsing " + value + " as float/double using locale " + locale.toString());
			NumberFormat numberFormat = NumberFormat.getInstance(locale);
			Number nb = numberFormat.parse(value);
			NumberFormat dbnumberFormat = NumberFormat.getInstance(Locale.US); // always
																				// note
																				// decimals
																				// with
																				// a
																				// dot
																				// (.),
																				// not
																				// a
																				// comma,
																				// whatever
																				// the
																				// locale
																				// is
			dbnumberFormat.setGroupingUsed(false); // US uses comma to mark
													// thousands.... this
													// disables it
			value = dbnumberFormat.format(nb.doubleValue());
			break;
		case java.sql.Types.DATE:
		case java.sql.Types.TIME:
		case java.sql.Types.TIME_WITH_TIMEZONE:
		case java.sql.Types.TIMESTAMP:
		case java.sql.Types.TIMESTAMP_WITH_TIMEZONE:
			value = "'" + value + "'";
			logger.debug("Parsing " + value + " as date/timestamp");
			break;
		default:
			value = "'" + value + "'";
			logger.debug("Parsing " + value + " as string");
		}

		return value;
	}

	private int getValueType(String value, int index) {
		String fieldId = this.headers.get(index);
		SQLColumn col = this.fieldsMapper.get(fieldId);
		if (col == null) {
			return 0;
		}
		return col.getTypeCode();
	}

	private Map<String, SQLColumn> loadTableMetadata(String tablename) {
		// we don't need to get real data, we just need a query, to get the
		// metadata
		String simplequery = "SELECT * FROM " + getFullTablename(tablename) + " LIMIT 1;";
		return jdbcTemplate.query(simplequery, new ResultsetMetadataExtractor(ignorefieldscase));
	}

	/**
	 * Buils full & escaped tablename : appends the schema and double quotes
	 * 
	 * @param tablename
	 * @return
	 */
	private String getFullTablename(String tablename) {
		if (this.db_schema.isEmpty()) {
			return "\"" + tablename + "\"";
		}
		return "\"" + db_schema + "\".\"" + tablename + "\"";
	}

	/**
	 * surrounds a field name by double quotes
	 */
	private String quote(String field) {
		if (field.startsWith("\"")) {
			return field;
		}
		return "\"" + field + "\"";
	}

	private String trimLastChar(String s) {
		return s = s.substring(0, s.length() - 1);
	}

}
