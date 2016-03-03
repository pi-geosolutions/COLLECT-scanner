package scanner.db.impl;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import scanner.db.DbPublisher;

@Component
public class PostgresqlPublisher implements DbPublisher {

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Value("${parsing.locale}")
	private String parsing_locale;
	@Value("${parsing.dateformat}")
	private String parsing_dateformat;

	@Value("${db.fields.ignorecase:false}")
	private boolean ignorefieldscase;
	@Value("${db.schema:public}")
	private String db_schema;

	public int publish(String tablename, List<String> headers, List<List<String>> data) {
		// build complete tablename
		tablename = db_schema + "." + tablename;
		Map<String, SQLColumn> fieldsMapper;
		fieldsMapper = this.loadTableMetadata(tablename);
		if (fieldsMapper != null) {
			TypeMappingPreparedStatementCreator psc = new TypeMappingPreparedStatementCreator(tablename, headers, data,
					fieldsMapper);
			if (!this.parsing_locale.isEmpty()) {
				psc.setLocale(Locale.forLanguageTag(parsing_locale));
			}
			if (!this.parsing_dateformat.isEmpty()) {
				psc.setDateParsingFormat(parsing_dateformat);
			}
			psc.buildRequest();
			return jdbcTemplate.update(psc);
		} else {
			return 0;
		}

	}

	private Map<String, SQLColumn> loadTableMetadata(String tablename) {
		// we don't need to get real data, we just need a query, to get the
		// metadata
		String simplequery = "SELECT * FROM " + tablename + " LIMIT 1;";
		return jdbcTemplate.query(simplequery, new ResultsetMetadataExtractor(ignorefieldscase));
	}
}
