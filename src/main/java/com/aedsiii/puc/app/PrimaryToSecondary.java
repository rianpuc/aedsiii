package com.aedsiii.puc.app;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;

import com.aedsiii.puc.model.Job;

public class PrimaryToSecondary {
    public static void toSecondary(ArrayList<Job> jobs, String path){ // criando arquivo db com os dados, caso ainda não exista
        try {
            FileOutputStream arq;
            DataOutputStream dos;
            arq = new FileOutputStream(path);
            dos = new DataOutputStream(arq);
            int lastIndex = jobs.size()-1;
            Job lastJob = jobs.get(lastIndex);
            int lastId = lastJob.getJob_id(); // cabeçalho, ultimo id da lista
            dos.writeInt(lastId);
            for(Job job : jobs) {
                job.toBytes(dos, 1, false, 0);
            }
            arq.close();
            dos.close();
        } catch (Exception e){
            System.err.println("Erro em PrimaryToSecondary.java: " + e);
        }
    }
}
