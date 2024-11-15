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
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AllLinks implements Serializable {
    private static String directory = "./data/";
    private static String fileAllLinks = "allLinks.ser";
    private static final File path = new File(directory + fileAllLinks);
    private int autosave = 1000;

    private String nextLink;
    private Map<Integer, String> links;
    private Map<String, Integer> linksInvert = new HashMap<>();

    @SuppressWarnings("unchecked")
    public AllLinks() {
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(path));) {
            ObjectInputStream os = new ObjectInputStream(bis);
            nextLink = (String) os.readObject();
            links = (HashMap<Integer, String>) os.readObject();
            System.out.println("Файл открыт " + fileAllLinks);
        } catch (Exception e) {
            System.out.println("Файл будет создан " + fileAllLinks);
            links = new HashMap<Integer, String>();
        }
        linksInvert = new HashMap<>();
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
                links.putAll(takeLinks());
                if (i % autosave == 0 && i > 0) {
                    saveData();
                }
            }
        } catch (Exception e) {
            // e.printStackTrace();
            System.out.println("Остановка формирования данных о всех статьях Wiki");
        }
        saveData();
    }

    public HashMap<Integer, String> getData() {
        return (HashMap<Integer, String>) links;
    }

    public Integer getInteger(String value) {
        synchronized(linksInvert) {
            if (linksInvert.size() != links.size()) {
                System.out.println("Создана инвертированная карта");
                linksInvert.clear();
                for (Map.Entry<Integer,String> entry : links.entrySet()) {
                    linksInvert.put(entry.getValue(), entry.getKey());
                }
            }
        }
        return linksInvert.get(value);
    }

    public String getString(Integer key) {
        return links.get(key);
    }

    private Map<Integer, String> takeLinks() throws IOException {
        Map<Integer, String> out = new HashMap<Integer, String>();
        URL url = urlCreate();

        ObjectMapper om = new ObjectMapper();
        JsonNode jn = om.readTree(url);
        this.nextLink = jn.get("continue")
                                                .get("apcontinue").asText();

        jn = jn.get("query").get("allpages");
        for (JsonNode jsonNode : jn) {
            out.put(jsonNode.get("pageid").asInt(), jsonNode.get("title").asText());
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
