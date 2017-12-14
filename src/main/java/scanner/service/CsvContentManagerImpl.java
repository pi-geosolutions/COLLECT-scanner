package scanner.service;

import java.text.ParseException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import scanner.dao.DbPublisher;
import scanner.dao.utils.UpsertResult;
import scanner.model.CsvContent;

@Component
public class CsvContentManagerImpl implements CsvContentManager {
	private static final Logger logger = LoggerFactory.getLogger(CsvContentManagerImpl.class);

	@Autowired
	private DbPublisher publisher;

	public CsvContentManagerImpl() {
	}

	@Override
	public boolean publish(CsvContent csv, String tablename) {
		int res;
		try {
			res = publisher.publish(tablename, csv.getFieldNames(), csv.getData());
			logger.info("Inserted "+res+" new rows");
			return true;
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();logger.error(e.getLocalizedMessage());
			return false;
		}
	}

	@Override
	@Transactional
	public boolean publish(CsvContent csv, String tablename, List<String> keys, boolean forceUpdate) {
		try {
			UpsertResult res = publisher.publish(tablename, keys, csv.getFieldNames(), csv.getData(),forceUpdate);
			logger.info(res.toString(forceUpdate));
			return true;
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			logger.error(e.getLocalizedMessage());
			return false;
		}
	}
	
	

/*	@Override
	public boolean create(File file) {
		try {
			CSVReader reader = new CSVReader(new FileReader(file), csv_separator, csv_quotechar, csv_skiplines);
			List<String[]> content = reader.readAll();

			List<List<String>> data = new ArrayList<List<String>>();
			// format from List<String[]> to List<List<String>>
			Iterator<String[]> it = content.iterator();
			while (it.hasNext()) {
				String[] line = it.next();
				data.add(new ArrayList<String>(Arrays.asList(line)));
			}

			// read headers (and remove them)
			setFieldNamesList(popRow(0));
			
			

			if (this.ignoreValues != null) {
				this.doIgnoreValues();
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
*/
}
