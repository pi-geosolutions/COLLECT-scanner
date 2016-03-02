package scanner;

import java.io.File;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import scanner.db.DbPublisher;
import scanner.model.impl.CsvContent;

@Component
public class ScannerImpl implements Scanner{

	private static final Logger logger = LoggerFactory.getLogger(ScannerImpl.class);
	
	@Value( "${dir.path}" )
	private String dir_path;
	@Value( "${file.pattern}" )
	private String file_pattern;
	@Value( "${file.collectFolder:collect}" )
	private String file_collectFolder;
	@Value( "${file.collectPrefix:}" )
	private String file_collectPrefix;
	@Value( "${file.postPublishPolicy:delete}" )
	private String file_postPublishPolicy;
	@Value( "${file.archiveDirectory}" )
	private String file_archiveDirectory;
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
	         int success=0;
	         try {
	        	 success = dbPublisher.publish("c_meteo_pluiesquot", filecontent.getFieldNames(), filecontent.getData());
	        	 logger.debug("publish request returned value "+success);
	        	 if (success>0) {
	        		 this.removeFile(file);
	        	 }
	         } 
	         catch (DataAccessException e)
	         {
	        	 logger.error(e.getLocalizedMessage());
	        	 //throw new RuntimeException(e);
	         }
	      }
		
	}
	
	private void removeFile(File file) {
		if(file_postPublishPolicy.equalsIgnoreCase("delete")) {
			if (file.delete()) {
				logger.info("Deleted file "+file.getName());
			} else {
				logger.error("Could not delete "+file.getName());
			}
		} else 
		if(file_postPublishPolicy.equalsIgnoreCase("rename")) {
			if (file.renameTo(new File(file.getParent(), file.getName()+".bak"))) {
				logger.info("Renamed file "+file.getPath()+"(.bak)");
			} else {
				logger.error("Could not rename "+file.getPath()+" to "+file.getName()+".bak");
			}
		} else 
		if(file_postPublishPolicy.equalsIgnoreCase("archive")) {
			if (file.renameTo(new File(file_archiveDirectory, file.getName()))) {
				logger.info("Archived file "+file.getPath()+" in "+file_archiveDirectory);
			} else {
				logger.error("Could not archive "+file.getPath()+" to "+file_archiveDirectory);
			}
		} else {
			logger.warn("Unconventional value for file_postPublishPolicy property: "+file_postPublishPolicy+"\n \tDoing nothing !");
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
