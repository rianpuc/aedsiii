package com.aedsiii.puc.app;

import java.io.RandomAccessFile;
import java.util.ArrayList;

import com.aedsiii.puc.model.RegistroBTree;

/**
 * Classe para coleta de IDs e Offsets.
 */
public class KeyDataCreator {
    /**
     * Método para ler o dataset e criar registros de Árvore B compostos por ID e offset.
     * 
     * @param path caminho do arquivo dataset.
     */
    public static ArrayList<RegistroBTree> criarPares(String path) {
        ArrayList<RegistroBTree> registrosBT = new ArrayList<RegistroBTree>();
        try {
            RandomAccessFile arq = new RandomAccessFile(path, "rw");

            long currentOffset;

            arq.readInt(); // pulando o primeiro byte que guarda o ultimo ID cadastrado
            while(arq.getFilePointer() < arq.length()) {
                currentOffset = arq.getFilePointer();
                byte alive = arq.readByte(); // lapide
                int recordSize = arq.readInt(); // tamanho do registro
                short jobId = arq.readShort(); // ID do registro Já lemos o ID, então o resto é o conteúdo
                
                // Pula os bytes restantes do registro
                arq.skipBytes(recordSize - 3);

                // Adiciona o registro à lista
                if (alive == 1) { // Verifica se o registro está "vivo"
                    registrosBT.add(new RegistroBTree(jobId, currentOffset));
                }
            }
            arq.close();
        } catch (Exception e){
            System.err.println("Erro em SecondaryToPrimary.java: " + e);
        }
        return registrosBT;
    }
}