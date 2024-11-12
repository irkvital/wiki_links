package ru.vital.wiki_links;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class WikiData {
    private DataMap datamap;
    private AllLinks allLinks;

    public WikiData() {
        this.allLinks = new AllLinks();
        this.datamap = new DataMap(allLinks);
    }


    public void info() {
        System.out.println("Размер карты ссылок: " + datamap.size());
        System.out.println("Размер списка всех ссылок: " + allLinks.size());
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
        Set<Integer> tmp = datamap.get(fromId);
        for (Integer integer : tmp) {
            map[0].put(integer, fromId);
            if (integer.equals(toId)) {
                resultStage = 0;
                break;
            }
        }
        // заполнение последующих итераций
        for (int i = 1; i < num && resultStage == -1; i++) {
            map[i] = new HashMap<>();
            // для каждой ссылки предыдущей ступени
            for (Map.Entry<Integer, Integer> entry : map[i - 1].entrySet()) {
                int fromStage = entry.getKey();
                // формируем текущую ступень
                tmp = datamap.get(fromStage);
                for (Integer integer : tmp) {
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

}