package scanner.db;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import scanner.model.impl.CsvContent;


@Component
public interface DbPublisher {
	//TODO :remove
	public void publish(CsvContent content, String tablename);
	public void publish(String tablename, List<String> headers, List<List<String>> data) ;
}
