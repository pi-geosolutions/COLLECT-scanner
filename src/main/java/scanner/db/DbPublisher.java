package scanner.db;

import java.util.List;

import org.springframework.stereotype.Component;


@Component
public interface DbPublisher {
	public int publish(String tablename, List<String> headers, List<List<String>> data) ;
}
