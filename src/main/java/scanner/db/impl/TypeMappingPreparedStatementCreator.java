package scanner.db.impl;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.util.StringUtils;

import scanner.model.impl.CsvContent;

public class TypeMappingPreparedStatementCreator implements PreparedStatementCreator {

	private static final Logger logger = LoggerFactory.getLogger(TypeMappingPreparedStatementCreator.class);

	private String tablename;
	private List<String> headers;
	private List<List<String>> data;
	private Map<String, SQLColumn> fieldsMapper;

	public TypeMappingPreparedStatementCreator(String tablename, List<String> headers, List<List<String>> data,
			Map<String, SQLColumn> fieldsMapper) {
		this.tablename = tablename;
		this.headers = headers;
		this.data = data;
		this.fieldsMapper = fieldsMapper;
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
		String req = "INSERT INTO collect.c_meteo_pluiesquot (\""
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
		switch (valuetype) {
		case 4:
			logger.debug("Parsing "+value+" as integer");
			ps.setInt(index, Integer.parseInt(value));
			break;
		case 91:
			logger.debug("Parsing "+value+" as date");
			ps.setDate(index, this.parseDate(value));
			break;
		default:
			logger.debug("Parsing "+value+" as string");
			ps.setString(index, value);
		}

	}

	private Date parseDate(String value) throws ParseException {
		DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
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

}
