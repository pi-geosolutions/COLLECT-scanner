package scanner;

import java.io.File;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import scanner.db.DbPublisher;
import scanner.model.impl.CsvContent;

@Component
public class ScannerImpl implements Scanner{

	private static final Logger logger = LoggerFactory.getLogger(CsvContent.class);
	
	@Value( "${dir.path}" )
	private String dir_path;
	@Value( "${file.pattern}" )
	private String file_pattern;
	@Value( "${file.collectFolder:collect}" )
	private String file_collectFolder;
	@Value( "${file.collectPrefix:}" )
	private String file_collectPrefix;
	@Value( "${db.collectTablePrefix:c_}" )
	private String db_collectTablePrefix;
	
	
	
	@Value( "${csv.separator:';'}" )
	private char csv_separator;
	@Value( "${csv.quotechar:'\"'}" )
	private char csv_quotechar;
	@Value( "${csv.skiplines:0}" )
	private int csv_skiplines;
	@Value( "${csv.ignoreFields:}" )
	private String csv_ignoreFields;


	@Autowired
    DbPublisher dbPublisher;
	
	public void scan() {
		if (dir_path==null || file_pattern==null) {
			System.out.println("Oops, file is null");
		}
		File directory = new File(dir_path);
		String [] extensions = {"csv"};
		Iterator<File> files = FileUtils.iterateFiles(directory, extensions, true);
		while(files.hasNext()) {
	         File file = files.next();
	         String tablename = this.buildTableName(file);
	         if (tablename==null) {
	        	 break;
	         }
	         CsvContent filecontent = new CsvContent(csv_separator, csv_quotechar, csv_skiplines);
	         filecontent.setIgnoreValues(csv_ignoreFields);
	         filecontent.readFile(file);
	         
	         logger.debug(filecontent.print());
	         dbPublisher.publish("c_meteo_pluiesquot", filecontent.getFieldNames(), filecontent.getData());
	      }
		
	}
	
	private String buildTableName(File file) {
		//check collectFolder matches (defined in properties file)
		if (file_collectFolder.isEmpty() || !file_collectFolder.equalsIgnoreCase(file.getParentFile().getName())) {
			return null;
		}
		//check file_collectPrefix matches (defined in properties file)
		if ( (!file_collectPrefix.isEmpty()) && (!file.getName().startsWith(file_collectPrefix)) ) {
			return null;
		}
		String name=db_collectTablePrefix+file.getName().split("--")[0];
		
		logger.info("File '"+file.getName()+"' -> table name '"+name+"'");
		return name;
	}
	
	
	
}
