package ru.vital.wiki_links;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WikiLinksApplication {

	public static void main(String[] args) {
		SpringApplication.run(WikiLinksApplication.class, args);
		WikiData wiki = new WikiData();
		// wiki.start(1000);
		wiki.startThreads(10000, 10);
		wiki.saveData();
		wiki.info();
		System.out.println("End");
	}
}
