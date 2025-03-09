package com.aedsiii.puc.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Scanner;
import com.aedsiii.puc.model.Job;

public class Main {
    private static final String DB_PATH = "binary_db.db";
    private static final String CONFIG_FILE = "config.properties";
    public static void printMenu(){
        System.out.printf("\t1. Inserir\n" +
                          "\t2. Editar\n" +
                          "\t3. Remover\n" +
                          "\t4. Mostrar\n" +
                          "\t5. Get\n" +
                          "\t0. Sair\n" +
                          "\tOpcao: ");
    }
    public static void main(String[] args) {
        Properties config = new Properties();
        File configFile = new File(CONFIG_FILE);
        try {
            if (configFile.exists()) {
                try (FileInputStream fis = new FileInputStream(configFile)) {
                    config.load(fis);
                }
            }
            String dbPath = config.getProperty("binary.path");
            if (dbPath == null || dbPath.isEmpty()) {
                ArrayList<Job> jobs = FileParser.parseFile();
                PrimaryToSecondary.toSecondary(jobs);
                config.setProperty("binary.path", DB_PATH);
                try (FileOutputStream fos = new FileOutputStream(configFile)) {
                    config.store(fos,  "Guardando o local do binario");
                }
            }
        } catch (IOException e){
            e.printStackTrace();
        }
        Scanner sc = new Scanner(System.in);
        int answer = -1;
        int id;
        while(answer != 0){
            printMenu();
            answer = Integer.parseInt(sc.nextLine());
            switch (answer) {
                case 1: // addJob
                    Job addJob = JobDataCollector.collectJobData(sc);
                    id = SecondaryToPrimary.addJob(addJob, DB_PATH);
                    if(id != -1){
                        System.out.println("Nova vaga adicionada com sucesso! ID: " + id);
                    }
                    break;
                case 2: // editJob
                    System.out.println("Informe o ID da vaga a ser editada: ");
                    id = Integer.parseInt(sc.nextLine());
                    boolean status = SecondaryToPrimary.updateJob(id, DB_PATH, sc);
                    if (status) {
                        System.out.println("Vaga editada com sucesso! ID: " + id);
                    } else {
                        System.out.println("Vaga não encontrada. ID: " + id);
                    }
                    break;
                case 3: // removeJob
                    System.out.println("Insira o ID: ");
                    id = Integer.parseInt(sc.nextLine());
                    boolean res = SecondaryToPrimary.removeJob(id, DB_PATH);
                    if(res){
                        System.out.println("Registro com ID " + id + " removido!");
                    } else {
                        System.out.println("Registro nao encontrado.");
                    }
                    break;
                case 4: // mostrar todos os jobs
                    ArrayList<Job> jobs = SecondaryToPrimary.toPrimary(DB_PATH);
                    for (Job job : jobs) {
                        System.out.println(job);
                    }
                    break;
                case 5: // getJob
                    System.out.println("Insira o ID: ");
                    id = Integer.parseInt(sc.nextLine());
                    Job job = SecondaryToPrimary.getJob(id, DB_PATH);
                    if(job.getJob_id() != -1) {
                        System.out.println(job);
                    }
                    else {
                        System.out.println("Registro nao encontrado.");
                    }
                    break;
                case 0:
                    break;
                default:
                    System.out.println("Opção inválida. Tente novamente.");
            }
        }
        sc.close();
    }
}