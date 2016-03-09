package scanner.main;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.shared.utils.io.DirectoryScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import scanner.model.CsvContent;
import scanner.service.CsvContentManager;

@Component
public class ScannerImpl implements Scanner {

	private static final Logger logger = LoggerFactory.getLogger(ScannerImpl.class);

	@Value("${dir.path}")
	private String dir_path;
	@Value("${file.pattern}")
	private String file_pattern;
	@Value("${file.partsSeparator}")
	private String file_partsSeparator;

	@Value("${file.postPublishPolicy:delete}")
	private String file_postPublishPolicy;
	@Value("${file.archiveDirectory}")
	private String file_archiveDirectory;
	@Value("${file.renameExtension}")
	private String file_renameExtension;
	@Value("${db.collectTablePrefix:c_}")
	private String db_collectTablePrefix;

	@Value("${csv.separator:';'}")
	private char csv_separator;
	@Value("${csv.quotechar:'\"'}")
	private char csv_quotechar;
	@Value("${csv.skiplines:0}")
	private int csv_skiplines;
	@Value("${csv.ignoreFields:}")
	private String csv_ignoreFields;

	@Autowired
	CsvContentManager csvManager;

	public void scan() {
		if (dir_path == null || file_pattern == null) {
			logger.error("File pattern/path ill-defined");
		}
		logger.debug("File pattern is: " + file_pattern);
		DirectoryScanner ds = new DirectoryScanner();
		String[] includes = { file_pattern };
		ds.setIncludes(includes);
		ds.setBasedir(new File(dir_path));
		ds.setCaseSensitive(true);
		ds.scan();

		String[] files = ds.getIncludedFiles();
		for (int i = 0; i < files.length; i++) {
			File file = new File(dir_path, files[i]);
			if (!file.isFile()) {
				logger.error("Error building file names");
				break;
			}
			boolean successfulRead = this.readFile(file);
			if (successfulRead) {
				this.removeFile(file);
			} else {
				logger.error("Error publishing file "+file.getPath());
			}
		}
	}

	/*
	 * Returns true if file successfully read.
	 */
	private boolean readFile(File file) {
		List<String> names = this.buildTableName(file);
		if (names == null || names.isEmpty()) {
			return false;
		}
		CsvContent csv = new CsvContent(csv_separator, csv_quotechar, csv_skiplines);
		csv.setIgnoreValues(csv_ignoreFields);
		csv.readFile(file);

		logger.debug(csv.print());
		boolean success = false;
		try {
			//success = csvManager.publish(csv, names.get(0));
			String tablename = names.get(0);
			names.remove(0);
			logger.debug(names.toString());
			success = csvManager.publish(csv, tablename, names);
		} catch (DataAccessException e) {
			logger.error(e.getLocalizedMessage());
			// throw new RuntimeException(e);
		}
		return success;
	}

	private void removeFile(File file) {
		if (file_postPublishPolicy.equalsIgnoreCase("delete")) {
			if (file.delete()) {
				logger.info("Deleted file " + file.getName());
			} else {
				logger.error("Could not delete " + file.getName());
			}
		} else if (file_postPublishPolicy.equalsIgnoreCase("rename")) {
			if (file.renameTo(new File(file.getParent(), file.getName() + file_renameExtension))) {
				logger.info("Renamed file " + file.getPath() + "("+file_renameExtension+")");
			} else {
				logger.error("Could not rename " + file.getPath() + " to " + file.getName() + file_renameExtension);
			}
		} else if (file_postPublishPolicy.equalsIgnoreCase("archive")) {
			if (file.renameTo(new File(file_archiveDirectory, file.getName()))) {
				logger.info("Archived file " + file.getPath() + " in " + file_archiveDirectory);
			} else {
				logger.error("Could not archive " + file.getPath() + " to " + file_archiveDirectory);
			}
		} else {
			logger.warn("Unconventional value for file_postPublishPolicy property: " + file_postPublishPolicy
					+ "\n \tDoing nothing !");
		}

	}

	private List<String> buildTableName(File file) {
		//cuts the filename into parts (using the '--' separator by default)
		List<String> names = new ArrayList(Arrays.asList(file.getName().split(file_partsSeparator)));
		//drops the last part, insignificant, unless it was tablename only  (size=1)!
		logger.debug(names.toString());
		if (names.size() > 1) {
			names.remove(names.size()-1);
		}
		//Of the remaining entries first entry is tablename, 
		//next ones are components of the primary key, used for UPDATEs
		
		//we add the table_prefix to the tablename (first entry
		names.set(0, db_collectTablePrefix + names.get(0)) ;
		logger.debug(names.toString());
		
		logger.info("File '" + file.getName() + "' -> table name '" + names.get(0) + "'");
		return names;
	}

}
