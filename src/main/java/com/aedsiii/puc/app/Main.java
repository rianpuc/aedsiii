package com.aedsiii.puc.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;

import com.aedsiii.puc.model.InvertedIndex;
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
    private static boolean FILE_CREATION_NEEDED;
    private static final String INVERTED_INDEX_JOBTITLE_PATH = "invertedindex_jobtitle.dat";
    private static final String INVERTED_INDEX_JOBROLE_PATH = "invertedindex_jobrole.dat";

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
        System.out.printf("\t1.  Inserir\n" +
                          "\t2.  Get\n" +
                          "\t3.  Editar\n" +
                          "\t4.  Remover\n" +
                          "\t5.  Mostrar todos\n" +
                          "\t6.  Pesquisar nas listas invertidas\n" +
                          "\t7.  Reordenar banco de dados\n" +
                          "\t8. Trocar método de armazenamento\n" +
                          "\t21. Mostar lista invertida (DEBUG)\n" +
                          "\t0.  Sair\n");
    }
    public static void main(String[] args) throws Exception{
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
        
        InvertedIndex invertedIndex_JT = new InvertedIndex();
        InvertedIndex invertedIndex_JR = new InvertedIndex();
        File invertedIndex_JT_File = new File(INVERTED_INDEX_JOBTITLE_PATH);
        File invertedIndex_JR_File = new File(INVERTED_INDEX_JOBROLE_PATH);

        // Listas invertidas com Job Titles e Roles
        if (invertedIndex_JT_File.exists() && invertedIndex_JR_File.exists()) {
            invertedIndex_JT.loadFromFile(INVERTED_INDEX_JOBTITLE_PATH);
            invertedIndex_JR.loadFromFile(INVERTED_INDEX_JOBROLE_PATH);
            System.out.println("Listas invertidas carregadas.");
        } else {
            System.out.println("Criando listas invertidas...");
            ArrayList<Job> jobs = SecondaryToPrimary.toPrimary(DB_PATH);
            for (Job job_ii: jobs) {
                invertedIndex_JT.add(job_ii.getJob_title(), job_ii.getJob_id());
                invertedIndex_JR.add(job_ii.getRole(), job_ii.getJob_id());
            }
            System.out.println("Listas invertidas criadas.");
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                invertedIndex_JT.saveToFile(INVERTED_INDEX_JOBTITLE_PATH);
                invertedIndex_JR.saveToFile(INVERTED_INDEX_JOBROLE_PATH);
            } catch (IOException e) {
                System.err.println("Erro ao salvar listas invertidas.");
                e.printStackTrace();
            }
        }));

        while(answer != 0){
            registroHE = null;
            if (FILE_CREATION_NEEDED) {
                switch (METHOD) {
                    case "btree":
                        btree = new BTree(BTREE_ORDER, BTREE_PATH, true);
                        btree.pagina = new PaginaBTree(BTREE_ORDER);
                        registrosBT = KeyDataCreator.criarPares(DB_PATH);
                        if (registrosBT.isEmpty()) {
                            System.out.println("Dataset vazio.");
                            break;
                        }
                        for (RegistroBTree reg : registrosBT) {
                            btree.create(reg);
                        }
                        FILE_CREATION_NEEDED = false;
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
                        FILE_CREATION_NEEDED = false;
                        break;
                    default:
                        break;
                }
            }
            printMenu();
            boolean validInput = false;
            while (!validInput) {
                try {
                    answer = Integer.parseInt(sc.nextLine());
                    validInput = true;
                } catch (NumberFormatException e) {
                    System.out.println("Entrada inválida. Insira um número.");
                }
            }
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

                    // Adicionando novos termos nas listas invertidas
                    invertedIndex_JT.add(newJob.getJob_title(), newJob.getJob_id());
                    invertedIndex_JR.add(newJob.getRole(), newJob.getJob_id());

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
                            } catch (Exception e){
                                System.err.println("Erro 307: " + e);
                            }
                            if (registroHE != null && registroHE.id != -1) {
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
                            break;
                        case "btree":
                            registroBTree = btree.search(id);
                            if (registroBTree.id != -1) {
                                System.out.println("encontrado");
                                status = SecondaryToPrimary.updateJobRAF_BT(id, DB_PATH, sc, registroBTree);
                                if (status) {
                                    // System.out.println("Novo offset: " + registroBTree.offset);
                                    System.out.println("Vaga editada com sucesso! ID: " + id);
                                    btree.updateOffset(registroBTree.offset);
                                }
                            } else {
                                System.out.println("Vaga não encontrada. ID: " + id);
                            }
                            break;
                        default:
                            break;
                    }

                    String[] termsToUpdate = new String[2];
                    Job aux_ii_updatedJob = SecondaryToPrimary.auxJob;

                    if (aux_ii_updatedJob.getJob_id() != -1) {
                        termsToUpdate[0] = aux_ii_updatedJob.getRole();
                        termsToUpdate[1] = aux_ii_updatedJob.getRole();
                        for (String string : termsToUpdate) {
                            invertedIndex_JT.delete(string, aux_ii_updatedJob.getJob_id());
                            invertedIndex_JR.delete(string, aux_ii_updatedJob.getJob_id());
                        }
                        System.out.println("Lista invertida atualizada após atualização de um registro.");
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
                    String[] termsRemoval = new String[2];
                    Job aux_ii_deletedJob = new Job();
                    switch (METHOD) {
                        case "sequencial":
                            // Guardando para atualizar lista de termos
                            aux_ii_deletedJob = SecondaryToPrimary.getJob(id, DB_PATH);
                            boolean res = SecondaryToPrimary.removeJob(id, DB_PATH);
                            if(res){
                                System.out.println("Registro com ID " + id + " removido!");
                            } else {
                                System.out.println("Registro não encontrado.");
                            }
                            break;
                        case "hash":
                            try {
                                registroHE = he.read(id);
                                if(registroHE != null) {
                                    // Guardando para atualizar lista de termos
                                    aux_ii_deletedJob = OffsetReader.readInOffset(registroHE.offset, DB_PATH);

                                    res = SecondaryToPrimary.removeJobRAF(id, DB_PATH, registroHE.offset);
                                    boolean s = he.delete(id);
                                    if(s) System.out.println("Removido do Hash com sucesso!");
                                } else {
                                    System.out.println("Registro não encontrado.");
                                }
                            } catch (Exception e) {
                                System.err.println("Erro removerJobRAF: " + e);
                            }
                            break;
                        case "btree":
                            RegistroBTree rbt_aux = new RegistroBTree();
                            rbt_aux = btree.search(id);
                            // Guardando para atualizar lista de termos
                            aux_ii_deletedJob = OffsetReader.readInOffset(rbt_aux.offset, DB_PATH);

                            btree.delete(id);
                            res = SecondaryToPrimary.removeJobRAF(id, DB_PATH, btree.auxRemovalOffset);
                            if (res) {
                                System.out.println("Removido da Árvore B com sucesso!");
                            } else {
                                System.out.println("Registro não encontrado.");
                            }
                            if (btree.antecessoraPendente) {
                                // System.out.println("Indo deletar antecessora pendente: " + btree.auxKey.id);
                                btree.delete(btree.auxKey.id);
                                btree.antecessoraPendente = false;
                            }
                            btree.auxRemovalOffset = -1;
                            break;
                        default:
                            break;
                    }
                    if (aux_ii_deletedJob.getJob_id() != -1) {
                        termsRemoval[0] = aux_ii_deletedJob.getRole();
                        termsRemoval[1] = aux_ii_deletedJob.getRole();
                        for (String string : termsRemoval) {
                            invertedIndex_JT.delete(string, aux_ii_deletedJob.getJob_id());
                            invertedIndex_JR.delete(string, aux_ii_deletedJob.getJob_id());
                        }
                        System.out.println("Lista invertida atualizada após deleção de um registro.");
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
                case 6: // Pesquisa usando as listas invertidas
                    System.out.println("Digite um termo para buscar em nomes de trabalhos e cargos:");
                    String term = sc.nextLine();
                    // List<Integer> resultsJT = invertedIndex_JT.search(term);
                    List<Integer> resultsJR = invertedIndex_JR.search(term);
                    Set<Integer> union = new HashSet<>(invertedIndex_JT.search(term));

                    // Faz união dos resultados tirando ids duplicados
                    union.addAll(resultsJR);
                    List<Integer> searchResults = new ArrayList<>(union);

                    if (searchResults.isEmpty()) {
                        System.out.println("Nenhum registro encontrado com o termo: \"" + term + "\"");
                    } else {
                        System.out.println("Registros encontrados: " + searchResults);
                        switch (METHOD) {
                            case "sequencial":
                                for (int II_sequential_id : searchResults) {
                                    Job searchedJob = SecondaryToPrimary.getJob(II_sequential_id, DB_PATH);
                                    System.out.println(searchedJob);
                                }
                                break;
                            case "btree":
                                for (int II_sequential_id : searchResults) {
                                    RegistroBTree rbt_searchedJob = btree.search(II_sequential_id);
                                    Job searchedJob = OffsetReader.readInOffset(rbt_searchedJob.offset, DB_PATH);
                                    System.out.println(searchedJob);
                                }
                                break;
                            case "hash":
                                for (int II_sequential_id : searchResults) {
                                    RegistroHashExtensivel rhe_searchedJob = he.read(II_sequential_id);
                                    Job searchedJob = OffsetReader.readInOffset(rhe_searchedJob.offset, DB_PATH);
                                    System.out.println(searchedJob);
                                }
                                break;
                            default:
                                break;
                        }
                    }
                    
                    break;
                case 7: // ordenação externa
                    switch (METHOD) {
                        case "sequencial":
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
                            break;
                        case "hash":
                            System.out.println("Reordenação disponível apenas no armazenamento sequencial.");
                            System.out.println("Reordene o arquivo no método sequencial e volte aqui para um novo hash.");
                            break;
                        case "btree":
                            System.out.println("Reordenação disponível apenas no armazenamento sequencial.");
                            System.out.println("Reordene o arquivo no método sequencial e volte aqui para uma nova Árvore B.");
                            break;
                        default:
                            System.out.println("Opção inválida.");
                            break;
                    }
                    break;
                case 8:
                    changeIndexMethod(sc, false);
                    break;
                case 21:
                    invertedIndex_JT.printIndex();
                    invertedIndex_JR.printIndex();
                case 0:
                    break;
                default:
                    System.out.println("Opção inválida. Tente novamente.");
                    break;
            }
        }
        sc.close();
    }
    public static void changeIndexMethod(Scanner sc, boolean load) {
        Properties config = new Properties();
        File configFile = new File(CONFIG_FILE);
        FILE_CREATION_NEEDED = true;
        try {
            if (configFile.exists()) {
                try (FileInputStream fis = new FileInputStream(configFile)) {
                    config.load(fis);
                }
            }
            String dbPath = config.getProperty("binary.path");
            if (dbPath == null || dbPath.isEmpty()) {
                ArrayList<Job> jobs = FileParser.parseFile();
                PrimaryToSecondary.toSecondary(jobs, DB_PATH);
                config.setProperty("binary.path", DB_PATH);

                try (FileOutputStream fos = new FileOutputStream(configFile)) {
                    config.store(fos,  "Guardando o local do binario");
                }
            }
            String indexMethod = config.getProperty("index.method");
            if (!load || indexMethod == null) {
                int select = -1;
                boolean validInput = false;
                System.out.println("Selecione o metodo de armazenamento: \n" + 
                                    "1. Sequencial\n" +
                                    "2. B-Tree\n" +
                                    "3. Hash Extensivel\n");
                while (!validInput) {
                    try {
                        select = Integer.parseInt(sc.nextLine());
                        if (select > 0 && select <= 3) {
                            validInput = true;
                        } else {
                            System.out.println("Por favor selecione uma opção válida.");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Entrada inválida. Insira um número.");
                    }
                }
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
                    default:
                        System.out.println("Por favor selecione uma opção válida.");
                        break;
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
}