package com.aedsiii.puc.app;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import com.aedsiii.puc.model.Job;

public class ExternalSort {
    /*
     * int m = caminhos
     * int b = tamanho dos blocos
     */
    public static void sort(int b, int m, String db_path, String external_sort_path) {
        try {
            
            String temp_path = external_sort_path; // só pelo nome msm
            
            // Criar o diretório se não existir
            File dir = new File(temp_path);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            FileInputStream arqOriginal = new FileInputStream(db_path);
            DataInputStream dis = new DataInputStream(arqOriginal);
            dis.readInt(); // lendo cabeçalho

            ArrayList<FileOutputStream> caminhos_fos = new ArrayList<FileOutputStream>();
            ArrayList<DataOutputStream> caminhos_dos = new ArrayList<DataOutputStream>();

            for (int i = 0; i < m; i++) {
            	FileOutputStream fos = new FileOutputStream(temp_path + "/temp_fos" + i + ".tmp.db");
            	DataOutputStream dos = new DataOutputStream(fos);
            	caminhos_fos.add(fos);
            	caminhos_dos.add(dos);
            }

            // variável pra usar no loop circular de m caminhos
            int fileIndex = 0;
            while (dis.available() > 0) {
            	// Lista para guardar B registros
            	ArrayList<Job> jobs_block = new ArrayList<Job>();
            	// Coletar B registros
            	for (int i = 0; i < b && dis.available() > 0; i++) {
            		Job job = new Job();
                    byte alive = dis.readByte();
                    int recordSize = dis.readInt();
                    short jobId = dis.readShort();
                    byte[] data = new byte[recordSize - 3]; // Já lemos o ID, então o resto é o conteúdo
                    dis.readFully(data);
                    if (alive == 1) {
                        job = SecondaryToPrimary.deserializeJob(data);
                        job.setJob_id(jobId);
                        jobs_block.add(job);
                    }
            	}
            
            	// Ordenando o bloco obtido
            	Collections.sort(jobs_block, (job1, job2) -> {
            		return job1.getJob_id() - job2.getJob_id();
            	});
            
            	DataOutputStream dos = caminhos_dos.get(fileIndex);
            	for (Job job : jobs_block) {
            		job.toBytes(dos, 1, false, 0);
            	}
            
            	fileIndex = (fileIndex + 1) % m; // loop circular. Indo para o proximo arquivo dos m caminhos
            }

            dis.close();
        } catch (IOException e) {
            System.err.println("Erro em ExternalSort.java, sort");
            e.printStackTrace();
        }
    }

    public static void test_read(String external_sort_path, int m_caminhos) {
        try {
            for (int i = 0; i < m_caminhos; i++) {
                ArrayList<Job> jobs = new ArrayList<Job>();
                String temp_files_path = external_sort_path + "/temp_fos" + i + ".tmp.db";
                FileInputStream arq = new FileInputStream(temp_files_path);
                DataInputStream dis = new DataInputStream(arq);
                // dis.readInt(); os arquivos temporários não possuem cabeçalho
                while(dis.available() > 0) {
                    Job job = new Job();
                    byte alive = dis.readByte(); // lapide
                    int recordSize = dis.readInt(); // tamanho do registro
                    short jobId = dis.readShort(); // ID do registro
                    byte[] data = new byte[recordSize - 3]; // Já lemos o ID, então o resto é o conteúdo
                    dis.readFully(data);
                    job = SecondaryToPrimary.deserializeJob(data); // deserialize = transformar array de bytes em objeto
                    job.setJob_id(jobId);
                    if (alive == 1) { // adicionar na lista só se o registro tiver vivo
                    jobs.add(job);
                    }
                }
                arq.close();
                dis.close();
                
                System.out.println("\tARQUIVO TEMPORARIO (" + i + "):\n");
                for (Job job : jobs) {
                    System.out.println(job);
                }
            }
        } catch (Exception e){
            System.err.println("Erro em SecondaryToPrimary.java: " + e);
        }
    }
}
