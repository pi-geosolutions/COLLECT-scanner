package scanner.db.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import scanner.db.DbPublisher;
import scanner.model.impl.CsvContent;

@Component
public class PostgresqlPublisher implements DbPublisher {

	@Autowired
    JdbcTemplate jdbcTemplate;
	
	@Value( "${db.ignorefieldscase:false}" )
	private boolean ignorefieldscase;
/*
 * TODO: Remove
 */
	public void publish(CsvContent content, String tablename) {
		Map<String,SQLColumn> fieldsMapper;
		fieldsMapper = this.loadTableMetadata(tablename);
		/*
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
		}*/
		this.clean();
	}
	
	public void publish(String tablename, List<String> headers, List<List<String>> data) {
		Map<String,SQLColumn> fieldsMapper;
		fieldsMapper = this.loadTableMetadata(tablename);
		if (fieldsMapper!=null) {
			TypeMappingPreparedStatementCreator psc = new TypeMappingPreparedStatementCreator(tablename, headers, data, fieldsMapper);
			psc.buildRequest();
			jdbcTemplate.update(psc);
		}
		this.clean();
	}
	
	/*
	 @Transactional("transactionManager")
	private void publishtoDb(List<MeteoDaylyRains> beanList) {
		Iterator<MeteoDaylyRains> it = beanList.iterator();
		while (it.hasNext()) {
			MeteoDaylyRains bean = it.next();
			System.out.println("About to publish "+bean.toString());
			
			PreparedStatementCreator psc = new PreparedStatementCreator() {
				@Override
				public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
					PreparedStatement mesure = con.prepareStatement(bean.getReq());
					mesure.setInt(1, bean.getIdMesure());
					mesure.setInt(2, bean.getCodeStation());
					mesure.setDate(3, new java.sql.Date(bean.getDateMesure().getTime()));
					mesure.setInt(4, bean.getPluiemm());
					return mesure;
				}
			};
			jdbcTemplate.update(psc);

		}
	 */
	
	private Map<String,SQLColumn> loadTableMetadata(String tablename) {
		//we don't need to get real data, we just need a query, to get the metadata
		String simplequery = "SELECT * FROM "+tablename+" LIMIT 1;";
        System.out.println(simplequery);
        return jdbcTemplate.query(simplequery,new ResultsetMetadataExtractor(ignorefieldscase));
	}

	private void clean() {
		//TODO : clean the fieldsMapper HashMap, remove the file
	}


}
