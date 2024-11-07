package ru.vital.wiki_links;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WikiLinksApplication {

	public static void main(String[] args) {
		SpringApplication.run(WikiLinksApplication.class, args);
		// WikiData wiki = new WikiData();
		// wiki.start(1000);
		// wiki.startThreads(100, 10);
		// wiki.saveData();
		// wiki.info();
		// System.out.println("End");


		WikiAllLinks allLinks = WikiAllLinks.create();
		allLinks.start(100000);
		System.out.println("END");

	}
}
