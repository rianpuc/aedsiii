package com.aedsiii.puc.app;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;

import com.aedsiii.puc.model.Job;

public class SecondaryToPrimary {
    public static int addJob(Job job, String path){
        int last_id = -1;
        try {
            // Lendo cabeçalho (ultimo ID adicionado)
            FileInputStream arq = new FileInputStream(path);
            DataInputStream dis = new DataInputStream(arq);
            last_id = dis.readInt();
            arq.close();
            dis.close();

            // Abrindo no modo append pra escrever a partir do final (parametro true)
            FileOutputStream fos = new FileOutputStream(path, true);
            DataOutputStream dos = new DataOutputStream(fos);
            
            job.setJob_id((short) (last_id + 1));
            job.toBytes(dos);
        } catch (IOException e){
            System.err.println("Erro em SecondaryToPrimary.java, addJob: " + e);
        }
        return last_id;
    }

    public static Job getJob(int id, String path){
        Job job = new Job();
        try {
            FileInputStream arq = new FileInputStream(path);
            DataInputStream dis = new DataInputStream(arq);
            dis.readInt(); //pulando o primeiro byte que guarda o ultimo ID cadastrado
            while (dis.available() > 0) { // Enquanto houver bytes para ler
                byte alive = dis.readByte(); //Ve se ta vivo
                int recordSize = dis.readInt(); // Lê o tamanho do registro
                short jobId = dis.readShort(); // Lê o ID do registro
                //System.out.printf("Job ID Atual: %d\nTamanho: %d\n", jobId, recordSize);
                if (jobId == id && alive == 1) {
                    byte[] data = new byte[recordSize - 2]; // Já lemos o ID, então o resto é o conteúdo (-2 bytes do short jobId)
                    dis.readFully(data);
                    job = deserializeJob(data); // deserialize = transformar array de bytes em objeto
                    job.setJob_id(jobId);
                    break; // Para a leitura assim que encontramos o ID
                } else {
                    dis.skipBytes(recordSize - 2); // Pula o restante do registro
                }
            }
            arq.close();
            dis.close();
        } catch (IOException e){
            System.err.println("Erro no SecondaryToPrimary.java, getJob: " + e);
        }
        return job;
    }
    public static boolean removeJob(int id, String path){
        boolean found = false;
        String tempPath = path + ".tmp";
        try {
            FileInputStream fis = new FileInputStream(path);
            DataInputStream dis = new DataInputStream(fis);

            // colocando alterações em um arquivo novo pq n tamo usando RAF ainda, mas no fim só vai mudar o byte de lapide
            FileOutputStream fos = new FileOutputStream(tempPath);
            DataOutputStream dos = new DataOutputStream(fos);

            int lastId = dis.readInt();
            dos.writeInt(lastId);
            while (dis.available() > 0) {
                byte alive = dis.readByte();
                int recordSize = dis.readInt();
                short jobId = dis.readShort();
                byte[] data = new byte[recordSize - 2]; // -2 bytes do short jobId
                dis.readFully(data);
                if (jobId == id && alive == 1) {
                    dos.writeByte(0); // 0 = morto
                    found = true;
                } else {
                    dos.writeByte(alive);
                }
                dos.writeInt(recordSize);
                dos.writeShort(jobId);
                dos.write(data);
            }
            fis.close();
            dis.close();
            fos.close();
            dos.close();
            if (found) {
                File oldFile = new File(path);
                File newFile = new File(tempPath);
                if (oldFile.delete()) {
                    newFile.renameTo(oldFile);
                }
            } else { // não encontrado = nenhuma alteração feita, arquivo temp pode ir embora
                new File(tempPath).delete();
            }
        } catch (IOException e){
            System.err.println("Erro em SecondaryToPrimary.java, removeJob: " + e);
        }
        return found;
    }
    public static ArrayList<Job> toPrimary(String path){ // pegar jobs do db e transformar tudo em uma listona
        ArrayList<Job> jobs = new ArrayList<Job>();
        try {
            FileInputStream arq = new FileInputStream(path);
            DataInputStream dis = new DataInputStream(arq);
            dis.readInt(); //pulando o primeiro byte que guarda o ultimo ID cadastrado
            while(dis.available() > 0) {
                Job job = new Job();
                //System.out.printf("Tamanho do registro: %d bytes\nAtivo: %d\n", dis.readInt(), dis.readByte());
                dis.readByte(); // lapide
                dis.readInt(); // tamanho do registro
                job.setJob_id(dis.readShort());
                job.setExperience(dis.readUTF());
                job.setQualification(dis.readUTF());
                job.setSalary_range(dis.readUTF());
                job.setLocation(dis.readUTF());
                job.setCountry(dis.readUTF());
                job.setLatitude(dis.readFloat());
                job.setLongitude(dis.readFloat());
                job.setWork_type(dis.readUTF());
                job.setCompany_size(dis.readInt());
                job.setJob_posting_date(Instant.ofEpochSecond(dis.readLong()));
                int preferenciaLen = dis.readByte();
                byte[] preferenciaBytes = new byte[preferenciaLen];
                dis.readFully(preferenciaBytes);
                job.setPreference(new String(preferenciaBytes).trim());
                job.setContact_person(dis.readUTF());
                job.setContact(dis.readUTF());
                job.setJob_title(dis.readUTF());
                job.setRole(dis.readUTF());
                job.setJobPortal(dis.readUTF());
                job.setJob_description(dis.readUTF());
                job.setBenefits(readListBinary(dis));
                job.setSkills(readListBinary(dis));
                job.setResponsibilities(readListBinary(dis));
                job.setCompany(dis.readUTF());
                job.setCompany_profile(dis.readUTF());
                jobs.add(job);
            }
            arq.close();
            dis.close();
        } catch (Exception e){
            System.err.println("Erro em SecondaryToPrimary.java: " + e);
        }
        return jobs;
    }
    public static void addToList(Scanner sc, String message, List<String> list) {
        while (true) {
            System.out.println(message);
            String input = sc.nextLine();
            if (input.equalsIgnoreCase("PARAR")) {
                break;
            }
            list.add(input);
        }
    }
    private static List<String> readListBinary(DataInputStream dis) {
        List<String> list = new ArrayList<>();
        try {
            int size = dis.readInt();
            for (int i = 0; i < size; i++) {
                list.add(dis.readUTF());
            }
        } catch (Exception e) {
            System.err.println("Erro na funcao readListBinary em SecondaryToPrimary.java: " + e);
        }
        return list;
    }
    private static Job deserializeJob(byte[] data){
        Job job = new Job();
        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data))) {
            job.setExperience(dis.readUTF());
            job.setQualification(dis.readUTF());
            job.setSalary_range(dis.readUTF());
            job.setLocation(dis.readUTF());
            job.setCountry(dis.readUTF());
            job.setLatitude(dis.readFloat());
            job.setLongitude(dis.readFloat());
            job.setWork_type(dis.readUTF());
            job.setCompany_size(dis.readInt());
            job.setJob_posting_date(Instant.ofEpochSecond(dis.readLong()));
            int preferenciaLen = dis.readByte();
            byte[] preferenciaBytes = new byte[preferenciaLen];
            dis.readFully(preferenciaBytes);
            job.setPreference(new String(preferenciaBytes).trim());
            job.setContact_person(dis.readUTF());
            job.setContact(dis.readUTF());
            job.setJob_title(dis.readUTF());
            job.setRole(dis.readUTF());
            job.setJobPortal(dis.readUTF());
            job.setJob_description(dis.readUTF());
            job.setBenefits(readListBinary(dis));
            job.setSkills(readListBinary(dis));
            job.setResponsibilities(readListBinary(dis));
            job.setCompany(dis.readUTF());
            job.setCompany_profile(dis.readUTF());
        } catch (Exception e){
            System.err.println("Erro no SecondaryToPrimary.java, DeserializeJob: " + e);
        }
    return job;
    }
}
