package scanner.dao;

import java.text.ParseException;
import java.util.List;

import org.springframework.stereotype.Component;

import scanner.dao.utils.UpsertResult;


@Component
public interface DbPublisher {
	public int publish(String tablename, List<String> headers, List<List<String>> data) ;
	public UpsertResult publish(String tablename, List<String> primarykeys, List<String> headers, List<List<String>> data) throws ParseException ;
}
