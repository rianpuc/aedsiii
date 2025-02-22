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
    private static final String CONFIG_FILE = "config.properties";
    public static void printMenu(){
        System.out.printf("\t1. Inserir\n" +
                          "\t2. Editar\n" +
                          "\t3. Atualizar\n" +
                          "\t4. Remover\n" +
                          "\t5. Mostrar\n" +
                          "\t0. Sair\n" +
                          "\tOpcao: ");
    }
    public static void main(String[] args) {
        Properties config = new Properties();
        File configFile = new File(CONFIG_FILE);
        String dbPath = null;
        try {
            if (configFile.exists()) {
                try (FileInputStream fis = new FileInputStream(configFile)) {
                    config.load(fis);
                }
            }
            dbPath = config.getProperty("binary.path");
            if (dbPath == null || dbPath.isEmpty()) {
                ArrayList<Job> jobs = FileParser.parseFile();
                PrimaryToSecondary.toSecondary(jobs);
                config.setProperty("binary.path", "binary_db.db");
                try (FileOutputStream fos = new FileOutputStream(configFile)) {
                    config.store(fos,  "Guardando o local do binario");
                }
            }
        } catch (IOException e){
            e.printStackTrace();
        }
        Scanner sc = new Scanner(System.in);
        int answer = -1;
        while(answer != 0){
            printMenu();
            answer = sc.nextInt();
            switch (answer) {
                case 1:
                    break;
                case 2:
                    break;
                case 3:
                    break;
                case 4:
                    break;
                case 5:
                    ArrayList<Job> jobs = SecondaryToPrimary.toPrimary("binary_db.db");
                    for (Job job : jobs) {
                        job.mostrar();
                    }
                    break;
                case 0:
                    break;
            }
        }
        sc.close();
    }
}