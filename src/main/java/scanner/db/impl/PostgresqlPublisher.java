package scanner.db.impl;

import java.util.List;
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

	@Value("${db.fields.ignorecase:false}")
	private boolean ignorefieldscase;

	@Value("${db.schema:public}")
	private String jdbc_schema;

	public int publish(String tablename, List<String> headers, List<List<String>> data) {
		// build complete tablename
		tablename = jdbc_schema + "." + tablename;
		Map<String, SQLColumn> fieldsMapper;
		fieldsMapper = this.loadTableMetadata(tablename);
		if (fieldsMapper != null) {
			TypeMappingPreparedStatementCreator psc = new TypeMappingPreparedStatementCreator(tablename, headers, data,
					fieldsMapper);
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

	private void clean() {
		// TODO : clean the fieldsMapper HashMap, remove the file
	}

}
