package ru.vital.wiki_links;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import javax.net.ssl.HttpsURLConnection;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class WikiData {
    private String fileLinksProcessed = "linksProcessed.ser";
    private String fileAllWikiLinks = "allWikiLinks.ser";

    private Set<String> linksProcessed;
    private Map<String, Set<String>> allWikiLinks;

    @SuppressWarnings("unchecked")
    public WikiData() {
        try {
            FileInputStream fis = new FileInputStream(fileLinksProcessed);
            ObjectInputStream os = new ObjectInputStream(fis);
            linksProcessed = (HashSet<String>) os.readObject();
        } catch (Exception e) {
            linksProcessed = new HashSet<>();
        }

        try {
            FileInputStream fis = new FileInputStream(fileAllWikiLinks);
            ObjectInputStream os = new ObjectInputStream(fis);
            allWikiLinks = (HashMap<String, Set<String>>) os.readObject();
        } catch (Exception e) {
            allWikiLinks = new HashMap<String, Set<String>>();
            try {
                allWikiLinks.put("Приветствие", takeLinksfromPage("Приветствие"));
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }

    Set<String> takeLinksfromPage(String page) throws Exception {
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
                out.add(jsonNode.get("*").toString());
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
