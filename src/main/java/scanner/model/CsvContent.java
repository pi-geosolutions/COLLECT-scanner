package scanner.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.CSVReader;

public class CsvContent {

	private static final Logger logger = LoggerFactory.getLogger(CsvContent.class);
	
	private List<String> headers;
	private List<List<String>> data = new ArrayList<List<String>>();

	private char csv_separator;
	private char csv_quotechar;
	private int csv_skiplines;
	private List<String> ignoreValues;
	private File file;
	

	public CsvContent(char csv_separator, char csv_quotechar, int csv_skiplines) {
		this.csv_separator = csv_separator;
		this.csv_quotechar = csv_quotechar;
		this.csv_skiplines = csv_skiplines;
	}

	public void readFile(File file) {
		this.file=file;
		try {
			InputStreamReader isr = new InputStreamReader(new FileInputStream(file), "UTF-8");
			CSVReader reader = new CSVReader(isr, csv_separator, csv_quotechar, csv_skiplines);
			List<String[]> content = reader.readAll();

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
	}

	/*
	 * public void setFieldNamesList(String[] list) { this.headers =
	 * Arrays.asList(list); }
	 */
	public void setFieldNamesList(List<String> list) {
		this.headers = new ArrayList<String>(list);
	}

	public List<String> getFieldNames() {
		return this.headers;
	}

	public List<String> popRow(int i) {
		List<String> row = data.get(i);
		data.remove(i);
		return row;
	}
	
	public List<String> getNextRow() {
		return this.popRow(0);
	}
	
	public List<List<String>> getData() {
		return this.data;
	}
	
	public String filename2tablename() {
		String tablename="";
		return tablename;
	}

	/*
	 * For debugging purpose
	 */
	public String print() {
		String out = "Reading CSV from file "+file.getName()+"\n \t\t";
		if (headers == null) {
			out += "headers are null";
		} else {
			Iterator<String> it = headers.iterator();
			while (it.hasNext()) {
				out += it.next() + " | ";
			}
			out += "\n \t\t";
		}
		if (data == null) {
			out += "content is null";
		} else {
			Iterator<List<String>> it = data.iterator();
			while (it.hasNext()) {
				String[] line = it.next().toArray(new String[0]);
				for (int i = 0; i < line.length; i++) {
					//System.out.print(line[i] + ":");
					out += line[i] + " | ";
				}
				out += "\n \t\t";

			}
		}
		return out;

	}

	public void setIgnoreValues(String csv_ignoreValues) {
		this.ignoreValues = Arrays.asList(csv_ignoreValues.split(","));
		if ((this.headers!=null) && (!this.headers.isEmpty())) {
			this.doIgnoreValues();
		}
	}

	private void doIgnoreValues() {
		Iterator<String> values = this.ignoreValues.iterator();
		while (values.hasNext()) {
			String name = values.next();
			this.purgeValue(name);
		}
	}

	private void purgeValue(String name) {
		logger.debug("purging value "+name);
		int index = headers.indexOf(name);
		if (index>=0) {
			// Remove from headers list
			headers.remove(name);
			
			// remove values too, for each line
			Iterator<List<String>> it = data.iterator();
			while (it.hasNext()) {
				it.next().remove(index);
			}
		}

		
	}
}
