package ru.vital.wiki_links;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
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
    private String fileMapLinksOld = "mapLinks_old.ser";
    private Path path = Paths.get(directory, fileMapLinks);
    private Path pathOld = Paths.get(directory, fileMapLinksOld);
    private int autosave = 50000;

    private Map<Integer, Set<Integer>> dataMap;
    private WikiAllLinks allLinks;

    @SuppressWarnings("unchecked")
    public WikiData() {
        this.allLinks = new WikiAllLinks();
        try (BufferedInputStream ois = new BufferedInputStream(new FileInputStream(path.toFile()));) {
            ObjectInputStream os = new ObjectInputStream(ois);
            dataMap = (ConcurrentHashMap<Integer, Set<Integer>>) os.readObject();
            System.out.println("Файл открыт " + fileMapLinks);
        } catch (Exception e) {
            e.printStackTrace();
            dataMap = new ConcurrentHashMap<Integer, Set<Integer>>();
            System.out.println("Файл будет создан " + fileMapLinks);
        }
    }

    public void saveData() {
        if (new File(directory).mkdir()) {
            System.out.println("Создана директория с данными");
        }
        if (path.toFile().exists()) {
            try {
                Files.move(path, pathOld, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(path.toFile()))) {
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
        Map<Integer, String> links = allLinks.getData();
        Queue<Future<Boolean>> futureQueue = new LinkedList<>();
        Iterator<Map.Entry<Integer, String>> iterator = links.entrySet().iterator();

        int i = dataMap.size();
        while (iterator.hasNext()) {
            
            int count = 0;
            while (count < autosave && iterator.hasNext()) {
                Map.Entry<Integer, String> entry = iterator.next();
                if (!dataMap.containsKey(entry.getKey())) {
                    Task task = new Task(entry.getValue());
                    futureQueue.add(executor.submit(task));
                    count++;
                }
            }

            count = 0;
            while (!futureQueue.isEmpty()) {
                try {
                    System.out.println("Count " + (i + count) + "  " + futureQueue.poll().get());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                count++;
            }
            i += count;
            saveData();
        }
        executor.shutdown();
        saveData();

    }

    // false - error, true - OK
    private boolean linkProcessing(String page) {
        Set<String> resultStr = null;
        boolean out;
        try {
            resultStr = takeLinksfromPage(page);
            Set<Integer> resultInt = new HashSet<>();
            for (String str : resultStr) {
                Integer result = allLinks.getInteger(str);
                if (result != null) {
                    resultInt.add(result);
                }
            }
            dataMap.put(allLinks.getInteger(page), resultInt);
            out = true;
        } catch (Exception e) {
            out = false;
        }
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

    public void search (String from, String to) {
        search(5, from, to);
    }

    public void search (int num, String from, String to) {
        @SuppressWarnings("unchecked")
        Map<Integer, Integer> map[] = new Map[num];
        int fromId = allLinks.getInteger(from);
        int toId = allLinks.getInteger(to);
        int resultStage = -1;
        // инициализация первой ссылки
        map[0] = new HashMap<>();
        for (Integer integer : dataMap.get(fromId)) {
            map[0].put(integer, fromId);
            if (integer.equals(toId)) {
                resultStage = 0;
                break;
            }
        }
        // заполнение последующих итераций
        for (int i = 1; i < num && resultStage == -1; i++) {
            if (map[i] == null) {
                map[i] = new HashMap<>();
            }
            // для каждой ссылки предыдущей ступени
            for (Map.Entry<Integer, Integer> entry : map[i - 1].entrySet()) {
                int fromStage = entry.getKey();
                // формируем текущую ступень
                for (Integer integer : dataMap.get(fromStage)) {
                    map[i].put(integer, fromStage);
                    if (integer.equals(toId)) {
                        resultStage = i;
                        break;
                    }
                }
                if (resultStage != -1) {
                    break;
                }
            }
        }
        // вывод результата
        if (resultStage == -1) {
            System.out.println("Решений не найдено");
        }
        int prevLink = toId;
        System.out.println("Stage " + (resultStage + 1) + " | " + allLinks.getString(toId));
        for (int i = resultStage; i > 0; i--) {
            prevLink = map[i].get(prevLink);
            System.out.println("Stage " + (i) + " | " + allLinks.getString(prevLink));
        }
        System.out.println("Start  " + " | " + allLinks.getString(fromId));
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