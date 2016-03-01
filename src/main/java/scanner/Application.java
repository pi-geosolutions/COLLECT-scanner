package scanner;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public class Application {
	
	
    public static void main(String[] args) throws Exception {
        ConfigurableApplicationContext ctx = new SpringApplication("/scanner/context.xml").run(args);
        Scanner scanner = ctx.getBean(Scanner.class);
        scanner.scan();
        System.out.println("Hit Enter to terminate");
        System.in.read();
        ctx.close();
    }

}