package scanner;

import java.io.File;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import scanner.db.DbPublisher;
import scanner.model.impl.CsvContent;

@Component
public class ScannerImpl implements Scanner{
	
	@Value( "${dir.path}" )
	private String dir_path;
	@Value( "${file.pattern}" )
	private String file_pattern;
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
		System.out.println("TODO : scan "+dir_path+"//"+file_pattern);
		//System.out.println("params: sep="+csv_separator.toCharArray()[0]+"/quote="+csv_quotechar.toCharArray()[0]);
		File directory = new File(dir_path);
		String [] extensions = {"csv"};
		Iterator<File> files = FileUtils.iterateFiles(directory, extensions, true);
		while(files.hasNext()) {
	         File file = files.next();
	         CsvContent filecontent = new CsvContent(csv_separator, csv_quotechar, csv_skiplines);
	         filecontent.setIgnoreValues(csv_ignoreFields);
	         filecontent.readFile(file);
	         
	         filecontent.print();
	         dbPublisher.publish(filecontent, "collect.c_meteo_pluiesquot");
	      }
		
	}
	
	
	
}
