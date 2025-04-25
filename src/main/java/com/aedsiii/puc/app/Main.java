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
        System.out.printf("\t1. Inserir\t(Também insere na Árvore B)\n" +
                          "\t2. Get\n" +
                          "\t3. Editar\n" +
                          "\t4. Remover\n" +
                          "\t5. Mostrar todos\n" +
                          "\t6. Reordenar banco de dados\n");

        System.out.println(BTREE_ORDER != -1 ? "\t7. Criar Árvore B\t(Árvore existente detectada. Ordem atual: " + BTREE_ORDER + ")" : "\t7. Criar Árvore B\t(Nenhuma árvore atualmente criada)");

        System.out.println("\t8. Printar Árvore B\n" +
                           "\t9. Get usando Árvore B\n" +
                           "\t10. Teste remoção Árvore B\n" +
                           "\t0. Sair\n" +
                           "\tOpcao: ");
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
                    if(!d.exists()) d.mkdir();
                    he = new HashExtensivel(HASH_CESTOS, HE_PATH + "/jobs_diretorio.db", HE_PATH + "/jobs_cestos.db");
                } catch (Exception e) {
                    System.err.println("Erro ao criar HashExtensivel: " + e);
                }
            default:
                break;
        }
        while(answer != 0){
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

                    if (BTREE_ORDER != -1) {
                        RegistroBTree addJobRBT = new RegistroBTree(newJob.getJob_id(), addJobOffset);
                        btree.create(addJobRBT);
                    } else {
                        System.out.println("Nenhuma árvore detectada para inclusão de novo registro na árvore.");
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
                    job = SecondaryToPrimary.getJob(id, DB_PATH);
                    if (job.getJob_id() != -1) {
                        System.out.println(job);
                    }
                    else {
                        System.out.println("Registro nao encontrado.");
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
                    boolean status = SecondaryToPrimary.updateJob(id, DB_PATH, sc);
                    if (status) {
                        System.out.println("Vaga editada com sucesso! ID: " + id);
                    } else {
                        System.out.println("Vaga não encontrada. ID: " + id);
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
                    ArrayList<Job> jobs = SecondaryToPrimary.toPrimary(DB_PATH);
                    for (Job job_5 : jobs) {
                        System.out.println(job_5);
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
                case 7:
                    registrosBT = KeyDataCreator.criarPares(DB_PATH);
                    // for (RegistroBTree reg : registrosBT) {
                    //     System.out.println(reg);
                    // }
                    if (registrosBT.isEmpty()) {
                        System.out.println("Dataset vazio.");
                        continue;
                    }
                    System.out.println("Insira a ordem da árvore: ");
                    int ordem;
                    try {
                        ordem = Integer.parseInt(sc.nextLine());
                    } catch (NumberFormatException e) {
                        System.out.println("Entrada inválida. Por favor, insira um número.");
                        continue;
                    }
                    btree = new BTree(ordem, BTREE_PATH, true);
                    btree.pagina = new PaginaBTree(ordem);
                    for (RegistroBTree reg : registrosBT) {
                        btree.create(reg);
                    }
                    System.out.println("Árvore de ordem [" + ordem + "] criada.");
                    BTREE_ORDER = ordem;
                    //System.out.println("BTREE_ORDER: " +  BTREE_ORDER);
                    break;
                case 8:
                    if (btree != null) {
                        btree.print();
                    } else {
                        System.out.println("Árvore B não criada.");
                    }
                    break;
                case 9:
                    if (btree == null) {
                        System.out.println("Árvore B não criada.");
                        continue;
                    }
                    System.out.println("Insira o ID: ");
                    try {
                        id = Integer.parseInt(sc.nextLine());
                    } catch (NumberFormatException e) {
                        System.out.println("Entrada inválida. Por favor, insira um número.");
                        continue;
                    }
                    registroBTree = btree.search(id);
                    if (registroBTree.id != -1) {
                        job = OffsetReader.readInOffset(registroBTree.offset, DB_PATH);
                        System.out.println(job);
                    } else {
                        System.out.println("Registro não encontrado.");
                    }
                    break;
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
                case 11:
                    ArrayList<RegistroHashExtensivel> registrosHE = KeyDataCreator.criarParesHE(DB_PATH);
                    System.out.println("Insira a quantidade de registros no cesto: ");
                    int qtd;
                    try {
                        qtd = Integer.parseInt(sc.nextLine());
                    } catch (NumberFormatException e) {
                        System.out.println("Entrada inválida. Por favor, insira um número.");
                        continue;
                    }
                    try {
                        he = new HashExtensivel(qtd, HE_PATH + "/jobs_diretorio.db", HE_PATH + "/jobs_cestos.db");
                        for(RegistroHashExtensivel reg : registrosHE){
                            System.out.println("Registros: " + reg);
                            he.create(reg);
                        }
                    } catch (Exception e){
                        System.err.println("Erro ao criar HashExtensivel Linha 285: " + e);
                    }
                case 12:
                    he.print();
                    break;
                case 13:
                    if (he == null) {
                        System.out.println("Hash Extensivel não criado.");
                        continue;
                    }
                    System.out.println("Insira o ID: ");
                    try {
                        id = Integer.parseInt(sc.nextLine());
                    } catch (NumberFormatException e) {
                        System.out.println("Entrada inválida. Por favor, insira um número.");
                        continue;
                    }
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
                    System.out.println("Usando método sequencial.");
                    break;
                case "btree":
                    BTREE_ORDER = Integer.parseInt(config.getProperty("btree.ordem"));
                    System.out.println("Usando B-Tree com ordem " + BTREE_ORDER);
                    break;
                case "hash":
                    HASH_CESTOS = Integer.parseInt(config.getProperty("hash.cestos"));
                    System.out.println("Usando Hash Extensível com " + HASH_CESTOS + " cestos");
                    break;
                case "lista":
                    System.out.println("Usando lista invertida (ainda não implementada)");
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
}