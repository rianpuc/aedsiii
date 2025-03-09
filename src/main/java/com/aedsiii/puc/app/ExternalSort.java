package com.aedsiii.puc.app;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;

import javax.xml.crypto.Data;

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

            // Excluir arquivos temporários de possíveis ordenações anteriores
            // Ainda sobrará arquivos além de m, porém eles não serão usados ent n tem problema por enquanto
            for (int i = 0; i < m; i++) {
                File tempFile = new File(temp_path + "/temp_fos" + i + ".tmp.db");
                if (tempFile.exists()) {
                    tempFile.delete();
                }
            }

            FileInputStream arqOriginal = new FileInputStream(db_path);
            DataInputStream dis = new DataInputStream(arqOriginal);
            dis.readInt(); // lendo cabeçalho

            ArrayList<DataOutputStream> caminhos_dos = new ArrayList<DataOutputStream>();

            for (int i = 0; i < m; i++) {
            	FileOutputStream fos = new FileOutputStream(temp_path + "/temp_fos" + i + ".tmp.db");
            	DataOutputStream dos = new DataOutputStream(fos);
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
            dis = null;
            
            //INTERCALACAO

            ArrayList<DataOutputStream> temps_dos = new ArrayList<DataOutputStream>();
            ArrayList<DataInputStream> temps_dis = new ArrayList<DataInputStream>();

            for (int i = 0; i < m; i++) {
            	FileOutputStream fos = new FileOutputStream(temp_path + "/temp_i_fos" + i + ".tmp.db");
            	DataOutputStream dos = new DataOutputStream(fos);
            	temps_dos.add(dos);
                FileInputStream fis = new FileInputStream(temp_path + "/temp_fos" + i + ".tmp.db");
                dis = new DataInputStream(fis);
                temps_dis.add(dis);
            }

            boolean endOfFile[] = new boolean[m];
            int filePointer[] = new int[m];

            // usado como limite da primeira intercalação
            // int max_sorted = b*m;
            // usado como limite da segunda intercalação até o final
            // max_sorted = max_sorted * m;

            while(!finalized(endOfFile)){
                ArrayList<Job> jobs = new ArrayList<Job>();
                Job[] registrosAtuais = new Job[m];
                for(int i = 0; i < b*m; i++) {
                    Job menor = menorRegistro(temps_dis, filePointer, registrosAtuais);
                    jobs.add(menor);
                    filePointerPrint(filePointer);
                }
                for (Job job : jobs){
                    System.out.printf("%s\n", job.toString());
                }
            }


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

    public static boolean finalized(boolean[] files){
        boolean res = true;
        for(int i = 0; i < files.length; i++){
            if(files[i] == false){
                res = false;
                break;
            }
        }
        return res;
    }

    public static Job menorRegistro(ArrayList<DataInputStream> dis, int[] indexes, Job[] registrosAtuais) throws IOException {
        int menorIndex = -1;
        Job menorJob = null;
        for(int i = 0; i < dis.size(); i++){
            if (registrosAtuais[i] == null) { // Só lê do arquivo se ainda não temos um job salvo
                DataInputStream stream = dis.get(i);
                byte alive = stream.readByte(); // Lápide
                int recordSize = stream.readInt(); // Tamanho do registro
                short jobId = stream.readShort(); // ID do registro
                byte[] data = new byte[recordSize - 3];
                stream.readFully(data);
                Job job = SecondaryToPrimary.deserializeJob(data);
                job.setJob_id(jobId);
                registrosAtuais[i] = job; // Salva o job lido
            }
    
            // Verifica se este é o menor job até agora
            if (menorJob == null || registrosAtuais[i].getJob_id() < menorJob.getJob_id()) {
                menorJob = registrosAtuais[i];
                menorIndex = i;
            }
        }
        System.out.printf("\nMenor Index: %d\n", menorIndex);
        registrosAtuais[menorIndex] = null;
        indexes[menorIndex] += 1;
        return menorJob;
    }

    public static void filePointerPrint(int[] filePointer){
        for(int i = 0; i < filePointer.length; i++){
            System.out.printf("filePointer[%d] = %d\n", i, filePointer[i]);
        }
    }
}
