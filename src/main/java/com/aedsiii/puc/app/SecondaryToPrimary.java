package com.aedsiii.puc.app;
import java.util.ArrayList;
import java.util.List;
import java.io.FileInputStream;
import java.time.Instant;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import com.aedsiii.puc.model.Job;

public class SecondaryToPrimary {
    public static Job getJob(int id, String path){
        Job job = new Job();
        try {
            FileInputStream arq = new FileInputStream(path);
            DataInputStream dis = new DataInputStream(arq);
            dis.readInt(); //pulando o primeiro byte que guarda o ultimo ID cadastrado
            while (dis.available() > 0) { // Enquanto houver bytes para ler
                int recordSize = dis.readInt(); // Lê o tamanho do registro
                byte alive = dis.readByte(); //Ve se ta vivo
                short jobId = dis.readShort(); // Lê o ID do registro
                //System.out.printf("Job ID Atual: %d\nTamanho: %d\n", jobId, recordSize);
                if (jobId == id && alive == 1) {
                    byte[] data = new byte[recordSize - 3]; // Já lemos o ID, então o resto é o conteúdo
                    dis.readFully(data);
                    job = deserializeJob(data);
                    job.setJob_id(jobId);
                    break; // Para a leitura assim que encontramos o ID
                } else {
                    dis.skipBytes(recordSize - 3); // Pula o restante do registro
                }
            }
            arq.close();
            dis.close();
        } catch (Exception e){
            System.err.println("Erro no SecondaryToPrimary.java, getJob: " + e);
        }
        return job;
    }
    public static ArrayList<Job> toPrimary(String path){
        ArrayList<Job> jobs = new ArrayList<Job>();
        try {
            FileInputStream arq = new FileInputStream(path);
            DataInputStream dis = new DataInputStream(arq);
            dis.readInt(); //pulando o primeiro byte que guarda o ultimo ID cadastrado
            while(dis.available() > 0) {
                Job job = new Job();
                //System.out.printf("Tamanho do registro: %d bytes\nAtivo: %d\n", dis.readInt(), dis.readByte());
                dis.readInt();
                dis.readByte();
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
