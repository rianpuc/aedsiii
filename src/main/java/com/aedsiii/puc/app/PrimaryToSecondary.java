package com.aedsiii.puc.app;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;

import com.aedsiii.puc.model.Job;

public class PrimaryToSecondary {
    /**
     * Criando arquivo .db com os dados, caso ainda não exista
     * @param jobs lista com os jobs obtidos do arquivo .csv
     * @param path caminho onde o arquivo .db resultante ficará
     */
    public static void toSecondary(ArrayList<Job> jobs, String path){
        try {
            FileOutputStream arq;
            DataOutputStream dos;
            arq = new FileOutputStream(path);
            dos = new DataOutputStream(arq);

            int highest_id = -1;
            dos.writeInt(highest_id); // Cabeçalho temporário

            for(Job job : jobs) {
                job.toBytes(dos, 1, false, 0);
                if (job.getJob_id() > highest_id) {
                    highest_id = job.getJob_id();
                }
            }
            dos.close();
            arq.close();

            // Reescrevendo cabeçalho com o ID correto
            RandomAccessFile raf = new RandomAccessFile(path, "rw");
            raf.seek(0);
            raf.writeInt(highest_id);
            raf.close();
        } catch (Exception e){
            System.err.println("Erro em PrimaryToSecondary.java: " + e);
        }
    }
}
