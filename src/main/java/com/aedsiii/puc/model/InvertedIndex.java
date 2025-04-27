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
import java.util.Set;

public class InvertedIndex {
    private Map<String, List<Integer>> index;

    // Nos campos escolhidos, praticamente não há nenhum termo que precise ser filtrado, mas vou deixar aq por precaução
    // Filtro de palavras para não adicionar termos inúteis na lista invertida
    private static final Set<String> WordFilter = Set.of(
    "the", "and", "a", "an", "of", "in", "on", "at", "to", "for", "with", "is", "it", "this", "that"
    );

    public InvertedIndex() {
        this.index = new HashMap<>();
    }

    /**
     * Função para adicionar um termo na lista invertida.
     * @param string string a ser avaliada
     * @param recordId id a ser adicionado na lista
     */
    public void add(String string, int recordId) {
        if (recordId == -1) {
            //System.err.println("ID inválido: -1. Não adicionado.");
            return;
        }

        // Separa a string por espaços
        String[] words = string.toLowerCase().split("\\s+"); // um ou mais espaços

        for (String word : words) {
            // Verifica se a palavra é relevante
            if (WordFilter.contains(word)) {
                //System.out.println("Termo irrelevante ignorado: " + word);
                continue;
            }

            // Adiciona o termo e atribui ao termo uma lista com os ids dos registros q possuem o termo
            index.computeIfAbsent(word, k -> new ArrayList<>()).add(recordId);
        }
    }

    /**
     * Função para buscar as ocorrências de um termo.
     * @param term termo a ser procurado
     * @return lista de ids que possuem ocorrência do termo
     */
    public List<Integer> search(String term) {
        return index.getOrDefault(term.toLowerCase(), new ArrayList<>());
    }

    /**
     * Função para remover termos de uma lista invertida.
     * @param string string a ser avaliada
     * @param recordId id a ser removido da lista
     */
    public void delete(String string, int recordId) {
        // Separa a string por espaços
        String[] words = string.toLowerCase().split("\\s+"); // um ou mais espaços

        for (String word : words) {
            if (WordFilter.contains(word)) {
                continue;
            }

            List<Integer> ids = index.get(word);
            if (ids != null) {
                int removalIndex = ids.indexOf(recordId);
                if (removalIndex != -1) {
                    ids.remove(removalIndex);
                }
                if (ids.isEmpty()) {
                    index.remove(word);
                }
            }

        }
    }

    /**
     * Função para printar todos os termos atuais na lista e seus ids de ocorrência.
     */
    public void printIndex() {
        for (Map.Entry<String, List<Integer>> entry : index.entrySet()) {
            System.out.println(entry.getKey() + " -> " + entry.getValue());
        }
    }

    /**
     * Salva a lista invertida em um arquivo.
     * @param filePath lugar onde a lista invertida será guardada.
     * @throws IOException
     */
    public void saveToFile(String filePath) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(index);
        }
    }

    /**
     * Carrega a lista invertida de um arquivo.
     * @param filePath lugar de onde a lista invertida será carregada.
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @SuppressWarnings("unchecked")
    public void loadFromFile(String filePath) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath))) {
            this.index = (Map<String, List<Integer>>) ois.readObject();
        }
    }
}
