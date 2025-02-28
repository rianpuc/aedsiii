package com.aedsiii.puc.app;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;

import com.aedsiii.puc.model.Job;

public class SecondaryToPrimary {
    public static int addJob(String path){
        int last_id = -1;
        try {
            Scanner sc = new Scanner(System.in);
            FileInputStream arq = new FileInputStream(path);
            DataInputStream dis = new DataInputStream(arq);
            last_id = dis.readInt();
            arq.close();
            dis.close();
            FileOutputStream fos = new FileOutputStream(path, true);
            DataOutputStream dos = new DataOutputStream(fos);
            String experience;
            String qualification;
            String salary_range;
            String location;
            String country;
            float latitude;
            float longitude;
            String work_type;
            int company_size;
            Instant job_posting_date;
            String preference;
            String contact_person;
            String contact;
            String job_title;
            String role;
            String job_portal;
            String job_description;
            String company;
            String company_profile;
            System.out.println("Experiencia? (XX to XX Years)");
            experience = sc.nextLine();
            System.out.println("Qualificacao? (PhD, M.Tech, BCA, BBA, etc.)");
            qualification = sc.nextLine();
            System.out.println("Salario? ($XX-$XX)");
            salary_range = sc.nextLine();
            System.out.println("Cidade?");
            location = sc.nextLine();
            System.out.println("Pais?");
            country = sc.nextLine();
            System.out.println("Latitude? (4 decimais)");
            latitude = sc.nextFloat();
            sc.nextLine();
            System.out.println("Longitude? (4 decimais)");
            longitude = sc.nextFloat();
            sc.nextLine();
            System.out.println("Tipo de Trabalho? (Full-Time, Intern, Contract, etc.)");
            work_type = sc.nextLine();
            System.out.println("Quantidade de Funcionarios na Empresa?");
            company_size = sc.nextInt();
            sc.nextLine();
            System.out.println("Data que foi postada a vaga: (dd/MM/yyyy)");
            String inputDate = sc.nextLine();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            LocalDate localDate = LocalDate.parse(inputDate, formatter);
            LocalDateTime localDateTime = localDate.atStartOfDay();
            job_posting_date = localDateTime.toInstant(ZoneOffset.UTC);
            System.out.println("Preferencia de Genero? (Male, Female or Both)");
            preference = sc.nextLine();
            System.out.println("Nome da Pessoa para Contato: ");
            contact_person = sc.nextLine();
            System.out.println("Contato da Pessoa (Telefone ou Celular): ");
            contact = sc.nextLine();
            System.out.println("Titulo do Trabalho: ");
            job_title = sc.nextLine();
            System.out.println("Cargo do Trabalho: ");
            role = sc.nextLine();
            System.out.println("Onde a Vaga foi Divulgada: ");
            job_portal = sc.nextLine();
            System.out.println("Descricao da Vaga: ");
            job_description = sc.nextLine();
            List<String> benefits = new ArrayList<>();
            List<String> skills = new ArrayList<>();
            List<String> responsibilities = new ArrayList<>();
            addToList(sc, "Digite um benefício (ou 'PARAR' para terminar):", benefits);
            addToList(sc, "Digite uma habilidade necessária (ou 'PARAR' para terminar):", skills);
            addToList(sc, "Digite uma responsabilidade do cargo (ou 'PARAR' para terminar):", responsibilities);
            System.out.println("Nome da Empresa: ");
            company = sc.nextLine();
            System.out.println("Perfil da Empresa (Website, Localizacao, etc.): ");
            company_profile = sc.nextLine();
            Job job = new Job((short)++last_id, experience, qualification, salary_range, location, country, latitude, longitude, work_type, company_size, job_posting_date, preference, contact_person, contact, job_title, role,
            job_portal, job_description, benefits, skills, responsibilities, company, company_profile);
            job.toBytes(dos);
        } catch (Exception e){
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
    public static boolean removeJob(int id, String path){
        boolean found = false;
        String tempPath = path + ".tmp";
        try {
            FileInputStream fis = new FileInputStream(path);
            DataInputStream dis = new DataInputStream(fis);
            FileOutputStream fos = new FileOutputStream(tempPath);
            DataOutputStream dos = new DataOutputStream(fos);

            int lastId = dis.readInt();
            dos.writeInt(lastId);
            while (dis.available() > 0) {
                int recordSize = dis.readInt();
                byte alive = dis.readByte();
                short jobId = dis.readShort();
                byte[] data = new byte[recordSize - 3];
                dis.readFully(data);
                dos.writeInt(recordSize);
                if (jobId == id && alive == 1) {
                    dos.writeByte(0);
                    found = true;
                } else {
                    dos.writeByte(alive);
                }
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
            } else {
                new File(tempPath).delete();
            }
        } catch (Exception e){
            System.err.println("Erro em SecondaryToPrimary.java, removeJob: " + e);
        }
        return found;
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
