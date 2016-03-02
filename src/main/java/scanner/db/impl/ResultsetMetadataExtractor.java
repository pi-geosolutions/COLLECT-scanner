package scanner.db.impl;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

public class ResultsetMetadataExtractor implements ResultSetExtractor<Map<String, SQLColumn>> {
	private boolean lowercasekeys=false;

	public ResultsetMetadataExtractor() {
		super();
	}
	
	public ResultsetMetadataExtractor(boolean lowercasekeys) {
		this.lowercasekeys = lowercasekeys;
	}

	public Map<String, SQLColumn> extractData(ResultSet rs) throws SQLException, DataAccessException {
		
		Map<String, SQLColumn> fieldsMapper = new HashMap<String, SQLColumn>();

		ResultSetMetaData rsmd = rs.getMetaData();
		int columnCount = rsmd.getColumnCount();
		for (int i = 1; i <= columnCount; i++) {
			SQLColumn column = new SQLColumn();
			column.setName(rsmd.getColumnName(i));
			column.setType(rsmd.getColumnTypeName(i));
			column.setTypeCode(rsmd.getColumnType(i));
			System.out.println(column.toString());
			fieldsMapper.put(formatKey(rsmd.getColumnName(i)), column);
		}
		return fieldsMapper;
	}
	
	private String formatKey(String key) {
		if (this.lowercasekeys) {
			return key.toLowerCase();
		}
		return key;
	}

}
