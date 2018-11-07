package scanner.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
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

	private static String softUpdateFilenameId = "softUpdate.csv";
	private static String messageFileName = "scanner-msg.log";

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
	@Value("${db.updatable:true}")
	private boolean db_updatable;

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
			String dir = file.getParent();
			this.initMessageFile(dir);
			if (!file.isFile()) {
				String msg = "Error building file names" + "\n -------------------------------------------------------------"
						+ "----------------------------------------------------------------" + "\n "
						+ fileNotPublishedErrorMessage(file.getName())
						+ "\n -------------------------------------------------------------"
						+ "----------------------------------------------------------------";
				logmsg(msg, dir);
				logger.error(msg);
				break;
			}
			boolean successfulRead = this.readFile(file);
			if (successfulRead) {
				this.removeFile(file);
				String msg = file.getName() + "successfully processed and archived";
				logmsg(msg, dir);
			} else {
				
				String msg = "Error publishing file " + file.getPath()
						+ "\n -------------------------------------------------------------"
						+ "----------------------------------------------------------------"
						+ "\n   Transaction aborted. " + "\n " + fileNotPublishedErrorMessage(file.getName())
						+ "\n -------------------------------------------------------------"
						+ "----------------------------------------------------------------";
				logmsg(msg, dir);
				logger.error(msg);				
			}
		}
	}

	/*
	 * Creates a new file, or replaces it, with the current date. The file is
	 * placed in the current dir and serves as a "light log" for end-users of
	 * the COLLECTscanner so they can know what happens
	 */
	private void initMessageFile(String dir) {
			String timeLog = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
			logmsg(timeLog +"\n ===================", dir, true);
	}

	/*
	 * Add msg to the "light log" file of current directory
	 */
	private void logmsg(String message, String dir) {
		logmsg("\n\n"+message, dir, false);
	}

	/*
	 * Writes message into a file called messageFileName, created in dir
	 * serves as a "light log" for end-users of
	 * the COLLECTscanner so they can know what happens
	 * 
	 */
	private void logmsg(String message, String dir, boolean overwrite) {
		File msgfile = new File(dir, messageFileName);
		BufferedWriter writer = null;
		try {
			// create a temporary file
			String timeLog = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
			writer = new BufferedWriter(new FileWriter(msgfile, !overwrite));
			writer.write(message);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				writer.close();
			} catch (Exception e) {
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
			// success = csvManager.publish(csv, names.get(0));
			String tablename = names.get(0);
			names.remove(0);
			logger.debug(names.toString());
			if (this.db_updatable && (!names.isEmpty())) {
				if (file.getName().endsWith(softUpdateFilenameId)) {
					// soft update is not advised, because it will shadow every
					// conflict : in case of conflict, you have no way to know
					// if the right value is the old or the new one. But
					// sometimes, soft updates is handy...
					logger.info("Performing a SOFT UPDATE ( in case an entry is already present, we don't update it) "
							+ tablename + " from file " + file.getPath());
					success = csvManager.publish(csv, tablename, names, false);
				} else {
					logger.info("Performing an UPSERT in " + tablename + " from file " + file.getPath());
					success = csvManager.publish(csv, tablename, names, true);
				}
			} else {
				logger.info("Performing a simple INSERT (no update) in " + tablename + " from file " + file.getPath());
				logmsg("Performing a simple INSERT (no update) in " + tablename + " from file " + file.getPath(), file.getParent());
				success = csvManager.publish(csv, tablename);
			}
		} catch (DataAccessException e) {
			logger.error(e.getLocalizedMessage());
			logmsg(e.getLocalizedMessage(), file.getParent());
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
				logger.info("Renamed file " + file.getPath() + "(" + file_renameExtension + ")");
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
		// cuts the filename into parts (using the '--' separator by default)
		List<String> names = new ArrayList(Arrays.asList(file.getName().split(file_partsSeparator)));
		// drops the last part, insignificant, unless it was tablename only
		// (size=1)!
		logger.debug(names.toString());
		if (names.size() > 1) {
			names.remove(names.size() - 1);
		}
		// Of the remaining entries first entry is tablename,
		// next ones are components of the primary key, used for UPDATEs

		// we add the table_prefix to the tablename (first entry
		names.set(0, db_collectTablePrefix + names.get(0));
		logger.debug(names.toString());

		logger.info("File '" + file.getName() + "' -> table name '" + names.get(0) + "'");
		return names;
	}

	private String fileNotPublishedErrorMessage(String filename) {
		return "File " + filename + " not published"
				+ " see documentation https://github.com/pi-geosolutions/COLLECT-scanner to help you fix this situation"
				+ " or contact your system administrator"
				+ "\n The file will be kept as is until it can be published or you remove it";
	}

}
