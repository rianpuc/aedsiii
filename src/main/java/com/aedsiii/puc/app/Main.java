package com.aedsiii.puc.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Scanner;
import com.aedsiii.puc.model.Job;
import com.aedsiii.puc.model.PaginaBTree;
import com.aedsiii.puc.model.RegistroBTree;
import com.aedsiii.puc.model.RegistroHashExtensivel;

public class Main {
    private static final String DB_PATH = "binary_db.db";
    private static final String CONFIG_FILE = "config.properties";
    private static final String EXTERNAL_SORT_PATH = "./external_sort";
    private static final String HE_PATH = "./hash_extensivel";
    private static final String BTREE_PATH = "btree.db";
    private static String METHOD = "";
    private static int BTREE_ORDER = -1;
    private static int HASH_CESTOS = -1;

    public static void printMenu(){
        System.out.printf("\tMetodo atual:\t");
        switch (METHOD) {
            case "sequencial":
                System.out.printf("Sequencial\n");
                break;
            case "btree":
                System.out.printf("B-Tree\t Ordem: %d\n", BTREE_ORDER);
                break;
            case "hash":
                System.out.printf("Hash Extensivel\t Cestos: %d\n", HASH_CESTOS);
                break;
            case "lista":
                break;
        }
        System.out.printf("\t1. Inserir\n" +
                          "\t2. Get\n" +
                          "\t3. Editar\n" +
                          "\t4. Remover\n" +
                          "\t5. Mostrar todos\n" +
                          "\t6. Reordenar banco de dados\n");
    }
    public static void main(String[] args) throws IOException{
        Scanner sc = new Scanner(System.in);
        changeIndexMethod(sc, true);
        int answer = -1;
        int id;
        //System.out.println("BTREE_ORDER: " +  BTREE_ORDER);
        // Variáveis auxiliares
        Job job = new Job();
        RegistroHashExtensivel registroHE = new RegistroHashExtensivel();
        RegistroBTree registroBTree = new RegistroBTree();
        ArrayList<RegistroBTree> registrosBT = new ArrayList<RegistroBTree>();        
        HashExtensivel he = null;
        BTree btree = null;
        while(answer != 0){
            switch (METHOD) {
                case "btree":
                    // Verificar se já tem uma árvore B em memória secundária
                    File btreeAuxFile = new File(BTREE_PATH);
                    if (btreeAuxFile.exists()) {
                        RandomAccessFile raf = new RandomAccessFile(btreeAuxFile, "rw");
                        if (btreeAuxFile.length() > 4) {
                            BTREE_ORDER = raf.readInt();
                        }
                        raf.close();
                        //System.out.println("BTREE_ORDER: " +  BTREE_ORDER);
                        btree = new BTree(BTREE_ORDER, BTREE_PATH, false);
                    } else {
                        registrosBT = KeyDataCreator.criarPares(DB_PATH);
                        if (registrosBT.isEmpty()) {
                            System.out.println("Dataset vazio.");
                            break;
                        }
                        btree = new BTree(BTREE_ORDER, BTREE_PATH, true);
                        btree.pagina = new PaginaBTree(BTREE_ORDER);
                        for (RegistroBTree reg : registrosBT) {
                            btree.create(reg);
                        }
                        System.out.println("Árvore de ordem [" + BTREE_ORDER + "] criada.");
                    }
                    break;
                case "hash":
                    try {
                        File d = new File(HE_PATH);
                        if(d.exists()) { 
                            File diretorioDB = new File(HE_PATH + "/jobs_diretorio.db");
                            File cestosDB = new File(HE_PATH + "/jobs_cestos.db");
                            he = new HashExtensivel(HASH_CESTOS, HE_PATH + "/jobs_diretorio.db", HE_PATH + "/jobs_cestos.db", false);
                            if(he.cestosSize() != (short)HASH_CESTOS){
                                he = null;
                                System.gc();
                                diretorioDB.delete();
                                cestosDB.delete();
                                ArrayList<RegistroHashExtensivel> registrosHE = KeyDataCreator.criarParesHE(DB_PATH);
                                try {
                                    System.gc();
                                    he = new HashExtensivel(HASH_CESTOS, HE_PATH + "/jobs_diretorio.db", HE_PATH + "/jobs_cestos.db", true);
                                    for(RegistroHashExtensivel reg : registrosHE){
                                        he.create(reg);
                                    }
                                } catch (Exception e){
                                    System.err.println("Erro ao criar HashExtensivel Linha 424: " + e);
                                }
                            }
                        }
                        else {
                            d.mkdir();
                            ArrayList<RegistroHashExtensivel> registrosHE = KeyDataCreator.criarParesHE(DB_PATH);
                            try {
                                he = new HashExtensivel(HASH_CESTOS, HE_PATH + "/jobs_diretorio.db", HE_PATH + "/jobs_cestos.db", true);
                                for(RegistroHashExtensivel reg : registrosHE){
                                    he.create(reg);
                                }
                            } catch (Exception e){
                                System.err.println("Erro ao criar HashExtensivel Linha 436: " + e);
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Erro ao criar HashExtensivel: " + e);
                    }
                default:
                    break;
            }
            printMenu();
            answer = Integer.parseInt(sc.nextLine());
            switch (answer) {
                case 1: // addJob
                    // Salvando offset da inserção do novo registro
                    RandomAccessFile addjRaf = new RandomAccessFile(DB_PATH, "rw");
                    long addJobOffset = addjRaf.length();
                    addjRaf.close();
                    Job newJob = JobDataCollector.collectJobData(sc);
                    id = SecondaryToPrimary.addJob(newJob, DB_PATH);
                    if(id != -1){
                        System.out.println("Nova vaga adicionada com sucesso! ID: " + id);
                    }
                    switch(METHOD){
                        case "btree":
                            RegistroBTree addJobRBT = new RegistroBTree(newJob.getJob_id(), addJobOffset);
                            btree.create(addJobRBT);
                            break;
                        case "hash":
                            RegistroHashExtensivel addJobRHE = new RegistroHashExtensivel(newJob.getJob_id(), addJobOffset);
                            try {
                                he.create(addJobRHE);
                            } catch (Exception e){
                                System.out.println("Erro ao adicionar Job em HashExtensivel: " + e);
                            }
                            break;
                    }
                    break;
                case 2: // getJob
                    System.out.println("Insira o ID: ");
                    try {
                        id = Integer.parseInt(sc.nextLine());
                    } catch (NumberFormatException e) {
                        System.out.println("Entrada inválida. Tente novamente.");
                        continue;
                    }
                    switch (METHOD) {
                        case "sequencial":
                            job = SecondaryToPrimary.getJob(id, DB_PATH);
                            if (job.getJob_id() != -1) {
                                System.out.println(job);
                            }
                            else {
                                System.out.println("Registro não encontrado.");
                            }
                            break;
                        case "btree":
                            registroBTree = btree.search(id);
                            if (registroBTree.id != -1) {
                                job = OffsetReader.readInOffset(registroBTree.offset, DB_PATH);
                                System.out.println(job);
                            } else {
                                System.out.println("Registro não encontrado.");
                            }
                            break;
                        case "hash":
                            try {
                                registroHE = he.read(id);
                                System.out.println(registroHE);
                            } catch (Exception e){
                                System.err.println("Erro 307: " + e);
                            }
                            if (registroHE.id != -1) {
                                job = OffsetReader.readInOffset(registroHE.offset, DB_PATH);
                                System.out.println(job);
                            } else {
                                System.out.println("Registro não encontrado.");
                            }
                            break;
                        default:
                            break;
                    }
                    break;
                case 3: // editJob
                    System.out.println("Informe o ID da vaga a ser editada: ");
                    try {
                        id = Integer.parseInt(sc.nextLine());
                    } catch (NumberFormatException e) {
                        System.out.println("Entrada inválida. Por favor, insira um número.");
                        continue;
                    }
                    switch (METHOD) {
                        case "sequencial":
                            boolean status = SecondaryToPrimary.updateJob(id, DB_PATH, sc);
                            if (status) {
                                System.out.println("Vaga editada com sucesso! ID: " + id);
                            } else {
                                System.out.println("Vaga não encontrada. ID: " + id);
                            }
                            break;
                        case "hash":
                            status = SecondaryToPrimary.updateJobRAF(id, DB_PATH, sc, he);
                            if (status) {
                                System.out.println("Vaga editada com sucesso! ID: " + id);
                            } else {
                                System.out.println("Vaga não encontrada. ID: " + id);
                            }
                        default:
                            break;
                    }
                    break;
                case 4: // removeJob
                    System.out.println("Insira o ID: ");
                    try {
                        id = Integer.parseInt(sc.nextLine());
                    } catch (NumberFormatException e) {
                        System.out.println("Entrada inválida. Por favor, insira um número.");
                        continue;
                    }
                    boolean res = SecondaryToPrimary.removeJob(id, DB_PATH);
                    if(res){
                        System.out.println("Registro com ID " + id + " removido!");
                    } else {
                        System.out.println("Registro não encontrado.");
                    }
                    break;
                case 5: // mostrar todos os jobs
                    switch (METHOD) {
                        case "sequencial":
                            ArrayList<Job> jobs = SecondaryToPrimary.toPrimary(DB_PATH);
                            for (Job job_5 : jobs) {
                                System.out.println(job_5);
                            }
                            break;
                        case "btree":
                            if (btree != null) {
                                btree.print();
                            } else {
                                System.out.println("Árvore B não criada.");
                            }
                            break;
                        case "hash":
                            if (he != null){
                                he.print();
                            } else {
                                System.out.println("Hash Extensivel não criado");
                            }
                            break;
                        default:
                            break;
                    }
                    break;
                case 6: // ordenação externa
                    System.out.println("Insira o limite de registros na memória primária: ");
                    int b_registros;
                    try {
                        b_registros = Integer.parseInt(sc.nextLine());
                    } catch (NumberFormatException e) {
                        System.out.println("Entrada inválida. Por favor, insira um número.");
                        continue;
                    }
                    System.out.println("Insira o número de caminhos a serem usados: ");
                    int m_caminhos;
                    try {
                        m_caminhos = Integer.parseInt(sc.nextLine());
                    } catch (NumberFormatException e) {
                        System.out.println("Entrada inválida. Por favor, insira um número.");
                        continue;
                    }
                    ExternalSort.sort(b_registros, m_caminhos, DB_PATH, EXTERNAL_SORT_PATH);

                    // Teste leitura da distribuição inicial
                    //ExternalSort.test_read(EXTERNAL_SORT_PATH, m_caminhos);
                case 10:
                    short deleteID;
                    System.out.println("escolhe id");
                    deleteID = Short.parseShort(sc.nextLine());
                    boolean deleteCheck = btree.delete(deleteID);
                    if (deleteCheck) {
                        System.out.println("Registro com ID " + deleteID + " removido!");
                    } else {
                        System.out.println("Registro não encontrado.");
                    }
                    if (btree.antecessoraPendente) {
                        // System.out.println("Indo deletar antecessora pendente: " + btree.auxKey.id);
                        btree.delete(btree.auxKey.id);
                        btree.antecessoraPendente = false;
                    }
                    break;
                case 14:
                    changeIndexMethod(sc, false);
                    break;
                case 0:
                    break;
                default:
                    System.out.println("Opção inválida. Tente novamente.");
            }
        }
        sc.close();
    }
    public static void changeIndexMethod(Scanner sc, boolean load) {
        Properties config = new Properties();
        File configFile = new File(CONFIG_FILE);
        try {
            if (configFile.exists()) {
                try (FileInputStream fis = new FileInputStream(configFile)) {
                    config.load(fis);
                }
            }
            String dbPath = config.getProperty("binary.path");
            // String smallDbPath = config.getProperty("small_binary.path");
            if (dbPath == null || dbPath.isEmpty() /*|| smallDbPath == null || smallDbPath.isEmpty()*/) {
                // PRIMEIRA ESCOLHA É O DB ORIGINAL
                ArrayList<Job> jobs = FileParser.parseFile();
                PrimaryToSecondary.toSecondary(jobs, DB_PATH);
                config.setProperty("binary.path", DB_PATH);

                // SEGUNDA ESCOLHA É O DB MENOR DE TESTES
                // ArrayList<Job> smallerJobs = FileParser.parseFile();
                // PrimaryToSecondary.toSecondary(smallerJobs, "smaller_binary_db.db");
                // config.setProperty("small_binary.path", "smaller_binary_db.db");

                try (FileOutputStream fos = new FileOutputStream(configFile)) {
                    config.store(fos,  "Guardando o local do binario");
                }
            }
            String indexMethod = config.getProperty("index.method");
            if (!load || indexMethod == null) {
                int select;
                System.out.println("Selecione o metodo de armazenamento: \n" + 
                                    "1. Sequencial\n" +
                                    "2. B-Tree\n" +
                                    "3. Hash Extensivel\n" +
                                    "4. Lista invertida\n");
                select = Integer.parseInt(sc.nextLine());
                switch(select){
                    case 1:
                        config.setProperty("index.method", "sequencial");
                        config.remove("btree.ordem");
                        config.remove("hash.cestos");
                        break;
                    case 2:
                        System.out.println("Digite a ordem da arvore: \n");
                        int o = Integer.parseInt(sc.nextLine());
                        config.setProperty("index.method", "btree");
                        config.setProperty("btree.ordem", "" + o);
                        config.remove("hash.cestos");
                        break;
                    case 3:
                        System.out.println("Digite o tamanho do cesto: \n");
                        int c = Integer.parseInt(sc.nextLine());
                        config.setProperty("index.method", "hash");
                        config.setProperty("hash.cestos", "" + c);
                        config.remove("btree.ordem");
                        break;
                    case 4:
                        //lista invertida aqui//
                        config.setProperty("index.method", "lista");
                        config.remove("btree.ordem");
                        config.remove("hash.cestos");
                        break;
                    default:
                        System.out.println("Por favor selecione uma opcao valida.");
                        return;
                }
                try (FileOutputStream fos = new FileOutputStream(configFile)) {
                    config.store(fos,  "Guardando o index method");
                }
                try (FileInputStream fis = new FileInputStream(configFile)) {
                    config.load(fis);
                }
            }
            METHOD = config.getProperty("index.method");
            switch (METHOD) {
                case "sequencial":
                    break;
                case "btree":
                    BTREE_ORDER = Integer.parseInt(config.getProperty("btree.ordem"));
                    break;
                case "hash":
                    HASH_CESTOS = Integer.parseInt(config.getProperty("hash.cestos"));
                    break;
                case "lista":
                    break;
                default:
                    System.out.println("Método desconhecido.");
                    System.out.println(METHOD);
                    break;
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }
    public static void loadNewMethod(BTree btree, HashExtensivel he, ArrayList<RegistroBTree> registrosBT, RegistroBTree registroBTree, RegistroHashExtensivel registroHE) throws IOException{
        switch (METHOD) {
            case "btree":
                // Verificar se já tem uma árvore B em memória secundária
                File btreeAuxFile = new File(BTREE_PATH);
                if (btreeAuxFile.exists()) {
                    RandomAccessFile raf = new RandomAccessFile(btreeAuxFile, "rw");
                    if (btreeAuxFile.length() > 4) {
                        BTREE_ORDER = raf.readInt();
                    }
                    raf.close();
                    //System.out.println("BTREE_ORDER: " +  BTREE_ORDER);
                    btree = new BTree(BTREE_ORDER, BTREE_PATH, false);
                } else {
                    registrosBT = KeyDataCreator.criarPares(DB_PATH);
                    if (registrosBT.isEmpty()) {
                        System.out.println("Dataset vazio.");
                        break;
                    }
                    btree = new BTree(BTREE_ORDER, BTREE_PATH, true);
                    btree.pagina = new PaginaBTree(BTREE_ORDER);
                    for (RegistroBTree reg : registrosBT) {
                        btree.create(reg);
                    }
                    System.out.println("Árvore de ordem [" + BTREE_ORDER + "] criada.");
                }
                break;
            case "hash":
                try {
                    File d = new File(HE_PATH);
                    if(d.exists()) { 
                        File diretorioDB = new File(HE_PATH + "/jobs_diretorio.db");
                        File cestosDB = new File(HE_PATH + "/jobs_cestos.db");
                        he = new HashExtensivel(HASH_CESTOS, HE_PATH + "/jobs_diretorio.db", HE_PATH + "/jobs_cestos.db", false);
                        if(he.cestosSize() != (short)HASH_CESTOS){
                            diretorioDB.delete();
                            cestosDB.delete();
                            ArrayList<RegistroHashExtensivel> registrosHE = KeyDataCreator.criarParesHE(DB_PATH);
                            try {
                                System.gc();
                                he = new HashExtensivel(HASH_CESTOS, HE_PATH + "/jobs_diretorio.db", HE_PATH + "/jobs_cestos.db", true);
                                for(RegistroHashExtensivel reg : registrosHE){
                                    he.create(reg);
                                }
                            } catch (Exception e){
                                System.err.println("Erro ao criar HashExtensivel Linha 424: " + e);
                            }
                        }
                    }
                    else {
                        d.mkdir();
                        ArrayList<RegistroHashExtensivel> registrosHE = KeyDataCreator.criarParesHE(DB_PATH);
                        try {
                            he = new HashExtensivel(HASH_CESTOS, HE_PATH + "/jobs_diretorio.db", HE_PATH + "/jobs_cestos.db", true);
                            for(RegistroHashExtensivel reg : registrosHE){
                                he.create(reg);
                            }
                        } catch (Exception e){
                            System.err.println("Erro ao criar HashExtensivel Linha 436: " + e);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Erro ao criar HashExtensivel: " + e);
                }
            default:
                break;
        }
    }
}