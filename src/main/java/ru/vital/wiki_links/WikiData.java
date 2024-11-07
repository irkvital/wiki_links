package ru.vital.wiki_links;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
        try (BufferedInputStream fis = new BufferedInputStream(new FileInputStream(fileLinksForProcessing))) {
            ObjectInputStream os = new ObjectInputStream(fis);
            linksForProcessing = (Queue<String>) os.readObject();
            System.out.println("linksForProcessing opened");
        } catch (Exception e) {
            linksForProcessing = new LinkedList<>();
            System.out.println("linksForProcessing created");
        }

        try (BufferedInputStream fis = new BufferedInputStream(new FileInputStream(fileBadLinks))) {
            ObjectInputStream os = new ObjectInputStream(fis);
            badLinks = Collections.synchronizedSet((Set<String>) os.readObject());
            System.out.println("fileBadLinks opened");
        } catch (Exception e) {
            badLinks = Collections.synchronizedSet(new HashSet<>());
            System.out.println("fileBadLinks created");
        }

        try (BufferedInputStream fis = new BufferedInputStream(new FileInputStream(fileAllWikiLinks));) {
            ObjectInputStream os = new ObjectInputStream(fis);
            allWikiLinks = (ConcurrentHashMap<String, Set<String>>) os.readObject();
            System.out.println("fileAllWikiLinks opened");
        } catch (Exception e) {
            allWikiLinks = new ConcurrentHashMap<String, Set<String>>();
            linkProcessing("Приветствие");
            System.out.println("fileAllWikiLinks created");
        }
    }

    public void saveData() {
        if (new File(directory).mkdir()) {
            System.out.println("Создана директория с данными");
        }
        
        try (BufferedOutputStream fis = new BufferedOutputStream(new FileOutputStream(fileLinksForProcessing))) {
            ObjectOutputStream os = new ObjectOutputStream(fis);
            os.writeObject(linksForProcessing);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try (BufferedOutputStream fis = new BufferedOutputStream(new FileOutputStream(fileBadLinks))) {
            ObjectOutputStream os = new ObjectOutputStream(fis);
            os.writeObject(badLinks);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try (BufferedOutputStream fis = new BufferedOutputStream(new FileOutputStream(fileAllWikiLinks))) {
            ObjectOutputStream os = new ObjectOutputStream(fis);
            os.writeObject(allWikiLinks);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void start(int count) {
        String page = linksForProcessing.poll();
        for (int i = 0; i < count && page != null; i++) {
            while (allWikiLinks.containsKey(page) && page != null) {
                page = linksForProcessing.poll();
            }
            System.out.println(i + "  Открыли " + page);
            if (linkProcessing(page)) {
                System.out.println("Обработали и удалили " + page);
            } else {
                System.out.println("Не обработали ссылку " + page);
            }
            page = linksForProcessing.poll();
        }
    }

    public void startThreads(int count, int threadsNum) {
        ExecutorService executor = Executors.newFixedThreadPool(threadsNum);

        String page = linksForProcessing.poll();
        @SuppressWarnings("unchecked")
        Future<Boolean>[] future = new Future[count];
        int i = 0;
        int j = 0;

        while (i < count && page != null) {
            for (j = i; j < count && page != null; j++) {
                // Проверка на повторное открытие ссылки
                synchronized(linksForProcessing) {
                    while (allWikiLinks.containsKey(page) && page != null) {
                        page = linksForProcessing.poll();
                    }
                    synchronized(badLinks) {
                        while (badLinks.contains(page) && page != null) {
                            page = linksForProcessing.poll();
                        }
                    }
                }

                future[j] = executor.submit(new Task(page));
                synchronized(linksForProcessing) {
                    page = linksForProcessing.poll();
                }
            }
    
            for (; i < j; i++) {
                try {
                    System.out.println(i + "  " + future[i].get());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            page = linksForProcessing.poll();
            System.out.println("END CIRCLE");
            optimizeQueue();
        }
        executor.shutdown();
    }

    // false - error, true - OK
    private boolean linkProcessing(String page) {
        try {
            Set<String> result = takeLinksfromPage(page);
            allWikiLinks.put(page, result);
            // Добавляем элемент
            synchronized(linksForProcessing) {
                for (String element : result) {
                    if (!allWikiLinks.containsKey(element)) {
                        linksForProcessing.add(element);
                    }
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
        Set<String> out = Collections.synchronizedSet(new HashSet<>());

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

    public void optimizeQueue() {
        System.out.println("Размер до    " + linksForProcessing.size());
        Set<String> tmp = new HashSet<>();
        
        synchronized(linksForProcessing) {
            String page = linksForProcessing.poll();
            while (page != null) {
                tmp.add(page);
                page = linksForProcessing.poll();
            }
            
            for (String string : tmp) {
                if (!allWikiLinks.containsKey(string) && !badLinks.contains(string)) {
                    linksForProcessing.add(string);
                }
            }
        }
        System.out.println("Размер после " + linksForProcessing.size());
    }

    public void info() {
        System.out.println("Обработанных ссылок: " + allWikiLinks.size());
        System.out.println("Bad ссылок: " + badLinks.size());
        System.out.println("В очереди ссылок: " + linksForProcessing.size());
    }

    private class Task implements Callable<Boolean> {
        private String page;

        Task(String page) {
            this.page = page;
        }

        @Override
        public Boolean call() {
            return linkProcessing(page);
        }
    }

}