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

    /*
     * Função para criação de um novo job
     */
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
        System.out.println("Data de Postagem da Vaga: (dd/MM/yyyy)");
        String inputDate = sc.nextLine();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDate localDate = LocalDate.parse(inputDate, formatter);
        LocalDateTime localDateTime = localDate.atStartOfDay();
        job_posting_date = localDateTime.toInstant(ZoneOffset.UTC);
        System.out.println("Preferencia de Genero? (Male, Female ou Both)");
        preference = sc.nextLine();
        System.out.println("Nome da Pessoa para Contato: ");
        contact_person = sc.nextLine();
        System.out.println("Contato da Pessoa (Telefone ou Celular): ");
        contact = sc.nextLine();
        System.out.println("Titulo do Trabalho: ");
        job_title = sc.nextLine();
        System.out.println("Cargo: ");
        role = sc.nextLine();
        System.out.println("Local de Divulgacao da Vaga: ");
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

    /*
     * Função para atualizar um job
     */
    public static Job updateJobData(Scanner sc, Job job) {
        while (true) {
            System.out.println("\tQual atributo você deseja atualizar?");
            System.out.println("\t1.  Experiência\t\t\t12. Pessoa de Contato");
            System.out.println("\t2.  Qualificação\t\t13. Contato");
            System.out.println("\t3.  Faixa Salarial\t\t14. Título do Trabalho");
            System.out.println("\t4.  Cidade\t\t\t15. Cargo");
            System.out.println("\t5.  País\t\t\t16. Portal de Vagas");
            System.out.println("\t6.  Latitude\t\t\t17. Descrição da Vaga");
            System.out.println("\t7.  Longitude\t\t\t18. Benefícios");
            System.out.println("\t8.  Tipo de Trabalho\t\t19. Habilidades");
            System.out.println("\t9.  Tamanho da Empresa\t\t20. Responsabilidades");
            System.out.println("\t10. Data de Postagem da Vaga\t21. Nome da Empresa");
            System.out.println("\t11. Preferência de Gênero\t22. Perfil da Empresa");
            System.out.println("\t0.  Finalizar");
            System.out.print("\tEscolha uma opção: ");
            int choice = Integer.parseInt(sc.nextLine());
    
            switch (choice) {
                case 1:
                    System.out.println("Experiencia? (XX to XX Years)");
                    job.setExperience(sc.nextLine());
                    break;
                case 2:
                    System.out.println("Qualificacao? (PhD, M.Tech, BCA, BBA, etc.)");
                    job.setQualification(sc.nextLine());
                    break;
                case 3:
                    System.out.println("Salario? ($XX-$XX)");
                    job.setSalary_range(sc.nextLine());
                    break;
                case 4:
                    System.out.println("Cidade?");
                    job.setLocation(sc.nextLine());
                    break;
                case 5:
                    System.out.println("Pais?");
                    job.setCountry(sc.nextLine());
                    break;
                case 6:
                    System.out.println("Latitude? (4 decimais)");
                    job.setLatitude(Float.parseFloat(sc.nextLine()));
                    break;
                case 7:
                    System.out.println("Longitude? (4 decimais)");
                    job.setLongitude(Float.parseFloat(sc.nextLine()));
                    break;
                case 8:
                    System.out.println("Tipo de Trabalho? (Full-Time, Intern, Contract, etc.)");
                    job.setWork_type(sc.nextLine());
                    break;
                case 9:
                    System.out.println("Quantidade de Funcionarios na Empresa?");
                    job.setCompany_size(Integer.parseInt(sc.nextLine()));
                    break;
                case 10:
                    System.out.println("Data de Postagem da Vaga: (dd/MM/yyyy)");
                    String inputDate = sc.nextLine();
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                    LocalDate localDate = LocalDate.parse(inputDate, formatter);
                    LocalDateTime localDateTime = localDate.atStartOfDay();
                    job.setJob_posting_date(localDateTime.toInstant(ZoneOffset.UTC));
                    break;
                case 11:
                    System.out.println("Preferencia de Genero? (Male, Female ou Both)");
                    job.setPreference(sc.nextLine());
                    break;
                case 12:
                    System.out.println("Nome da Pessoa para Contato: ");
                    job.setContact_person(sc.nextLine());
                    break;
                case 13:
                    System.out.println("Contato da Pessoa (Telefone ou Celular): ");
                    job.setContact(sc.nextLine());
                    break;
                case 14:
                    System.out.println("Titulo do Trabalho: ");
                    job.setJob_title(sc.nextLine());
                    break;
                case 15:
                    System.out.println("Cargo: ");
                    job.setRole(sc.nextLine());
                    break;
                case 16:
                    System.out.println("Local de Divulgacao da Vaga: ");
                    job.setCompany_profile(sc.nextLine());
                    break;
                case 17:
                    System.out.println("Descricao da Vaga: ");
                    job.setJob_description(sc.nextLine());
                    break;
                case 18:
                    List<String> benefits = new ArrayList<>();
                    addToList(sc, "Digite um benefício (ou 'PARAR' para terminar):", benefits);
                    job.setBenefits(benefits);
                    break;
                case 19:
                    List<String> skills = new ArrayList<>();
                    addToList(sc, "Digite uma habilidade necessária (ou 'PARAR' para terminar):", skills);
                    job.setSkills(skills);
                    break;
                case 20:
                    List<String> responsibilities = new ArrayList<>();
                    addToList(sc, "Digite uma responsabilidade do cargo (ou 'PARAR' para terminar):", responsibilities);
                    job.setResponsibilities(responsibilities);
                    break;
                case 21:
                    System.out.println("Nome da Empresa: ");
                    job.setCompany(sc.nextLine());
                    break;
                case 22:
                    System.out.println("Perfil da Empresa (Website): ");
                    job.setCompany_profile(sc.nextLine());
                    break;
                case 0:
                    return job;
                default:
                    System.out.println("Opção inválida. Tente novamente.");
            }
        }
    }

    /*
     * Função auxiliar para criação das listas
     */
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
