package com.aedsiii.puc.app;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;

import com.aedsiii.puc.model.CifraCesar;
import com.aedsiii.puc.model.Job;
import com.aedsiii.puc.model.RegistroBTree;
import com.aedsiii.puc.model.Rsa;

public class SecondaryToPrimary {
    
    // Variável auxiliar
    public static Job auxJob;
    public static Job oldJob;
    private static final String CONFIG_FILE = "config.properties";

    /**
     * Adiciona um novo Job a um arquivo de banco de dados, lidando com criptografia.
     *
     * @param job  O objeto Job a ser adicionado.
     * @param path O caminho para o arquivo de banco de dados.
     * @param ec   O método de encriptação ("caesar" ou null).
     * @return O novo ID do job adicionado, ou -1 em caso de erro.
     */
    public static int addJob(Job job, String path, String ec) {
        
        byte[] bytesDoArquivo;
        int last_id = -1;

        // --- PASSO 1: LER O ARQUIVO INTEIRO PARA A MEMÓRIA ---
        try {
            File dbFile = new File(path);
            if (dbFile.exists() && dbFile.length() > 0) {
                bytesDoArquivo = Files.readAllBytes(dbFile.toPath());
            } else {
                // Se o arquivo não existe, começamos com um array vazio.
                bytesDoArquivo = new byte[0];
            }
        } catch (IOException e) {
            System.err.println("Erro fatal ao ler o arquivo de banco de dados: " + e);
            return -1;
        }

        // --- PASSO 2: DECIFRAR OS BYTES (SE NECESSÁRIO) ---
        byte[] bytesDecifrados = bytesDoArquivo; // Por padrão, são os mesmos bytes lidos.
        int shift = 0; // Inicializa o shift

        if (ec != null && ec.equals("caesar")) {
            System.out.println("-> Método 'caesar' detectado. Decifrando...");
            Properties config = new Properties();
            File configFile = new File(CONFIG_FILE);
            try (FileInputStream fisConfig = new FileInputStream(configFile)) {
                config.load(fisConfig);
                String shift_str = config.getProperty("shift");
                shift = Integer.parseInt(shift_str);
                
                // Chama a função que opera em bytes e retorna um novo array de bytes
                bytesDecifrados = CifraCesar.decifrar(bytesDoArquivo, shift);
            } catch (Exception e) {
                System.err.println("Erro ao carregar configuração ou decifrar: " + e);
                return -1;
            }
        }

        try (
            // --- PASSO 3 E 4: PROCESSAR E MODIFICAR OS DADOS EM MEMÓRIA ---
            // Usa um ByteArrayInputStream para ler os dados decifrados
            ByteArrayInputStream bais = new ByteArrayInputStream(bytesDecifrados);
            DataInputStream dis = new DataInputStream(bais);
            
            // Usa um ByteArrayOutputStream para construir o novo arquivo em memória
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos)
        ) {
            // Lendo o cabeçalho (se o arquivo não estava vazio)
            if (bytesDecifrados.length > 0) {
                last_id = dis.readInt();
            } else {
                last_id = 0; // Se o arquivo é novo, o primeiro ID será 1.
            }

            // Calculando o novo ID
            int novo_id = last_id + 1;
            job.setJob_id((short) novo_id);

            // Escrevendo o novo cabeçalho no nosso buffer de memória
            dos.writeInt(novo_id);

            // Escrevendo o resto do conteúdo antigo (se houver) no buffer
            dos.write(dis.readAllBytes());

            // Escrevendo o novo Job no final do buffer
            job.toBytes(dos, 1, false, 0); // Supondo que toBytes escreva os dados do job

            // Pega todos os bytes do nosso buffer de memória
            byte[] dadosFinaisDecifrados = baos.toByteArray();

            // --- PASSO 5: CIFRAR O RESULTADO FINAL (SE NECESSÁRIO) ---
            byte[] dadosFinaisParaEscrever = dadosFinaisDecifrados;
            if (ec != null && ec.equals("caesar")) {
                System.out.println("-> Cifrando o novo conteúdo antes de salvar...");
                dadosFinaisParaEscrever = CifraCesar.cifrar(dadosFinaisDecifrados, shift);
            }

            // --- PASSO 6: ESCREVER TUDO DE VOLTA PARA O DISCO ---
            try (FileOutputStream fos = new FileOutputStream(path)) {
                fos.write(dadosFinaisParaEscrever);
                System.out.println("Job com ID " + novo_id + " adicionado com sucesso ao arquivo " + path);
            }
            
            return novo_id;

        } catch (IOException e) {
            System.err.println("Erro ao manipular os dados em memória: " + e);
            return -1;
        }
    }

    public static Job getJob(int id, String path, String ec) {
        Job job = new Job(); // Inicializa um Job "vazio" com ID padrão -1
        
        File dbFile = new File(path);
        // Se o arquivo não existe ou está vazio, não há o que procurar.
        if (!dbFile.exists() || dbFile.length() == 0) {
            return job;
        }

        byte[] bytesDoArquivo;
        // --- PASSO 1: LER O ARQUIVO INTEIRO PARA A MEMÓRIA ---
        try {
            bytesDoArquivo = Files.readAllBytes(dbFile.toPath());
        } catch (IOException e) {
            System.err.println("Erro fatal ao ler o arquivo de banco de dados: " + e);
            return job;
        }

        // --- PASSO 2: DECIFRAR OS BYTES (SE NECESSÁRIO) ---
        byte[] bytesDecifrados = bytesDoArquivo;
        if (ec != null && ec.equals("caesar")) {
            System.out.println("-> Método 'caesar' detectado. Decifrando para busca...");
            Properties config = new Properties();
            File configFile = new File(CONFIG_FILE);
            try (FileInputStream fisConfig = new FileInputStream(configFile)) {
                config.load(fisConfig);
                String shift_str = config.getProperty("shift");
                int shift = Integer.parseInt(shift_str);
                
                bytesDecifrados = CifraCesar.decifrar(bytesDoArquivo, shift);
            } catch (Exception e) {
                System.err.println("Erro ao carregar configuração ou decifrar: " + e);
                return job; // Retorna job vazio se a decifragem falhar
            }
        }

        // --- PASSO 3: PROCESSAR OS BYTES DECIFRADOS EM MEMÓRIA ---
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytesDecifrados);
             DataInputStream dis = new DataInputStream(bais)) {
            
            // Pula o cabeçalho com o último ID
            dis.readInt();

            // A sua lógica de busca sequencial, agora operando nos dados corretos!
            while (dis.available() > 0) {
                byte alive = dis.readByte();
                int recordSize = dis.readInt();
                short jobId = dis.readShort();
                
                // O tamanho do resto do registro é o tamanho total menos os 2 bytes do ID
                int bytesRestantes = recordSize - 3; 

                if (jobId == id && alive == 1) {
                    byte[] data = new byte[bytesRestantes];
                    dis.readFully(data);
                    job = deserializeJob(data); // Supondo que esta função exista
                    job.setJob_id(jobId);
                    // Encontramos o que queríamos, podemos parar o loop
                    break; 
                } else {
                    // Pula os bytes do registro atual para ir para o próximo
                    dis.skipBytes(bytesRestantes);
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao processar os dados do Job em memória: " + e);
        }

        return job;
    }

    /**
     * Realiza a deleção lógica de um Job em um arquivo de banco de dados,
     * lidando com criptografia.
     *
     * @param id   O ID do Job a ser removido.
     * @param path O caminho para o arquivo de banco de dados.
     * @param ec   O método de encriptação ("caesar" ou null/outro).
     * @return true se o registro foi encontrado e removido, false caso contrário.
     */
    public static boolean removeJob(int id, String path, String ec) {
        
        File dbFile = new File(path);
        if (!dbFile.exists() || dbFile.length() == 0) {
            System.out.println("Arquivo de banco de dados não existe ou está vazio. Nada para remover.");
            return false;
        }

        // --- PASSO 1: LER E DECIFRAR O ARQUIVO ---
        byte[] bytesDoArquivo;
        try {
            bytesDoArquivo = Files.readAllBytes(dbFile.toPath());
        } catch (IOException e) {
            System.err.println("Erro fatal ao ler o arquivo de banco de dados: " + e);
            return false;
        }

        byte[] bytesDecifrados = bytesDoArquivo;
        int shift = 0;
        if (ec != null && ec.equals("caesar")) {
            System.out.println("-> Método 'caesar' detectado. Decifrando para remoção...");
            Properties config = new Properties();
            File configFile = new File(CONFIG_FILE);
            try (FileInputStream fisConfig = new FileInputStream(configFile)) {
                config.load(fisConfig);
                String shift_str = config.getProperty("shift");
                shift = Integer.parseInt(shift_str);
                bytesDecifrados = CifraCesar.decifrar(bytesDoArquivo, shift);
            } catch (Exception e) {
                System.err.println("Erro ao carregar configuração ou decifrar: " + e);
                return false;
            }
        }

        // --- PASSO 2: PROCESSAR EM MEMÓRIA PARA MARCAR A LÁPIDE ---
        boolean found = false;
        try (
            ByteArrayInputStream bais = new ByteArrayInputStream(bytesDecifrados);
            DataInputStream dis = new DataInputStream(bais);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos)
        ) {
            // Copia o cabeçalho (último ID)
            dos.writeInt(dis.readInt());

            while (dis.available() > 0) {
                byte alive = dis.readByte();
                int recordSize = dis.readInt();
                short jobId = dis.readShort();
                byte[] data = new byte[recordSize - 3];
                dis.readFully(data);

                // Se encontramos o registro a ser deletado
                if (jobId == id && alive == 1) {
                    dos.writeByte((byte) 0); // Lápide = morto
                    found = true;
                } else {
                    dos.writeByte(alive); // Mantém a lápide original
                }
                // Copia o resto do registro como estava
                dos.writeInt(recordSize);
                dos.writeShort(jobId);
                dos.write(data);
            }

            if (!found) {
                // Se não achou, não precisa salvar nada de novo.
                return false;
            }

            // --- PASSO 3: CIFRAR E SALVAR O ARQUIVO MODIFICADO ---
            byte[] dadosFinaisDecifrados = baos.toByteArray();
            byte[] dadosFinaisParaEscrever = dadosFinaisDecifrados;

            if (ec != null && ec.equals("caesar")) {
                System.out.println("-> Cifrando o conteúdo modificado antes de salvar...");
                dadosFinaisParaEscrever = CifraCesar.cifrar(dadosFinaisDecifrados, shift);
            }

            // Escreve tudo de uma vez, sobrescrevendo o arquivo original de forma segura
            // (A abordagem com arquivo temporário seria a mais segura, mas a sobrescrita direta
            // após o processamento em memória também é uma opção viável).
            Files.write(Path.of(path), dadosFinaisParaEscrever);

            return true; // Retorna true porque o registro foi encontrado e marcado

        } catch (IOException e) {
            System.err.println("Erro ao processar os dados para remoção em memória: " + e);
            return false;
        }
    }
    public static boolean removeJobRAF(int id, String path, long offset){
        boolean found = false;
        try {
            RandomAccessFile raf = new RandomAccessFile(path, "rw");
            raf.seek(offset);
            raf.writeByte(0);
            raf.close();
            found = true;
        } catch (IOException e){
            System.err.println("Erro em SecondaryToPrimary.java, removeJobRAF: " + e);
        }
        return found;
    }
    public static boolean updateJob(int id, String path, Scanner sc, String ec) {
        // --- PASSO 1: LER O ARQUIVO E DECIFRAR (SE NECESSÁRIO) ---
        File dbFile = new File(path);
        if (!dbFile.exists() || dbFile.length() == 0) {
            System.out.println("Arquivo de banco de dados não existe ou está vazio. Nada para atualizar.");
            return false;
        }
        byte[] bytesDoArquivo;
        try {
            bytesDoArquivo = Files.readAllBytes(dbFile.toPath());
        } catch (IOException e) {
            System.err.println("Erro fatal ao ler o arquivo de banco de dados: " + e);
            return false;
        }

        byte[] bytesDecifrados = bytesDoArquivo;
        int shift = 0;
        if (ec != null && ec.equals("caesar")) {
            Properties config = new Properties();
            File configFile = new File(CONFIG_FILE);
            try (FileInputStream fisConfig = new FileInputStream(configFile)) {
                config.load(fisConfig);
                String shift_str = config.getProperty("shift");
                shift = Integer.parseInt(shift_str);
                } catch (Exception e) {
                System.err.println("Erro ao carregar configuração ou decifrar: " + e);
            }
            // ... (lógica para ler o shift do config.properties) ...
            bytesDecifrados = CifraCesar.decifrar(bytesDoArquivo, shift);
        }
        // --- PASSO 2: PROCESSAR E MODIFICAR OS DADOS EM MEMÓRIA ---
        try (
            ByteArrayInputStream bais = new ByteArrayInputStream(bytesDecifrados);
            DataInputStream dis = new DataInputStream(bais);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos)
        ) {
            boolean found = false;
            boolean biggerRecord = false;
            Job updatedJob = null;

            int lastId = dis.readInt();
            dos.writeInt(lastId);

            while (dis.available() > 0) {
                byte alive = dis.readByte();
                int originalRecordSize = dis.readInt();
                short jobId = dis.readShort();
                byte[] data = new byte[originalRecordSize - 3];
                dis.readFully(data);

                if (jobId == id && alive == 1) {
                    found = true;
                    Job jobOriginal = deserializeJob(data);
                    jobOriginal.setJob_id(jobId);
                    
                    oldJob = jobOriginal; // Salva o estado antigo do Job

                    System.out.println("Registro encontrado. Forneça os novos dados.");
                    updatedJob = JobDataCollector.updateJobData(sc, jobOriginal);
                    int newRecordSize = updatedJob.getByteSize();

                    if (newRecordSize <= originalRecordSize) {
                        // O novo registro cabe no lugar do antigo
                        updatedJob.toBytes(dos, alive, true, originalRecordSize);
                        // Preenche o espaço restante com bytes nulos para não deixar lixo
                        for (int i = 0; i < (originalRecordSize - newRecordSize); i++) {
                            dos.writeByte(0);
                        }
                    } else {
                        // O novo registro é maior, "mata" o antigo e adiciona o novo no final
                        dos.writeByte((byte) 0); // Lápide = morto
                        dos.writeInt(originalRecordSize);
                        dos.writeShort(jobId);
                        dos.write(data);
                        biggerRecord = true;
                    }
                } else {
                    // Se não é o registro que queremos, apenas o copiamos como está
                    dos.writeByte(alive);
                    dos.writeInt(originalRecordSize);
                    dos.writeShort(jobId);
                    dos.write(data);
                }
            }

            if (!found) {
                System.out.println("Registro com ID " + id + " não encontrado.");
                return false;
            }

            // Se o registro atualizado era maior, escrevemos ele no final
            if (biggerRecord) {
                updatedJob.toBytes(dos, (byte) 1, false, 0);
            }
            
            auxJob = updatedJob; // Atualiza a referência para o job modificado

            // --- PASSO 3: CIFRAR E SALVAR DE VOLTA NO DISCO ---
            byte[] dadosFinaisDecifrados = baos.toByteArray();
            byte[] dadosFinaisParaEscrever = dadosFinaisDecifrados;

            if (ec != null && ec.equals("caesar")) {
                dadosFinaisParaEscrever = CifraCesar.cifrar(dadosFinaisDecifrados, shift);
            }

            // Escreve tudo de uma vez, sobrescrevendo o arquivo original
            Files.write(Path.of(path), dadosFinaisParaEscrever);

            return true;

        } catch (Exception e) {
            System.err.println("Erro ao atualizar o Job em memória: " + e);
            e.printStackTrace();
            return false;
        }
    }
    public static boolean updateJobRAF_BT(int id, String path, Scanner sc, RegistroBTree registroBTree) {
        long jobOffset = registroBTree.offset;
        Job job = new Job();
        Job updatedJob = new Job();
        boolean status = true;
        try {
            RandomAccessFile raf = new RandomAccessFile(path, "rw");
            raf.seek(jobOffset);

            byte lapide = raf.readByte();
            if (lapide != 0) {
                int originalRecordSize = raf.readInt();
                short jobId = raf.readShort();
                System.out.println(jobId);
                byte[] data = new byte[originalRecordSize - 3];
                raf.readFully(data);
                job = deserializeJob(data);
                job.setJob_id(jobId);

                oldJob = job;

                System.out.println("Job encontrado: " + job.getJob_id());
                updatedJob = JobDataCollector.updateJobData(sc, job);
                updatedJob.setJob_id(jobId);
                int newRecordSize = updatedJob.getByteSize();

                if (newRecordSize <= originalRecordSize) {
                    raf.seek(jobOffset);
                    System.out.println("Sobrescrevendo, o registro novo é menor que o antigo");
                    updatedJob.toBytesRAF(raf, lapide, true, originalRecordSize);
                    int bytesEmBranco = originalRecordSize - newRecordSize;
                    for (int i = 0; i < bytesEmBranco; i++) {
                        raf.writeByte(0);
                    }
                } else { // Registro atualizado ocupa mais espaço que antes, então matar ele e colocar no final
                    raf.seek(jobOffset);
                    System.out.println("Matando o registro atual e criando novo no final: " + raf.length());
                    raf.writeByte(0);
                    long newOffset = raf.length();
                    raf.seek(newOffset);
                    updatedJob.toBytesRAF(raf, lapide, status, newRecordSize);
                    registroBTree.offset = newOffset;
                }
            } else {
                status = false;
            }
            raf.close();
            
            auxJob = updatedJob;
        } catch (IOException e) {
            System.err.println("Erro em updateJobRAF_BT: " + e);
        }
        return status;
    }

    public static boolean updateJobRAF(int id, String path, Scanner sc, HashExtensivel he){
        Job job = new Job();
        Job updatedJob = new Job();
        short foundId = 0;
        boolean status = true;
        long pos = -1;
        long foundPos = -1;
        long newPos = -1;
        try {
            RandomAccessFile raf = new RandomAccessFile(path, "rw");
            raf.readInt();
            while(raf.getFilePointer() < raf.length()){
                pos = raf.getFilePointer();
                System.out.println("Procurando, pos atual: " + pos);
                byte alive = raf.readByte(); //Ve se ta vivo
                int originalRecordSize = raf.readInt(); // Lê o tamanho do registro
                short jobId = raf.readShort(); // Lê o ID do registro
                byte[] data = new byte[originalRecordSize - 3]; // Já lemos o ID, então o resto é o conteúdo
                raf.readFully(data);
                job = deserializeJob(data); // deserialize = transformar array de bytes em objeto
                job.setJob_id(jobId);
                System.out.println("Job encontrado: " + job.getJob_id());
                if(jobId == id && alive == 1){
                    System.out.println("Encontrado na posição: " + pos);
                    foundPos = pos;
                    foundId = jobId;
                    job.setJob_id(jobId);

                    oldJob = job;

                    updatedJob = JobDataCollector.updateJobData(sc, job);
                    int newRecordSize = updatedJob.getByteSize(); // tamanho do registro atualizado
                    if(newRecordSize <= originalRecordSize){
                        raf.seek(pos);
                        System.out.println("Sobrescrevendo, o registro novo é menor que o antigo");
                        updatedJob.toBytesRAF(raf, alive, true, originalRecordSize); // talvez seria melhor se a gnt mudasse o toBytes pra n escrever a lapide e o tamanho do registro? aí faria por fora
                        int bytesEmBranco = originalRecordSize - newRecordSize;
                        for (int i = 0; i < bytesEmBranco; i++) {
                            raf.writeByte(0);
                        }
                        status = true;
                        newPos = foundPos;
                    } else { // registro atualizado ocupa mais espaço, ent matar o registro aq e colocar o atualizado no final
                        raf.seek(pos);
                        System.out.println("Matando o registro atual e criando novo no final: " + raf.length());
                        raf.writeByte(0); // 0 = morto
                        raf.writeInt(originalRecordSize);
                        raf.writeShort(jobId);
                        raf.write(data);
                        newPos = raf.length();
                        raf.seek(newPos);
                        updatedJob.setJob_id((short)foundId);
                        updatedJob.toBytesRAF(raf, alive, status, newRecordSize);
                        //System.out.println("BiggerRecord: " + biggerRecord);
                    }
                } else {
                    raf.seek(pos);
                    raf.writeByte(alive);
                    raf.writeInt(originalRecordSize);
                    raf.writeShort(jobId);
                    raf.write(data);
                }
            }
            raf.close();

            auxJob = updatedJob;

            System.out.println("newPos: " + newPos + " foundPos: " + foundPos);
            if(newPos != foundPos){
                boolean att = he.updateEndereco(updatedJob.getJob_id(), newPos);
                if(att){
                    System.out.println("Atualizado no Hash com sucesso!");
                } else {
                    System.out.println("Nao encontrado no hash");
                }
            }
        } catch (Exception e){
            System.err.println("Erro updateJobRAF: " + e);
        }
        return status;
    }
    /*
     * Função que transforma todos os registros em uma lista de objetos
     */
    public static ArrayList<Job> toPrimary(String path){
        ArrayList<Job> jobs = new ArrayList<Job>();
        try {
            FileInputStream arq = new FileInputStream(path);
            DataInputStream dis = new DataInputStream(arq);
            dis.readInt(); // pulando o primeiro byte que guarda o ultimo ID cadastrado
            while(dis.available() > 0) {
                Job job = new Job();
                byte alive = dis.readByte(); // lapide
                int recordSize = dis.readInt(); // tamanho do registro
                short jobId = dis.readShort(); // ID do registro
                byte[] data = new byte[recordSize - 3]; // Já lemos o ID, então o resto é o conteúdo
                dis.readFully(data);
                job = deserializeJob(data); // deserialize = transformar array de bytes em objeto
                job.setJob_id(jobId);
                if (alive == 1) { // adicionar na lista só se o registro tiver vivo
                    jobs.add(job);
                }
            }
            arq.close();
            dis.close();
        } catch (Exception e){
            System.err.println("Erro em SecondaryToPrimary.java: " + e);
        }
        return jobs;
    }

    public static ArrayList<Job> toPrimaryCodificado(String path, String method) {
        ArrayList<Job> jobs = new ArrayList<>();
        byte[] arquivoBytes; // Array que vai guardar os bytes, criptografados ou não
        // --- ETAPA 1: LER O ARQUIVO DO DISCO APENAS UMA VEZ ---
        try (FileInputStream fis = new FileInputStream(path)) {
            arquivoBytes = fis.readAllBytes();
        } catch (IOException e) {
            System.err.println("Erro ao ler o arquivo inicial: " + e);
            // Se não conseguimos ler o arquivo, não há mais nada a fazer.
            // Retorna a lista vazia.
            return jobs; 
        }
        // --- ETAPA 2: DECIFRAR OS BYTES ---
        if (method != null && method.equals("caesar")) {
            System.out.println("Método 'caesar' detectado. Decifrando o arquivo...");
            Properties config = new Properties();
            File configFile = new File(CONFIG_FILE);
            try (FileInputStream fisConfig = new FileInputStream(configFile)) {
                config.load(fisConfig);
                String shift_str = config.getProperty("shift");
                int shift = Integer.parseInt(shift_str);

                // A MÁGICA ACONTECE AQUI:
                // O array original 'arquivoBytes' é substituído pelo resultado da decifragem.
                System.out.println("shift: " + shift);
                arquivoBytes = CifraCesar.decifrar(arquivoBytes, shift);
                System.out.println("Arquivo decifrado com sucesso.");

            } catch (Exception e) {
                System.err.println("Erro ao carregar configuração ou decifrar: " + e);
                // Se a decifragem falhar, talvez não seja possível continuar.
                return jobs;
            }
        } else if (method.equals("rsa")) {
            System.out.println("-> Método 'rsa' detectado. Decifrando blocos...");
            Properties config = new Properties();
            File configFile = new File(CONFIG_FILE);
            try (FileInputStream fisConfig = new FileInputStream(configFile)) {
                config.load(fisConfig);
                BigInteger p = new BigInteger(config.getProperty("fp"));
                BigInteger q = new BigInteger(config.getProperty("sp"));
                arquivoBytes = Rsa.decifrarArquivoRsaManualEmBlocos(arquivoBytes, p, q);
                System.out.println("Arquivo decifrado com sucesso.");
            } catch (Exception e) {
                System.err.println("Erro ao carregar configuração ou decifrar: " + e);
                // Se a decifragem falhar, talvez não seja possível continuar.
                return jobs;
            }
        }
        
        // --- ETAPA 3: PROCESSAR OS BYTES (JÁ DECIFRADOS) EM MEMÓRIA ---
        // Usamos ByteArrayInputStream para ler do nosso array de bytes como se fosse um arquivo.
        try (ByteArrayInputStream bais = new ByteArrayInputStream(arquivoBytes);
            DataInputStream dis = new DataInputStream(bais)) {
            dis.readInt();
            while (dis.available() > 0) {
                Job job = new Job();
                byte alive = dis.readByte();
                int recordSize = dis.readInt();
                short jobId = dis.readShort();
                // O tamanho do 'data' é o tamanho total do registro menos o que já lemos (short = 2 bytes)
                byte[] data = new byte[recordSize - 3]; 
                dis.readFully(data);
                job = deserializeJob(data); 
                job.setJob_id(jobId);
                if (alive == 1) {
                    jobs.add(job);
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao processar os bytes do arquivo em memória: " + e);
        }
        return jobs;
    }

    /*
     * Função auxiliar para editar campos com lista
     */
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

    /*
     * Função auxiliar para ler campos com lista
     */
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

    /*
     * Função para transformar registro em objeto
     */
    public static Job deserializeJob(byte[] data){
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

            // quando atualiza um job, se o registro ficar menor, vai ter bytes em branco no final
            while (dis.available() > 0) {
                dis.readByte();
            }
        } catch (Exception e){
            System.err.println("Erro no SecondaryToPrimary.java, DeserializeJob: " + e);
        }
        return job;
    }
}
