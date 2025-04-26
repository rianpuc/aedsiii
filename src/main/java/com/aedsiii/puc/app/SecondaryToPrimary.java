package com.aedsiii.puc.app;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.swing.JOptionPane;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.time.Instant;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;

import com.aedsiii.puc.model.Job;
import com.aedsiii.puc.model.RegistroHashExtensivel;

public class SecondaryToPrimary {
    
    public static int addJob(Job job, String path){
        int last_id = -1;
        try {
            // Lendo cabeçalho (ultimo ID adicionado)
            FileInputStream arq = new FileInputStream(path);
            DataInputStream dis = new DataInputStream(arq);
            last_id = dis.readInt();
            byte[] resto = arq.readAllBytes();
            arq.close();
            dis.close();
            last_id += 1; //vai ser adicionado 1 pq vai ser um id novo
            FileOutputStream fos = new FileOutputStream(path);
            DataOutputStream dos = new DataOutputStream(fos);
            dos.writeInt(last_id);
            dos.write(resto);
            job.setJob_id((short) (last_id));
            job.toBytes(dos, 1, false, 0);
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
                    byte[] data = new byte[recordSize - 3]; // Já lemos o ID, então o resto é o conteúdo
                    dis.readFully(data);
                    job = deserializeJob(data); // deserialize = transformar array de bytes em objeto
                    job.setJob_id(jobId);
                    break; // Para a leitura assim que encontramos o ID
                } else {
                    dis.skipBytes(recordSize - 3); // Pula o restante do registro
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

            FileOutputStream fos = new FileOutputStream(tempPath);
            DataOutputStream dos = new DataOutputStream(fos);

            int lastId = dis.readInt(); // cabeçalho
            dos.writeInt(lastId);
            while (dis.available() > 0) {
                byte alive = dis.readByte(); // lápide
                int recordSize = dis.readInt();
                short jobId = dis.readShort();
                byte[] data = new byte[recordSize - 3]; // resto do registro
                dis.readFully(data);

                // encontrou o registro a ser deletado
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

            // encontrado = arquivo temp se tornará o novo arquivo .db
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

    public static boolean updateJob(int id, String path, Scanner sc) {
        boolean status = false;
        boolean found = false;
        Job job = new Job(); // pras iterações
        Job updatedJob = new Job(); // pra escrita no fim do arquivo caso necessario
        String tempPath = path + ".tmp";
        try {
            FileInputStream fis = new FileInputStream(path);
            DataInputStream dis = new DataInputStream(fis);
            // n tamo usando raf ainda ent coloca em arquivo temporario
            FileOutputStream fos = new FileOutputStream(tempPath);
            DataOutputStream dos = new DataOutputStream(fos);

            boolean biggerRecord = false; // true caso a atualização deixe o registro maior
            int foundId = -1; // o jobId vai mudando durante o loop, msm dps de encontrar o especificado

            int lastId = dis.readInt(); //pulando o primeiro byte que guarda o ultimo ID cadastrado
            dos.writeInt(lastId);

            while (dis.available() > 0) { // Enquanto houver bytes para ler
                byte alive = dis.readByte(); //Ve se ta vivo
                int originalRecordSize = dis.readInt(); // Lê o tamanho do registro
                short jobId = dis.readShort(); // Lê o ID do registro
                byte[] data = new byte[originalRecordSize - 3]; // Já lemos o ID, então o resto é o conteúdo
                dis.readFully(data);
                job = deserializeJob(data); // deserialize = transformar array de bytes em objeto
                
                if (jobId == id && alive == 1) {
                    found = true;
                    foundId = jobId;
                    job.setJob_id(jobId);
                    updatedJob = JobDataCollector.updateJobData(sc, job);
                    int newRecordSize = updatedJob.getByteSize(); // tamanho do registro atualizado

                    if (newRecordSize <= originalRecordSize) {
                        updatedJob.toBytes(dos, alive, true, originalRecordSize); // talvez seria melhor se a gnt mudasse o toBytes pra n escrever a lapide e o tamanho do registro? aí faria por fora
                        int bytesEmBranco = originalRecordSize - newRecordSize;
                        for (int i = 0; i < bytesEmBranco; i++) {
                            dos.writeByte(0);
                        }
                        status = true;
                    } else { // registro atualizado ocupa mais espaço, ent matar o registro aq e colocar o atualizado no final
                        dos.writeByte(0); // 0 = morto
                        dos.writeInt(originalRecordSize);
                        dos.writeShort(jobId);
                        dos.write(data);
                        biggerRecord = true;
                        //System.out.println("BiggerRecord: " + biggerRecord);
                    }
                } else {
                    dos.writeByte(alive);
                    dos.writeInt(originalRecordSize);
                    dos.writeShort(jobId);
                    dos.write(data);
                }
            }

            // se o registro for maior que o original, escrever o atualizado no final
            if (biggerRecord) {
                //System.out.println("Escrevendo registro no fim do arquivo: " + biggerRecord);
                //System.out.println("Job ID: " + job.getJob_id());
                updatedJob.setJob_id((short) foundId);
                //System.out.println("Job ID: " + job.getJob_id());
                updatedJob.toBytes(dos, 1, false, 0);
                status = true;
            }

            fis.close();
            dis.close();
            fos.close();
            dos.close();

            // encontrado = arquivo temp se tornará o novo arquivo .db
            if (found) {
                File oldFile = new File(path);
                File newFile = new File(tempPath);
                if (oldFile.delete()) {
                    newFile.renameTo(oldFile);
                }
            } else { // não encontrado = nenhuma alteração feita, arquivo temp pode ir embora
                new File(tempPath).delete();
            }
        } catch (IOException e) {
            System.err.println("Erro em SecondaryToPrimary.java, updateJob: " + e);
            e.printStackTrace();
        }
        return status;
    }
    public static boolean updateJobRAF(int id, String path, Scanner sc, HashExtensivel he){
        Job job = new Job();
        Job updatedJob = new Job();
        short foundId = 0;
        boolean status = true;
        long pos = -1;
        long foundPos = -1;
        long newPos = -1;
        try {
            RandomAccessFile raf = new RandomAccessFile(path, "rw");
            raf.readInt();
            while(raf.getFilePointer() < raf.length()){
                pos = raf.getFilePointer();
                System.out.println("Procurando, pos atual: " + pos);
                byte alive = raf.readByte(); //Ve se ta vivo
                int originalRecordSize = raf.readInt(); // Lê o tamanho do registro
                short jobId = raf.readShort(); // Lê o ID do registro
                byte[] data = new byte[originalRecordSize - 3]; // Já lemos o ID, então o resto é o conteúdo
                raf.readFully(data);
                job = deserializeJob(data); // deserialize = transformar array de bytes em objeto
                job.setJob_id(jobId);
                System.out.println("Job encontrado: " + job.getJob_id());
                if(jobId == id && alive == 1){
                    System.out.println("Achei na posicao: " + pos);
                    foundPos = pos;
                    foundId = jobId;
                    job.setJob_id(jobId);
                    updatedJob = JobDataCollector.updateJobData(sc, job);
                    int newRecordSize = updatedJob.getByteSize(); // tamanho do registro atualizado
                    if(newRecordSize <= originalRecordSize){
                        raf.seek(pos);
                        System.out.println("Sobrescrevendo, o registro novo eh menor que o antigo");
                        updatedJob.toBytesRAF(raf, alive, true, originalRecordSize); // talvez seria melhor se a gnt mudasse o toBytes pra n escrever a lapide e o tamanho do registro? aí faria por fora
                        int bytesEmBranco = originalRecordSize - newRecordSize;
                        for (int i = 0; i < bytesEmBranco; i++) {
                            raf.writeByte(0);
                        }
                        status = true;
                        newPos = foundPos;
                    } else { // registro atualizado ocupa mais espaço, ent matar o registro aq e colocar o atualizado no final
                        raf.seek(pos);
                        System.out.println("Matando o registro atual e criando novo no final: " + raf.length());
                        raf.writeByte(0); // 0 = morto
                        raf.writeInt(originalRecordSize);
                        raf.writeShort(jobId);
                        raf.write(data);
                        newPos = raf.length();
                        raf.seek(newPos);
                        updatedJob.setJob_id((short)foundId);
                        updatedJob.toBytesRAF(raf, alive, status, newRecordSize);
                        //System.out.println("BiggerRecord: " + biggerRecord);
                    }
                } else {
                    raf.seek(pos);
                    raf.writeByte(alive);
                    raf.writeInt(originalRecordSize);
                    raf.writeShort(jobId);
                    raf.write(data);
                }
            }
            raf.close();
            System.out.println("newPos: " + newPos + " foundPos: " + foundPos);
            if(newPos != foundPos){
                boolean att = he.updateEndereco(updatedJob.getJob_id(), newPos);
                if(att){
                    System.out.println("Atualizado no Hash com sucesso!");
                } else {
                    System.out.println("Nao encontrado no hash");
                }
            }
        } catch (Exception e){
            System.err.println("Erro updateJobRAF: " + e);
        }
        return status;
    }
    /*
     * Função que transforma todos os registros em uma lista de objetos
     */
    public static ArrayList<Job> toPrimary(String path){
        ArrayList<Job> jobs = new ArrayList<Job>();
        try {
            FileInputStream arq = new FileInputStream(path);
            DataInputStream dis = new DataInputStream(arq);
            dis.readInt(); // pulando o primeiro byte que guarda o ultimo ID cadastrado
            while(dis.available() > 0) {
                Job job = new Job();
                byte alive = dis.readByte(); // lapide
                int recordSize = dis.readInt(); // tamanho do registro
                short jobId = dis.readShort(); // ID do registro
                byte[] data = new byte[recordSize - 3]; // Já lemos o ID, então o resto é o conteúdo
                dis.readFully(data);
                job = deserializeJob(data); // deserialize = transformar array de bytes em objeto
                job.setJob_id(jobId);
                if (alive == 1) { // adicionar na lista só se o registro tiver vivo
                jobs.add(job);
                }
            }
            arq.close();
            dis.close();
        } catch (Exception e){
            System.err.println("Erro em SecondaryToPrimary.java: " + e);
        }
        return jobs;
    }

    /*
     * Função auxiliar para editar campos com lista
     */
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

    /*
     * Função auxiliar para ler campos com lista
     */
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

    /*
     * Função para transformar registro em objeto
     */
    public static Job deserializeJob(byte[] data){
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

            // quando atualiza um job, se o registro ficar menor, vai ter bytes em branco no final
            while (dis.available() > 0) {
                dis.readByte();
            }
        } catch (Exception e){
            System.err.println("Erro no SecondaryToPrimary.java, DeserializeJob: " + e);
        }
        return job;
    }
}
