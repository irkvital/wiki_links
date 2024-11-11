package ru.vital.wiki_links;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Nodes {
    private int count;
    private List<Node> listNodes;
    int fromId;
    int toId;


    Nodes(int count, int fromId, int toId) {
        this.count = count;
        listNodes = new ArrayList<>(count);
        this.fromId = fromId;
        this.toId = toId;
    }

    public void search() {
        for (int i = 0; i < count; i++) {
            if (listNodes.get(i) == null) {
                listNodes.add(new Node(i));
            }
            
        }
    }


    private class Node {
        private int stage;
        private Map<Integer, Integer> map;

        public Node(int stage) {
            this.stage = stage;
            map = new HashMap<>();
        }
    }
}
