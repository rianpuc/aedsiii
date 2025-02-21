package com.aedsiii.puc.app;
import java.util.ArrayList;
import java.util.List;
import java.io.FileInputStream;
import java.time.Instant;
import java.io.DataInputStream;
import com.aedsiii.puc.model.Job;

public class SecondaryToPrimary {
    public static ArrayList<Job> toPrimary(String path){
        ArrayList<Job> jobs = new ArrayList<Job>();
        try {
            FileInputStream arq = new FileInputStream(path);
            DataInputStream dis = new DataInputStream(arq);
            dis.readInt(); //pulando o primeiro byte que guarda o ultimo ID cadastrado
            while(dis.available() > 0) {
                Job job = new Job();
                System.out.printf("Tamanho do registro: %d bytes\nAtivo: %d\n", dis.readInt(), dis.readByte());
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
}
