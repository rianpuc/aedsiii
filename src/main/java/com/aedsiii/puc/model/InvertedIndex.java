package com.aedsiii.puc.model;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InvertedIndex {
    private Map<String, List<Short>> index;

    public InvertedIndex() {
        this.index = new HashMap<>();
    }

    public void add(String term, short recordId) {
        if (recordId == -1) {
            System.err.println("Invalid recordId: -1. Skipping addition.");
            return;
        }
        index.computeIfAbsent(term.toLowerCase(), k -> new ArrayList<>()).add(recordId);
    }

    public List<Short> search(String term) {
        return index.getOrDefault(term.toLowerCase(), new ArrayList<>());
    }

    public void printIndex() {
        for (Map.Entry<String, List<Short>> entry : index.entrySet()) {
            System.out.println(entry.getKey() + " -> " + entry.getValue());
        }
    }

    public void saveToFile(String filePath) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(index);
        }
    }

    public void loadFromFile(String filePath) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath))) {
            this.index = (Map<String, List<Short>>) ois.readObject();
        }
    }
}
