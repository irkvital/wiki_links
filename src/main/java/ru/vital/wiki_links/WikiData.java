package ru.vital.wiki_links;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import javax.net.ssl.HttpsURLConnection;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


public class WikiData {
    private String directory = "./data/";
    private String fileLinksForProcessing = directory + "linksForProcessing.ser";
    private String fileAllWikiLinks = directory + "allWikiLinks.ser";
    private String fileBadLinks = directory + "badLinks.ser";

    private Queue<String> linksForProcessing;
    private Map<String, Set<String>> allWikiLinks;
    private Set<String> badLinks;

    @SuppressWarnings("unchecked")
    public WikiData() {
        try (FileInputStream fis = new FileInputStream(fileLinksForProcessing)) {
            ObjectInputStream os = new ObjectInputStream(fis);
            linksForProcessing = (Queue<String>) os.readObject();
        } catch (Exception e) {
            linksForProcessing = new LinkedList<>();
        }

        try (FileInputStream fis = new FileInputStream(fileBadLinks)) {
            ObjectInputStream os = new ObjectInputStream(fis);
            badLinks = (Set<String>) os.readObject();
        } catch (Exception e) {
            badLinks = new HashSet<>();
        }

        try (FileInputStream fis = new FileInputStream(fileAllWikiLinks);) {
            ObjectInputStream os = new ObjectInputStream(fis);
            allWikiLinks = (HashMap<String, Set<String>>) os.readObject();
        } catch (Exception e) {
            allWikiLinks = new HashMap<String, Set<String>>();
            linkProcessing("Приветствие");
        }
    }

    public void saveData() {
        if (new File(directory).mkdir()) {
            System.out.println("Создана директория с данными");
        }
        
        try (FileOutputStream fis = new FileOutputStream(fileLinksForProcessing)) {
            ObjectOutputStream os = new ObjectOutputStream(fis);
            os.writeObject(linksForProcessing);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try (FileOutputStream fis = new FileOutputStream(fileBadLinks)) {
            ObjectOutputStream os = new ObjectOutputStream(fis);
            os.writeObject(badLinks);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try (FileOutputStream fis = new FileOutputStream(fileAllWikiLinks)) {
            ObjectOutputStream os = new ObjectOutputStream(fis);
            os.writeObject(allWikiLinks);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void start(int count) {
        String page = linksForProcessing.poll();
        while (count > 0 && page != null) {
            count--;
            System.out.println(count + "  Открыли " + page);
            if (linkProcessing(page)) {
                System.out.println("Обработали и удалили " + page);
            } else {
                System.out.println("Не обработали ссылку " + page);
            }
            page = linksForProcessing.poll();
        }
    }

    // false - error, true - OK
    private boolean linkProcessing(String page) {
        try {
            Set<String> result = takeLinksfromPage(page);
            allWikiLinks.put(page, result);
            // Добавляем элемент, если такого нет в очереди
            for (String element : result) {
                if (!linksForProcessing.contains(element)) {
                    linksForProcessing.add(element);
                }
            }

        } catch (Exception e1) {
            // e1.printStackTrace();
            badLinks.add(page);
            return false;
        }
        return true;
    }

    private Set<String> takeLinksfromPage(String page) throws Exception {
        String prefix = "https://ru.wikipedia.org/w/api.php?action=parse&page=";
        String postfix = "&format=json&prop=links";
        Set<String> out = new HashSet<>();

            String hexPage = toHex(page);
            StringBuilder response = new StringBuilder();
            URL url = URI.create(prefix + hexPage + postfix).toURL();
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            int respCode = conn.getResponseCode();
            if (respCode != 200) {
                System.err.println("Code " + respCode);
                return out;
            }
            // Обработка запроса
            InputStream inputStream = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            // Получение ссылок из данных
            ObjectMapper om = new ObjectMapper();
            JsonNode jsonArray = om.readTree(response.toString()).get("parse").get("links");
            for (JsonNode jsonNode : jsonArray) {
                out.add(jsonNode.get("*").asText());
            }

        return out;
    }

    private String toHex(String str) throws UnsupportedEncodingException {
        HexFormat commaFormat = HexFormat.ofDelimiter("").withPrefix("%");
        byte[] bytes = str.getBytes("UTF-8");
        String strHex = commaFormat.formatHex(bytes);

        return strHex.replaceAll("%20", "_").toUpperCase();
    }

}

// https://ru.wikipedia.org/w/api.php?action=parse&page=Приветствие&format=json&prop=links
