package scanner.service;

import java.util.List;

import scanner.model.CsvContent;

public interface CsvContentManager {
	public boolean publish(CsvContent csv, String tablename);
	public boolean publish(CsvContent csv, String tablename, List<String> keys);
}
