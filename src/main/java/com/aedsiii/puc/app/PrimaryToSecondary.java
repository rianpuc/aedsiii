package com.aedsiii.puc.app;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;

import com.aedsiii.puc.model.Job;

public class PrimaryToSecondary {
    public static void toSecondary(ArrayList<Job> jobs){
        try {
            FileOutputStream arq;
            DataOutputStream dos;
            arq = new FileOutputStream("binary_db.db");
            dos = new DataOutputStream(arq);
            int size = jobs.size()-1;
            Job last = jobs.get(size);
            int last_index = last.getJob_id();
            dos.writeInt(last_index);
            for(Job job : jobs) {
                job.toBytes(dos);
            }
            arq.close();
            dos.close();
        } catch (Exception e){
            System.err.println("Erro em PrimaryToSecondary.java: " + e);
        }
    }
}
