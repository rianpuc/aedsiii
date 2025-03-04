package com.aedsiii.puc.app;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.aedsiii.puc.model.Job;

public class JobDataCollector { // fiz pra deixar separado a coleta de informações usando o scanner
    public static Job collectJobData(Scanner sc) { // o id vai ser atribuido no SecondaryToPrimary
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
        latitude = Float.parseFloat(sc.nextLine());
        System.out.println("Longitude? (4 decimais)");
        longitude = Float.parseFloat(sc.nextLine());
        System.out.println("Tipo de Trabalho? (Full-Time, Intern, Contract, etc.)");
        work_type = sc.nextLine();
        System.out.println("Quantidade de Funcionarios na Empresa?");
        company_size = Integer.parseInt(sc.nextLine());
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

        // id -1 temporario
        return new Job((short) -1, experience, qualification, salary_range, location, country, latitude, longitude, work_type, company_size, job_posting_date, preference, contact_person, contact, job_title, role,
            job_portal, job_description, benefits, skills, responsibilities, company, company_profile);
    }

    private static void addToList(Scanner sc, String message, List<String> list) {
        while (true) {
            System.out.println(message);
            String input = sc.nextLine();
            if (input.trim().equalsIgnoreCase("PARAR")) {
                break;
            }
            list.add(input);
        }
    }
}
