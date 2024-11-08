package ru.vital.wiki_links;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


public class WikiData {
    private String directory = "./data/";
    private String fileMapLinks = "mapLinks.ser";
    private File path = new File(directory + fileMapLinks);
    private int autosave = 1000;

    private Map<String, Set<String>> dataMap;
    private WikiAllLinks allLinks;

    @SuppressWarnings("unchecked")
    public WikiData() {
        try (BufferedInputStream ois = new BufferedInputStream(new FileInputStream(path));) {
            ObjectInputStream os = new ObjectInputStream(ois);
            dataMap = (ConcurrentHashMap<String, Set<String>>) os.readObject();
            System.out.println("Файл открыт " + fileMapLinks);
        } catch (Exception e) {
            dataMap = new ConcurrentHashMap<String, Set<String>>();
            System.out.println("Файл будет создан " + fileMapLinks);
        }
        this.allLinks = new WikiAllLinks();
    }

    public void saveData() {
        if (new File(directory).mkdir()) {
            System.out.println("Создана директория с данными");
        }

        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(path))) {
            ObjectOutputStream os = new ObjectOutputStream(bos);
            synchronized(dataMap) {
                os.writeObject(dataMap);
            }
            System.out.println("Данные сохранены " + fileMapLinks + " | размер карты " + dataMap.size());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Данные не сохранены " + fileMapLinks);
        }
    }

    public void startThreads(int threadsNum) {
        // Формирование списка ссылок
        allLinks.start();
        System.out.println("Обработано статей: " + dataMap.size());

        // Обход сформированного списка
        ExecutorService executor = Executors.newFixedThreadPool(threadsNum);
        List<String> links = allLinks.getData();
        List<Future<Boolean>> futureList = new ArrayList<>();
        

        for (String page : links) {
            if (!dataMap.containsKey(page)) {
                Task task = new Task(page);
                futureList.add(executor.submit(task));
            }
        }

        executor.shutdown();

        for (int i = 0; i < futureList.size(); i++) {
            if (i % autosave == 0 && i > 0) {
                saveData();
            }
            try {
                System.out.println("Count " + i + "  " + futureList.get(i).get());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        saveData();
    }

    // false - error, true - OK
    private boolean linkProcessing(String page) {
        Set<String> result = null;
        boolean out;
        try {
            result = takeLinksfromPage(page);
            out = true;
        } catch (Exception e) {
            out = false;
        }
        dataMap.put(page, result);
        return out;
    }

    private Set<String> takeLinksfromPage(String page) throws Exception {
        Set<String> out = new HashSet<>();
        URL url = urlCreate(page);

        ObjectMapper om = new ObjectMapper();
        JsonNode jsonArray = om.readTree(url).get("parse").get("links");
        for (JsonNode jsonNode : jsonArray) {
            out.add(jsonNode.get("*").asText());
        }
        return out;
    }

    private URL urlCreate(String page) {
        String prefix = "https://ru.wikipedia.org/w/api.php?action=parse&page=";
        String postfix = "&format=json&prop=links";
        URL url = null;

        try {
            String hexPage = Util.toHex(page);
            url = new URL(prefix + hexPage + postfix);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return url;
    }


    public void info() {
        System.out.println("Размер карты ссылок: " + dataMap.size());
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