package scanner.db;

import java.util.Map;

import org.springframework.stereotype.Component;

import scanner.model.impl.CsvContent;


@Component
public interface DbPublisher {
	
	public void publish(CsvContent content, String tablename);
}
