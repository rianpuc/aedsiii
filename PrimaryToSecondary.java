package com.aedsiii.puc.app;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;

import com.aedsiii.puc.model.Job;

public class PrimaryToSecondary {
    public static void toSecondary(ArrayList<Job> jobs, String path){ // criando arquivo db com os dados, caso ainda não exista
        try {
            FileOutputStream arq;
            DataOutputStream dos;
            arq = new FileOutputStream(path);
            dos = new DataOutputStream(arq);
            int highest_id = -1;
            dos.writeInt(highest_id);
            for(Job job : jobs) {
                job.toBytes(dos, 1, false, 0);
                if (job.getJob_id() > highest_id) {
                    highest_id = job.getJob_id();
                }
            }
            dos.close();
            arq.close();

            RandomAccessFile raf = new RandomAccessFile(path, "rw");
            raf.seek(0);
            raf.writeInt(highest_id);
            raf.close();
        } catch (Exception e){
            System.err.println("Erro em PrimaryToSecondary.java: " + e);
        }
    }
}
