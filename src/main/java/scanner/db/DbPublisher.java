package scanner.db;

import java.util.List;

import org.springframework.stereotype.Component;


@Component
public interface DbPublisher {
	public void publish(String tablename, List<String> headers, List<List<String>> data) ;
}
