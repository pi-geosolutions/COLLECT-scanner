package scanner.db.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import scanner.db.DbPublisher;
import scanner.model.impl.CsvContent;

@Component
public class PostgresqlPublisher implements DbPublisher {

	@Autowired
    JdbcTemplate jdbcTemplate;

	public void publish(CsvContent content, String tablename) {
		Map<String,SQLColumn> fieldsMapper;
		fieldsMapper = this.loadTableMetadata(tablename);
		if (fieldsMapper!=null) {
			String req = buildRequest(tablename, fieldsMapper, content);
			
			ArrayList<Integer> fieldTypes = new ArrayList<Integer>();
			Iterator<String> it = content.getFieldNames().iterator();
			while (it.hasNext()) {
				String name=it.next();
				fieldTypes.add(fieldsMapper.get(name).getTypeCode());
			}

			String [] row = content.getNextRow().toArray(new String[0]);
			for (int i = 0 ; i< row.length ; i++) {
				System.out.println(content.getFieldNames().get(i)+": "+row[i]+"("+fieldTypes.get(i)+")");
			}
			jdbcTemplate.update(req, row, fieldTypes);
		}
		this.clean();
	}
	
	private Map<String,SQLColumn> loadTableMetadata(String tablename) {
		//we don't need to get real data, we just need a query, to get the metadata
		String simplequery = "SELECT * FROM "+tablename+" LIMIT 1;";
        System.out.println(simplequery);
        return jdbcTemplate.query(simplequery,new ResultsetMetadataExtractor());
	}
	
	private String buildRequest(String tablename, Map<String,SQLColumn> fieldsMapper, CsvContent content) {
		String req = "INSERT INTO collect.c_meteo_pluiesquot (\""+StringUtils.collectionToDelimitedString(content.getFieldNames(), "\",\"")+"\") ";
		req += "values ("+org.apache.commons.lang3.StringUtils.repeat("?,", content.getFieldNames().size()-1)+"?);";

		System.out.println(req);
		return req;
	}

	private void clean() {
		//TODO : clean the fieldsMapper HashMap, remove the file
	}


}
