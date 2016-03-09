package scanner.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import scanner.model.CsvContent;

public class Application {

	private static final Logger logger = LoggerFactory.getLogger(Application.class);
	
	
    public static void main(String[] args) throws Exception {
        ConfigurableApplicationContext ctx = new SpringApplication("/scanner/context.xml").run(args);
        Scanner scanner = ctx.getBean(Scanner.class);
        scanner.scan();
        ctx.close();
    }

}