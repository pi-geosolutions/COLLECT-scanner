package scanner.model.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.opencsv.CSVReader;

public class CsvContent {
	private List<String> headers;
	private List<List<String>> data = new ArrayList<List<String>>();

	private char csv_separator;
	private char csv_quotechar;
	private int csv_skiplines;
	private List<String> ignoreValues;

	public CsvContent(char csv_separator, char csv_quotechar, int csv_skiplines) {
		this.csv_separator = csv_separator;
		this.csv_quotechar = csv_quotechar;
		this.csv_skiplines = csv_skiplines;
	}

	public void readFile(File file) {
		try {
			CSVReader reader = new CSVReader(new FileReader(file), csv_separator, csv_quotechar, csv_skiplines);
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

	/*
	 * For debugging purpose
	 */
	public void print() {
		if (headers == null) {
			System.out.print("headers are null");
		} else {
			Iterator<String> it = headers.iterator();
			while (it.hasNext()) {
				System.out.print(it.next() + ":");
			}
			System.out.println("");
		}
		if (data == null) {
			System.out.print("content is null");
		} else {
			Iterator<List<String>> it = data.iterator();
			while (it.hasNext()) {
				String[] line = it.next().toArray(new String[0]);
				for (int i = 0; i < line.length; i++) {
					System.out.print(line[i] + ":");
				}
				System.out.println("");

			}
		}

	}

	/*
	 * Must be set AFTER executing readFile to be efficient
	 */
	public void setIgnoreValues(String csv_ignoreValues) {
		this.ignoreValues = Arrays.asList(csv_ignoreValues.split(","));
	}

	private void doIgnoreValues() {
		Iterator<String> values = this.ignoreValues.iterator();
		while (values.hasNext()) {
			String name = values.next();
			this.purgeValue(name);
		}
	}

	private void purgeValue(String name) {
		System.out.println("purging value "+name);
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
