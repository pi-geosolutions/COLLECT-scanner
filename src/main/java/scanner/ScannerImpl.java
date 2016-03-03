package scanner;

import java.io.File;

import org.apache.maven.shared.utils.io.DirectoryScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import scanner.db.DbPublisher;
import scanner.model.impl.CsvContent;

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
	DbPublisher dbPublisher;

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
			}
		}
	}

	/*
	 * Returns true if file successfully read.
	 */
	private boolean readFile(File file) {
		String tablename = this.buildTableName(file);
		if (tablename == null) {
			return false;
		}
		CsvContent filecontent = new CsvContent(csv_separator, csv_quotechar, csv_skiplines);
		filecontent.setIgnoreValues(csv_ignoreFields);
		filecontent.readFile(file);

		logger.debug(filecontent.print());
		int success = 0;
		try {
			success = dbPublisher.publish(tablename, filecontent.getFieldNames(), filecontent.getData());
			logger.debug("publish request returned value " + success);
			if (success > 0) {
				return true;
			}
		} catch (DataAccessException e) {
			logger.error(e.getLocalizedMessage());
			// throw new RuntimeException(e);
		}
		return false;
	}

	private void removeFile(File file) {
		if (file_postPublishPolicy.equalsIgnoreCase("delete")) {
			if (file.delete()) {
				logger.info("Deleted file " + file.getName());
			} else {
				logger.error("Could not delete " + file.getName());
			}
		} else if (file_postPublishPolicy.equalsIgnoreCase("rename")) {
			if (file.renameTo(new File(file.getParent(), file.getName() + ".bak"))) {
				logger.info("Renamed file " + file.getPath() + "(.bak)");
			} else {
				logger.error("Could not rename " + file.getPath() + " to " + file.getName() + ".bak");
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

	private String buildTableName(File file) {
		String name = db_collectTablePrefix + file.getName().split(file_partsSeparator)[0];
		logger.info("File '" + file.getName() + "' -> table name '" + name + "'");
		return name;
	}

}
