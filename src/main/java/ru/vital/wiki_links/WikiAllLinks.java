package ru.vital.wiki_links;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class WikiAllLinks implements Serializable {
    private static String directory = "./data/";
    private static String fileAllLinks = "allLinks.ser";
    private static final File path = new File(directory + fileAllLinks);
    private int autosave = 1000;

    private String nextLink;
    private List<String> links;

    @SuppressWarnings("unchecked")
    public WikiAllLinks() {
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(path));) {
            ObjectInputStream os = new ObjectInputStream(bis);
            nextLink = (String) os.readObject();
            links = (ArrayList<String>) os.readObject();
            System.out.println("Файл открыт " + fileAllLinks);
        } catch (Exception e) {
            System.out.println("Файл будет создан " + fileAllLinks);
            links = new ArrayList<String>();
        }
    }

    private void saveData() {
        if (new File(directory).mkdir()) {
            System.out.println("Создана директория с данными");
        }
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(path))) {
            ObjectOutputStream os = new ObjectOutputStream(bos);
            os.writeObject(this.nextLink);
            os.writeObject(this.links);
            System.out.println("Данные сохранены " + fileAllLinks);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Данные не сохранены " + fileAllLinks);
        }
    }

    public void start() {
        start(Integer.MAX_VALUE);
    }

    public void start(int count) {
        System.out.println("Начало формирования данных о всех статьях Wiki");
        try {
            // с условием выхода по достижению счетчика или по обходу всех ссылок
            for (int i = 0; i < count && !(nextLink == null && links.size() != 0); i++) {
                System.out.println("Count: " + i + "  Take from link: " + nextLink);
                links.addAll(takeLinks());
                if (i % autosave == 0 && i > 0) {
                    saveData();
                }
            }
        } catch (Exception e) {
            // e.printStackTrace();
            System.out.println("Остановка формирования данных о всех статьях Wiki");
        }
        System.out.println("Numbers of links: " + links.size());
        saveData();
    }

    public List<String> getData() {
        return new ArrayList<>(links);
    }

    private List<String> takeLinks() throws IOException {
        List<String> out = new ArrayList<>();
        URL url = urlCreate();

        ObjectMapper om = new ObjectMapper();
        JsonNode jn = om.readTree(url);
        this.nextLink = jn.get("continue")
                                                .get("apcontinue").asText();

        jn = jn.get("query").get("allpages");
        for (JsonNode jsonNode : jn) {
            out.add(jsonNode.get("title").asText());
        }
        return out;
    }

    private URL urlCreate() {
        String prefix = "https://ru.wikipedia.org/w/api.php?action=query&format=json&list=allpages&formatversion=2&aplimit=max";
        String postfix = "&apcontinue=";
        URL url = null;

        try {
            if (this.nextLink == null) {
                url = new URL(prefix);
            } else {
                String hexPage = Util.toHex(this.nextLink);
                url = new URL(prefix + postfix + hexPage);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return url;
    }

    public int size() {
        return links.size();
    }

}
