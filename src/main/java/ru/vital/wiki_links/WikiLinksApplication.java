package ru.vital.wiki_links;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WikiLinksApplication {

	public static void main(String[] args) {
		SpringApplication.run(WikiLinksApplication.class, args);
		WikiData wiki = new WikiData();
		// wiki.startThreads(10);
		wiki.info();
		String from = "Пиво";
		String to = "Гитлер";
		wiki.search(from, to);
		System.out.println("End");
	}
}
