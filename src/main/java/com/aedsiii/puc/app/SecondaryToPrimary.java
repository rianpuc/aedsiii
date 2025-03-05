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
            job.toBytes(dos, 1, false, 0);
            last_id += 1;
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

            // colocando alterações em um arquivo novo pq n tamo usando RAF ainda, mas no fim só vai mudar o byte de lapide
            FileOutputStream fos = new FileOutputStream(tempPath);
            DataOutputStream dos = new DataOutputStream(fos);

            int lastId = dis.readInt();
            dos.writeInt(lastId);
            while (dis.available() > 0) {
                byte alive = dis.readByte();
                int recordSize = dis.readInt();
                short jobId = dis.readShort();
                byte[] data = new byte[recordSize - 3];
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
    public static boolean updateJob(int id, String path, Scanner sc) { // n vejo mt sentido em usar 2 arquivos sem raf enquanto usa uma logica de raf com 1 arquivo só, dps a gnt podia tirar essa duvida
        boolean status = false;
        boolean found = false;
        Job job = new Job();
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
                    JobDataCollector.updateJobData(sc, job);
                    int newRecordSize = job.getByteSize(); // tamanho do registro atualizado

                    if (newRecordSize <= originalRecordSize) {
                        job.toBytes(dos, alive, true, originalRecordSize); // talvez seria melhor se a gnt mudasse o toBytes pra n escrever a lapide e o tamanho do registro? aí faria por fora
                        int bytesEmBranco = originalRecordSize - newRecordSize;
                        for (int i = 0; i < bytesEmBranco; i++) {
                            dos.writeByte(0);
                        }
                        status = true;
                    } else { // registro atualizado ocupa mais espaço, ent matar o registro aq e colocar o atualizado no final
                        // Mark the old job as deleted
                        dos.writeByte(0); // 0 = morto
                        dos.writeInt(originalRecordSize);
                        dos.writeShort(jobId);
                        dos.write(data);
                        biggerRecord = true;
                        System.out.println("BiggerRecord: " + biggerRecord);
                    }
                } else {
                    dos.writeByte(alive);
                    dos.writeInt(originalRecordSize);
                    dos.writeShort(jobId);
                    dos.write(data);
                }
            }

            if (biggerRecord) { // se o registro for maior que o original, escrever o atualizado no final
                System.out.println("Escrevendo registro no fim do arquivo: " + biggerRecord);
                System.out.println("Job ID: " + job.getJob_id());
                job.setJob_id((short) foundId);
                System.out.println("Job ID: " + job.getJob_id());
                job.toBytes(dos, 1, false, 0);
                status = true;
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
        } catch (IOException e) {
            System.err.println("Erro em SecondaryToPrimary.java, updateJob: " + e);
            e.printStackTrace();
        }
        return status;
    }
    public static ArrayList<Job> toPrimary(String path){ // pegar jobs do db e transformar tudo em uma listona
        ArrayList<Job> jobs = new ArrayList<Job>();
        try {
            FileInputStream arq = new FileInputStream(path);
            DataInputStream dis = new DataInputStream(arq);
            dis.readInt(); //pulando o primeiro byte que guarda o ultimo ID cadastrado
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
