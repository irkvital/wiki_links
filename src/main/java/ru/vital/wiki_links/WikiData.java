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
        Node[] nodes = new Node[num];
        int fromId = allLinks.getInteger(from);
        int toId = allLinks.getInteger(to);
        
        int resultStage = -1;
        for (int i = 0; i < num && resultStage == -1; i++) {
            nodes[i] = new Node(i);
            if (i == 0) {
                resultStage = nodes[i].fillNodeFrom(fromId, toId);
            } else {
                resultStage = nodes[i].fiilNode(nodes[i - 1], toId);
            }
        }
        // вывод результата
        if (resultStage == -1) {
            System.out.println("Решений не найдено");
        }
        int prevLink = toId;
        System.out.println("Stage " + (resultStage + 1) + " | " + allLinks.getString(toId));
        for (int i = resultStage; i > 0; i--) {
            prevLink = nodes[i].map.get(prevLink);
            System.out.println("Stage " + (i) + " | " + allLinks.getString(prevLink));
        }
        System.out.println("Start  " + " | " + allLinks.getString(fromId));
    }

    private class Node {
        private int stage;
        private Map<Integer, Integer> map;

        public Node(int stage) {
            this.stage = stage;
            map = new HashMap<>();
        }

        private int fillNodeFrom(int fromId, int toId) {
            Set<Integer> setLinksToOperate = datamap.get(fromId);
            for (Integer link : setLinksToOperate) {
                map.put(link, fromId);
                if (link.equals(toId)) {
                    return this.stage;
                }
            }
            return -1;
        }

        private int fiilNode(Node nodePrev, int toId) {
            int resultStage = -1;
            for (Map.Entry<Integer, Integer> entry : nodePrev.map.entrySet()) {
                int fromStage = entry.getKey();
                resultStage = fillNodeFrom(fromStage, toId);
                if (resultStage != -1) {
                    break;
                }
            }
            return resultStage;
        }
    }
}