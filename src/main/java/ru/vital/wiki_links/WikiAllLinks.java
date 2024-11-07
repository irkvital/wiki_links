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
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HexFormat;
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

    private WikiAllLinks() {
        links = new ArrayList<String>();
    }

    public static WikiAllLinks create() {
        WikiAllLinks out;
        try (BufferedInputStream fis = new BufferedInputStream(new FileInputStream(path));) {
            ObjectInputStream os = new ObjectInputStream(fis);
            out = (WikiAllLinks) os.readObject();
            System.out.println("fileAllLinks opened");
        } catch (Exception e) {
            System.out.println("fileAllLinks did not open");
            out = new WikiAllLinks();
        }
        return out;
    }

    private void saveData() {
        if (new File(directory).mkdir()) {
            System.out.println("Создана директория с данными");
        }
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(path))) {
            ObjectOutputStream os = new ObjectOutputStream(bos);
            os.writeObject(this);
            System.out.println("Данные сохранены");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Данные не сохранены");
        }
    }

    public void start() {
        start(Integer.MAX_VALUE);
    }

    public void start(int count) {
        try {
            // с условием выхода по достижению счетчика или по обходу всех ссылок
            for (int i = 0; i < count && !(nextLink == null && links.size() != 0); i++) {
                System.out.println("Count: " + i + "  Take from link: " + nextLink);
                links.addAll(takeLinks());
                if (i % autosave == 0) {
                    saveData();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Numbers of links: " + links.size());
        saveData();
    }

    public void start(int count, int autosave) {
        this.autosave = autosave;
        start(count);
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
                String hexPage = toHex(this.nextLink);
                url = new URL(prefix + postfix + hexPage);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return url;
    }


    private String toHex(String str) throws UnsupportedEncodingException {
        HexFormat commaFormat = HexFormat.ofDelimiter("").withPrefix("%");
        byte[] bytes = str.getBytes("UTF-8");
        String strHex = commaFormat.formatHex(bytes);

        return strHex.replaceAll("%20", "_").toUpperCase();
    }

}