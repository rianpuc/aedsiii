package com.aedsiii.puc.app;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.aedsiii.puc.model.BoyerMoore;
import com.aedsiii.puc.model.InvertedIndex;
import com.aedsiii.puc.model.Job;
import com.aedsiii.puc.model.KMP;
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
        }
        System.out.printf("\t1.  Inserir\n" +
                          "\t2.  Get\n" +
                          "\t3.  Editar\n" +
                          "\t4.  Remover\n" +
                          "\t5.  Mostrar todos\n" +
                          "\t6.  Pesquisar nas listas invertidas\n" +
                          "\t7.  Reordenar banco de dados\n" +
                          "\t8.  Trocar método de armazenamento\n" +
                          "\t9.  Compressão\n" +
                          "\t10. Descompressão\n" +
                          "----------------TESTES----------------\n" +
                          "\t21. Mostrar lista invertida (DEBUG)\n" +
                          "\t30. Pesquisar padrão usando KMP\n" +
                          "\t31. Pesquisar padrão usando Boyer Moore\n"+
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
            //System.out.println("Listas invertidas carregadas.");
        } else {
            //System.out.println("Criando listas invertidas...");
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
                    Job aux_ii_updatedOldJob = SecondaryToPrimary.oldJob;

                    if (aux_ii_updatedJob != null && aux_ii_updatedJob.getJob_id() != -1) {
                        termsToUpdate[0] = aux_ii_updatedJob.getJob_title();
                        termsToUpdate[1] = aux_ii_updatedJob.getRole();

                        // Termos antes da edição
                        String[] oldTerms = new String[2];
                        oldTerms[0] = aux_ii_updatedOldJob.getJob_title();
                        oldTerms[1] = aux_ii_updatedOldJob.getRole();

                        // Termos após a edição
                        Set<String> updatedTermsSet = new HashSet<>();
                        for (String term : termsToUpdate) {
                            if (term != null) {
                                updatedTermsSet.addAll(Arrays.asList(term.toLowerCase().split("\\s+")));
                            }
                        }

                        // deletando da lista invertida se um termo antes da edição não estiver no Job atualizado
                        for (String oldTerm : oldTerms) {
                            if (oldTerm != null) {
                                String[] oldWords = oldTerm.toLowerCase().split("\\s+");
                                for (String word : oldWords) {
                                    if (!updatedTermsSet.contains(word)) {
                                        // Remove the term from the inverted index
                                        invertedIndex_JT.delete(word, aux_ii_updatedJob.getJob_id());
                                        invertedIndex_JR.delete(word, aux_ii_updatedJob.getJob_id());
                                    }
                                }
                            }
                        }

                        // adicionando termos na lista invertida
                        for (String string : termsToUpdate) {
                            invertedIndex_JT.add(string, aux_ii_updatedJob.getJob_id());
                            invertedIndex_JR.add(string, aux_ii_updatedJob.getJob_id());
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
                    if (aux_ii_deletedJob != null && aux_ii_deletedJob.getJob_id() != -1) {
                        termsRemoval[0] = aux_ii_deletedJob.getJob_title();
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
                            jobs = null;
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
                                    if (rbt_searchedJob != null) {
                                        Job searchedJob = OffsetReader.readInOffset(rbt_searchedJob.offset, DB_PATH);
                                        System.out.println(searchedJob);
                                    }
                                }
                                break;
                            case "hash":
                                for (int II_sequential_id : searchResults) {
                                    RegistroHashExtensivel rhe_searchedJob = he.read(II_sequential_id);
                                    if (rhe_searchedJob != null) {
                                        Job searchedJob = OffsetReader.readInOffset(rhe_searchedJob.offset, DB_PATH);
                                        System.out.println(searchedJob);
                                    }
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
                case 9:
                    Compression.compress(DB_PATH);
                    break;
                case 10:
                    File snapshotDir = new File("snapshots/");
                    File[] arquivos = snapshotDir.listFiles();
                    if (arquivos == null || arquivos.length == 0) {
                        System.out.println("Nenhuma versão de snapshot foi encontrada.");
                        break;
                    }
                    Pattern pattern = Pattern.compile("binary_dbHuffmanCompressao(\\d+)\\.db");
                    List<Integer> versoesDisponiveis = new ArrayList<>();
                    for (File f : arquivos) {
                        Matcher matcher = pattern.matcher(f.getName());
                        if (matcher.matches()) {
                            int versao = Integer.parseInt(matcher.group(1));
                            versoesDisponiveis.add(versao);
                        }
                    }
                    if (versoesDisponiveis.isEmpty()) {
                        System.out.println("Nenhuma versão de snapshot encontrada.");
                        break;
                    }
                    // Ordenar e exibir opções ao usuário
                    Collections.sort(versoesDisponiveis);
                    System.out.println("Versões disponíveis para descompressão:");
                    for (int v : versoesDisponiveis) {
                        System.out.println( v +")" + " Versão " + v);
                    }
                    // Solicitar input do usuário
                    System.out.print("Digite a versão que deseja descomprimir: ");
                    int escolhida = sc.nextInt();

                    if (!versoesDisponiveis.contains(escolhida)) {
                        System.out.println("Versão inválida. Operação cancelada.");
                        break;
                    }
                    Compression.decompress(escolhida);
                    sc.nextLine();
                    break;
                case 30:
                    System.out.println("Digite o padrão a ser pesquisado em job_description (case-sensitive):");
                    String padraoKMP;
                    try {
                        padraoKMP = sc.nextLine();
                        if (padraoKMP == null || padraoKMP.trim().isEmpty()) {
                            System.out.println("Padrão inválido. Tente novamente.");
                            break;
                        }
                        ArrayList<Job> jobs = SecondaryToPrimary.toPrimary(DB_PATH);
                        ArrayList<Job> found_KMP_jobs = new ArrayList<>();
                        int foundCountKMP = 0;
                        int previousCountKMP = foundCountKMP;
                        for (Job job_5 : jobs) {
                            try {
                                foundCountKMP += KMP.search(padraoKMP, job_5.getJob_description());
                                if (foundCountKMP > previousCountKMP) {
                                    found_KMP_jobs.add(job_5);
                                }
                                previousCountKMP = foundCountKMP;
                            } catch (Exception e) {
                                System.out.println("Erro ao buscar no job ID " + job_5.getJob_id() + ": " + e.getMessage());
                            }
                        }
                        jobs = null;
                        if (!found_KMP_jobs.isEmpty()) {
                            System.out.println("Padrão \"" + padraoKMP + "\" encontrado " + foundCountKMP + " vezes em " + found_KMP_jobs.size() + " registros.");
                            String resposta;
                            while (true) {
                                System.out.println("Deseja ver os registros de ocorrência? (S/N)");
                                resposta = sc.nextLine();
                                if (resposta.toLowerCase().trim().equals("s")) {
                                    for (Job job_5 : found_KMP_jobs) {
                                        System.out.println(job_5);
                                    }
                                    System.out.println("Padrão \"" + padraoKMP + "\" encontrado " + foundCountKMP + " vezes em " + found_KMP_jobs.size() + " registros.");
                                    break;
                                } else if (resposta.toLowerCase().trim().equals("n")) {
                                    break;
                                } else {
                                    System.out.println("Resposta inválida.");
                                }
                            }
                            found_KMP_jobs = null;
                        } else {
                            System.out.println("Padrão não encontrado em nenhum registro.");
                        }
                    } catch (Exception e) {
                        System.out.println("Erro ao ler o padrão ou executar a busca: " + e.getMessage());
                    }
                    break;
                case 31:
                    System.out.println("Digite o padrão a ser pesquisado em job_description (case-sensitive):");
                    String padraoBM;
                    try {
                        padraoBM = sc.nextLine();
                        if (padraoBM == null || padraoBM.trim().isEmpty()) {
                            System.out.println("Padrão inválido. Tente novamente.");
                            break;
                        }
                        ArrayList<Job> jobs = SecondaryToPrimary.toPrimary(DB_PATH);
                        ArrayList<Job> found_BM_jobs = new ArrayList<>();
                        int foundCountBM = 0;
                        int previousCountBM = foundCountBM;
                        for (Job job_5 : jobs) {
                            try {
                                foundCountBM += BoyerMoore.search(padraoBM, job_5.getJob_description());
                                if (foundCountBM > previousCountBM) {
                                    found_BM_jobs.add(job_5);
                                }
                                previousCountBM = foundCountBM;
                            } catch (Exception e) {
                                System.out.println("Erro ao buscar no job ID " + job_5.getJob_id() + ": " + e.getMessage());
                            }
                        }
                        jobs = null;
                        if (!found_BM_jobs.isEmpty()) {
                            System.out.println("Padrão \"" + padraoBM + "\" encontrado " + foundCountBM + " vezes em " + found_BM_jobs.size() + " registros.");
                            String resposta;
                            while (true) {
                                System.out.println("Deseja ver os registros de ocorrência? (S/N)");
                                resposta = sc.nextLine();
                                if (resposta.toLowerCase().trim().equals("s")) {
                                    for (Job job_5 : found_BM_jobs) {
                                        System.out.println(job_5);
                                    }
                                    System.out.println("Padrão \"" + padraoBM + "\" encontrado " + foundCountBM + " vezes em " + found_BM_jobs.size() + " registros.");
                                    break;
                                } else if (resposta.toLowerCase().trim().equals("n")) {
                                    break;
                                } else {
                                    System.out.println("Resposta inválida.");
                                }
                            }
                            found_BM_jobs = null;
                        } else {
                            System.out.println("Padrão não encontrado em nenhum registro.");
                        }
                    } catch (Exception e) {
                        System.out.println("Erro ao ler o padrão ou executar a busca: " + e.getMessage());
                    }
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