package scanner.dao.utils;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.util.StringUtils;

public class TypeMappingPreparedStatementCreator implements PreparedStatementCreator {

	private static final Logger logger = LoggerFactory.getLogger(TypeMappingPreparedStatementCreator.class);

	private String tablename;
	private List<String> headers;
	private List<List<String>> data;
	private Map<String, SQLColumn> fieldsMapper;
	private Locale locale=Locale.US;
	private NumberFormat numberFormat;
	private String dateParsingFormat = "dd/MM/yyyy HH:mm:ss";

	public TypeMappingPreparedStatementCreator(String tablename, List<String> headers, List<List<String>> data,
			Map<String, SQLColumn> fieldsMapper) {
		this.tablename = tablename;
		this.headers = headers;
		this.data = data;
		this.fieldsMapper = fieldsMapper;
		this.numberFormat=NumberFormat.getInstance(this.locale);
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement ps = con.prepareStatement(buildRequest());
		Iterator<List<String>> it = data.iterator();
		int psSetIndex=1;
		while (it.hasNext()) {
			List<String> row = it.next();
			try {
				psSetIndex= this.setDataRow(row, ps, psSetIndex);
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return ps;
	}

	public String buildRequest() {
		String req = "INSERT INTO "+tablename+" (\""
				+ StringUtils.collectionToDelimitedString(headers, "\",\"") + "\") VALUES ";
		req += org.apache.commons.lang3.StringUtils.repeat(nthQMarksList(headers.size()) + ",", data.size() - 1);
		req += nthQMarksList(headers.size()) + ";";
		logger.debug("INSERT request: \n \t "+req);
		return req;
	}

	/*
	 * e.g. if length=4, will return "(?,?,?,?)"
	 */
	private String nthQMarksList(int length) {
		return "(" + org.apache.commons.lang3.StringUtils.repeat("?,", length - 1) + "?)";
	}

	private int setDataRow(List<String> row, PreparedStatement ps, int psSetIndex) throws NumberFormatException, SQLException, ParseException {
		int rowindex = 0;
		String log = "Inserting row: "; 
		for (String value : row) {
			int valuetype = getValueType(value, rowindex);
			log += " "+value+"("+valuetype+")("+psSetIndex+") ";
			setValue(value, valuetype, psSetIndex, ps);
			rowindex++;
			psSetIndex++;
		}
		logger.debug(log);
		return psSetIndex;
	}

	private void setValue(String value, int valuetype, int index, PreparedStatement ps) throws NumberFormatException, SQLException, ParseException {
		Number nb;
		switch (valuetype) {
		//switch upon java.sql.Types values
		case Types.INTEGER:
			logger.debug("Parsing "+value+" as integer");
			ps.setInt(index, Integer.parseInt(value));
			break;
		case Types.BIT:
			logger.debug("Parsing "+value+" as boolean");
			ps.setBoolean(index, Boolean.parseBoolean(value));
			break;
		case Types.SMALLINT:
			logger.debug("Parsing "+value+" as short");
			ps.setShort(index, Short.parseShort(value));
			break;
		case Types.BIGINT:
			logger.debug("Parsing "+value+" as long");
			ps.setLong(index, Long.parseLong(value));
			break;
		case Types.REAL:
			logger.debug("Parsing "+value+" as float using locale "+this.locale.toString());
			nb = this.numberFormat.parse(value);
			ps.setFloat(index, nb.floatValue());
			break;
		case Types.FLOAT: case Types.DOUBLE:
			logger.debug("Parsing "+value+" as double using locale "+this.locale.toString());
			nb = this.numberFormat.parse(value);
			ps.setDouble(index, nb.doubleValue());
			break;
		case Types.NUMERIC: case Types.DECIMAL:
			logger.debug("Parsing "+value+" as BigDecimal");
			ps.setBigDecimal(index, new BigDecimal(value));
			break;
		case java.sql.Types.DATE:
			logger.debug("Parsing "+value+" as date");
			ps.setDate(index, this.parseDate(value));
			break;
		default:
			logger.debug("Parsing "+value+" as string");
			ps.setString(index, value);
		}

	}

	private Date parseDate(String value) throws ParseException {
		DateFormat formatter = new SimpleDateFormat(dateParsingFormat);
		Date date = new java.sql.Date(formatter.parse(value).getTime());
		//System.out.println(date);
		return date;
	}

	private int getValueType(String value, int index) {
		String fieldId = this.headers.get(index);
		SQLColumn col = this.fieldsMapper.get(fieldId);
		if (col == null) {
			return 0;
		}
		return col.getTypeCode();
	}

	public void setDateParsingFormat(String parsing_dateformat) {
		this.dateParsingFormat=parsing_dateformat;
	}

	public void setLocale(Locale forLanguageTag) {
		this.locale = forLanguageTag;
		this.numberFormat = NumberFormat.getInstance(this.locale);
	}

}
