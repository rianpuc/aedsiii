package com.aedsiii.puc.app;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import com.aedsiii.puc.model.Job;

public class ExternalSort {

    /**
     * Função de ordenação externa principal
     * @param b limite de registros em memória principal
     * @param m número de caminhos a serem usados
     * @param db_path caminho do arquivo banco de dados
     * @param external_sort_path caminho para guardar arquivos temporários durante a ordenação
     */
    public static void sort(int b, int m, String db_path, String external_sort_path) {
        try {
            
            String temp_path = external_sort_path; // só pelo nome msm
            
            // Criar o diretório se não existir
            File dir = new File(temp_path);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // Excluir arquivos temporários de possíveis ordenações anteriores
            // Ainda sobrará arquivos além de m, porém eles não serão usados ent n tem problema por enquanto
            // for (int i = 0; i < m; i++) {
            //     File tempFile = new File(temp_path + "/temp_fos" + i + ".tmp.db");
            //     if (tempFile.exists()) {
            //         tempFile.delete();
            //     }
            // }

            FileInputStream arqOriginal = new FileInputStream(db_path);
            DataInputStream dis = new DataInputStream(arqOriginal);
            int last_id = dis.readInt(); // lendo cabeçalho

            ArrayList<DataOutputStream> caminhos_dos = new ArrayList<DataOutputStream>();

            for (int i = 0; i < m; i++) {
            	FileOutputStream fos = new FileOutputStream(temp_path + "/temp_fos" + i + ".tmp.db");
            	DataOutputStream dos = new DataOutputStream(fos);
            	caminhos_dos.add(dos);
            }

            // variável pra usar no loop circular de m caminhos
            int fileIndex = 0;
            while (dis.available() > 0) {
            	// Lista para guardar B registros
            	ArrayList<Job> jobs_block = new ArrayList<Job>();
            	// Coletar B registros
            	for (int i = 0; i < b && dis.available() > 0; i++) {
            		Job job = new Job();
                    byte alive = dis.readByte();
                    int recordSize = dis.readInt();
                    short jobId = dis.readShort();
                    byte[] data = new byte[recordSize - 3]; // Já lemos o ID, então o resto é o conteúdo
                    dis.readFully(data);
                    if (alive == 1) {
                        job = SecondaryToPrimary.deserializeJob(data);
                        job.setJob_id(jobId);
                        jobs_block.add(job);
                    }
            	}
            
            	// Ordenando o bloco obtido
            	Collections.sort(jobs_block, (job1, job2) -> {
            		return job1.getJob_id() - job2.getJob_id();
            	});
            
            	DataOutputStream dos = caminhos_dos.get(fileIndex);
            	for (Job job : jobs_block) {
            		job.toBytes(dos, 1, false, 0);
            	}
            
            	fileIndex = (fileIndex + 1) % m; // loop circular. Indo para o proximo arquivo dos m caminhos
            }

            caminhos_dos.clear();
            dis.close();
            
            //INTERCALACAO

            ArrayList<DataOutputStream> temps_dos = new ArrayList<DataOutputStream>();
            ArrayList<DataInputStream> temps_dis = new ArrayList<DataInputStream>();

            for (int i = 0; i < m; i++) {
            	FileOutputStream fos = new FileOutputStream(temp_path + "/temp_i_fos" + i + ".tmp.db");
            	temps_dos.add(new DataOutputStream(fos));

                // Os arquivos "/temp_fos" criados na distribuição agora serão abertos com FileInputStream para leitura e uso na intercalação
                FileInputStream fis = new FileInputStream(temp_path + "/temp_fos" + i + ".tmp.db");
                temps_dis.add(new DataInputStream(fis));
            }

            // Array para indicar se os arquivos atualmente abertos já foram completamente lidos
            boolean endOfFile[] = new boolean[m];

            // ARRAY PARA DEBUG
            // int filePointer[] = new int[m];

            // usado como limite da primeira intercalação
            // int max_sorted = b*m;
            // usado como limite da segunda intercalação até o final
            // max_sorted = max_sorted * m;

            // Indíce do arquivo M atual
            fileIndex = 0;
            // Estimativa tamanho do bloco ordenado
            int block_size = b*m;
            // Array para guardar os registros em memória primária
            Job[] registrosAtuais = new Job[m];

            while (true) {
                // Contar quantos arquivos ainda têm bytes disponíveis
                int ativos = 0;
                for (int i = 0; i < m; i++) {
                    if (temps_dis.get(i) != null){
                        try {
                            if (temps_dis.get(i).available() > 0) {
                                ativos++;
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                // Se só um arquivo ainda tem bytes, finalizamos
                if (ativos == 1) {
                    // System.out.println("Ordenação concluída! Apenas um arquivo restante.");
                    break;
                }
                
                // Enquanto ainda houver um arquivo que não foi totalmente lido
                while (!finalized(endOfFile)) {
                    ArrayList<Job> jobs = new ArrayList<Job>();

                    // Usando o limite da estimativa do tamanho do bloco ordenado
                    // Se determinado bloco não for do tamanho estimado, o resto das iterações não tentarão ler mais registros.
                    for(int i = 0; i < block_size; i++) {
                        Job menor = menorRegistro(temps_dis, /*filePointer,*/ registrosAtuais, endOfFile);
                        if (menor != null) {
                            jobs.add(menor);
                        }
                        
                        // filePointerPrint(filePointer);
                    }

                    // Ordenando os jobs lidos
                    Collections.sort(jobs, Comparator.comparingInt(Job::getJob_id));

                    // Escrevendo os jobs ordenados no arquivo de saída atual
                    for (Job job : jobs){
                        //System.out.printf("%s\n", job.toString());
                        //temps_dos.get(fileIndex).writeShort(job.getJob_id());
                        job.toBytes(temps_dos.get(fileIndex), 1, false, 0);
                    }
                    // Loop circular. Indo para o índice do próximo arquivo de saída
                    fileIndex = (fileIndex + 1) % m;
                    // System.out.printf("Terminou: %s\n", finalized(endOfFile) ? "SIM" : "NAO");
                }

                // Aumentando estimativa do tamanho do bloco ordenado
                block_size *= m;
                // Reposiciona ponteiros para novos arquivos de leitura e saída
                reloadPointers(temps_dos, temps_dis, temp_path, m);

                // Os arquivos serão reabertos/reutilizados, então nenhum estará em endOfFile
                for(int i = 0; i < endOfFile.length; i++){
                    endOfFile[i] = false;
                }
            }
            replaceDatabase(db_path, last_id, temps_dis, temps_dos, temp_path, m);
        } catch (IOException e) {
            System.err.println("Erro em ExternalSort.java, sort");
            e.printStackTrace();
        }
    }

    /**
     * Função para saber se todos os arquivos já foram totalmente lidos
     * @param files array com estados true ou false para os arquivos
     */
    public static boolean finalized(boolean[] files){
        boolean res = true;
        for(int i = 0; i < files.length; i++){
            if(files[i] == false){
                res = false;
                break;
            }
        }
        return res;
    }

    /**
     * Função para encontrar qual o menor registro dos ponteiros atuais
     * @param dis lista com arquivos temporários para leitura
     * @param indexes PARAMETRO DE DEBUG: array com indices atuais dos arquivos
     * @param registrosAtuais registros na memória primária
     * @param arquivoFinalizado array com true ou false, dependendo se o arquivo ja foi completamente lido ou não
     */
    public static Job menorRegistro(ArrayList<DataInputStream> dis, /*int[] indexes,*/ Job[] registrosAtuais, boolean[] arquivoFinalizado) throws IOException {
        // Varíavel para guardar o índice do menor job
        int menorIndex = -1;
        Job menorJob = null;
    
        // iterando em cada arquivo M
        for (int i = 0; i < dis.size(); i++) {
            if (arquivoFinalizado[i]) {
                continue; // Pula arquivos finalizados
            }
    
            // Se não tem um job salvo desse arquivo, lê um novo
            if (registrosAtuais[i] == null) {
                DataInputStream stream = dis.get(i);
    
                // Se não tiver nenhum byte sobrando pra ler, chegou ao final do arquivo
                if (stream.available() == 0) {
                    arquivoFinalizado[i] = true;
                    continue;
                }
    
                // Deserializando registro
                stream.readByte(); // Lápide
                int recordSize = stream.readInt(); // Tamanho do registro
                short jobId = stream.readShort(); // ID do registro
                byte[] data = new byte[recordSize - 3]; // Resto do registro
                stream.readFully(data);
    
                Job job = SecondaryToPrimary.deserializeJob(data);
                job.setJob_id(jobId);
                registrosAtuais[i] = job; // Salva o job lido na memória primária
            }
    
            // Verifica se este é o menor job até agora
            // nenhum job ainda || menor do que o menor job até agora
            if (menorJob == null || registrosAtuais[i].getJob_id() < menorJob.getJob_id()) {
                menorJob = registrosAtuais[i];
                menorIndex = i;
            }
        }
        
        // menorIndex -1 = não entrou no loop, então todos os arquivos em arquivosFinalizados já foram completamente lidos
        if (menorIndex == -1) {
            return null;
        }

        // printdoido(registrosAtuais);
        // System.out.printf("\nMenor Index: %d\nJob ID: %s\n", menorIndex, menorJob.getJob_id());
    
        // O menor job foi atribuído para a variável menorJob, então podemos retirá-lo do array de registros em memória primária
        registrosAtuais[menorIndex] = null;
    
        // Avança índice do arquivo com o menorJob para a próxima posição
        // indexes[menorIndex] += 1;
    
        return menorJob;
    }

    /**
     * Função para atualizar a lista de ponteiros com novos arquivos
     * @param temps_dos lista com DataOutputStreams dos arquivos temporários
     * @param temps_dis lista com DataInputStreams dos arquivos temporários
     * @param temp_path caminho onde os arquivos temporários estão guardados
     */
    public static void reloadPointers(ArrayList<DataOutputStream> temps_dos, ArrayList<DataInputStream> temps_dis, String temp_path, int m) throws IOException {
        // System.out.println("Fechando arquivos...");

        // Fechando DataInputStreams
        for (int i = 0; i < temps_dis.size(); i++) {
            // System.out.println("Fechando leitura de: " + temp_path + "/temp_fos" + i + ".tmp.db");
            temps_dis.get(i).close();
        }

        // Fechando DataOutputStreams
        for (int i = 0; i < temps_dos.size(); i++) {
            // System.out.println("Fechando escrita de: " + temp_path + "/temp_i_fos" + i + ".tmp.db");
            
            // Escrevendo qualquer dado remanescente no buffer no arquivo
            temps_dos.get(i).flush();
            temps_dos.get(i).close();
        }

        // Removendo dis e dos de suas listas
        temps_dis.clear();
        temps_dos.clear();
        System.gc();

        // Reutilização de arquivos já usados
        // Para cada arquivo, copiamos o conteúdo de temp_i_fos para temp_fos
        for (int i = 0; i < m; i++) {
            File source = new File(temp_path + "/temp_i_fos" + i + ".tmp.db");
            File target = new File(temp_path + "/temp_fos" + i + ".tmp.db");

            // Copia o conteúdo do arquivo fonte para o destino, sobrescrevendo
            Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // Opcionalmente, limpamos o arquivo de saída para a próxima rodada:
            source.delete();
            source.createNewFile();
        }
        
        // Reabre os streams:
        // Para entrada: abrimos os arquivos "temp_fos" (agora com o merge feito)
        // Para saída: abrimos os arquivos "temp_i_fos" (que foram limpos)
        for (int i = 0; i < m; i++) {
            // Os arquivos em "/temp_fos" são resultados de escritas anteriores, agora serão usados para leitura usando FileInputStream
            FileInputStream fis = new FileInputStream(temp_path + "/temp_fos" + i + ".tmp.db");
            temps_dis.add(new DataInputStream(fis));
            
            FileOutputStream fos = new FileOutputStream(temp_path + "/temp_i_fos" + i + ".tmp.db");
            temps_dos.add(new DataOutputStream(fos));
        }
    }

    /**
     * Função para trocar o arquivo .db original pelo novo arquivo .db ordenado
     * @param db_path caminho do arquivo banco de dados
     * @param last_id ultimo ID inserido (cabeçalho)
     * @param temps_dis lista com DataInputStreams dos arquivos temporários
     * @param temps_dos lista com DataOutputStreams dos arquivos temporários
     * @param temp_path caminho com arquivos temporários feitos durante a ordenação
     * @param m numéro de caminhos especificados pelo usuário ao iniciar ordenação
     */
    public static void replaceDatabase(String db_path, int last_id, ArrayList<DataInputStream> temps_dis, ArrayList<DataOutputStream> temps_dos, String temp_path, int m) throws IOException {
        // System.out.println("Last ID: " + last_id);

        // Encontrar o arquivo que ainda tem dados disponíveis, ou seja, o resultado final das intercalações
        File finalSortedFile = null;
        for (int i = 0; i < m; i++) {
            File file = new File(temp_path + "/temp_fos" + i + ".tmp.db");
            if (file.exists() && file.length() > 0) {
                finalSortedFile = file;
                break;
            }
        }

        if (finalSortedFile == null) {
            System.err.println("Erro: Nenhum arquivo final encontrado!");
            return;
        }

        // System.out.println("Substituindo o banco de dados original com o arquivo ordenado: " + finalSortedFile.getAbsolutePath());
        // Criar um novo arquivo temporário para escrever com o cabeçalho
        File tempFinalFile = new File(temp_path + "/temp_final.db");
        FileOutputStream fos = new FileOutputStream(tempFinalFile);
        DataOutputStream dos = new DataOutputStream(fos);

        // Escrever o cabeçalho (last_id) no novo arquivo
        dos.writeInt(last_id);
        
        // Copiar o conteúdo do arquivo ordenado para o novo arquivo
        FileInputStream fis = new FileInputStream(finalSortedFile);
        byte[] buffer = new byte[4096];
        int bytesRead;

        // fis.read(buffer) retorna -1 se estiver no fim do arquivo
        while ((bytesRead = fis.read(buffer)) != -1) {
            fos.write(buffer, 0, bytesRead);
        }

        // Fechar streams
        fis.close();
        fos.close();
        dos.flush();
        dos.close();

        // Rodando garbage collector
        System.gc();

        // Substituir o banco de dados original pelo novo arquivo ordenado
        Files.move(tempFinalFile.toPath(), new File(db_path).toPath(), StandardCopyOption.REPLACE_EXISTING);

        System.out.println("Banco de dados atualizado com sucesso!");

        for(DataInputStream dis : temps_dis){
            dis.close();
        }
        for(DataOutputStream _dos : temps_dos){
            _dos.flush();
            _dos.close();
        }
        temps_dis.clear();
        temps_dos.clear();
        System.gc();

        // Remover arquivos temporários
        excluirTemps(temp_path, m);
    }

    /**
     * 
     * @param temp_path caminho com arquivos temporários feitos durante a ordenação
     * @param m número de arquivos
     */
    public static void excluirTemps(String temp_path, int m){
        // System.out.println("Removendo arquivos temporários...");
        for (int i = 0; i < m; i++) {
            File tempFile1 = new File(temp_path + "/temp_fos" + i + ".tmp.db");
            File tempFile2 = new File(temp_path + "/temp_i_fos" + i + ".tmp.db");
    
            if (tempFile1.exists()) {
                if (tempFile1.delete()) {
                    // System.out.println("Arquivo deletado: " + tempFile1.getAbsolutePath());
                } else {
                    // System.err.println("Falha ao deletar: " + tempFile1.getAbsolutePath());
                }
            }
    
            if (tempFile2.exists()) {
                if (tempFile2.delete()) {
                    // System.out.println("Arquivo deletado: " + tempFile2.getAbsolutePath());
                } else {
                    // System.err.println("Falha ao deletar: " + tempFile2.getAbsolutePath());
                }
            }
        }
        // System.out.println("Arquivos temporários removidos!");
    }

    /* FUNÇÕES PARA DEBUG ABAIXO */

    /**
     * Função para debug (leitura do resultado da distribuição inicial)
     */
    public static void test_read(String external_sort_path, int m_caminhos) {
        try {
            for (int i = 0; i < m_caminhos; i++) {
                ArrayList<Job> jobs = new ArrayList<Job>();
                String temp_files_path = external_sort_path + "/temp_fos" + i + ".tmp.db";
                FileInputStream arq = new FileInputStream(temp_files_path);
                DataInputStream dis = new DataInputStream(arq);
                // dis.readInt(); os arquivos temporários não possuem cabeçalho
                while(dis.available() > 0) {
                    Job job = new Job();
                    byte alive = dis.readByte(); // lapide
                    int recordSize = dis.readInt(); // tamanho do registro
                    short jobId = dis.readShort(); // ID do registro
                    byte[] data = new byte[recordSize - 3]; // Já lemos o ID, então o resto é o conteúdo
                    dis.readFully(data);
                    job = SecondaryToPrimary.deserializeJob(data); // deserialize = transformar array de bytes em objeto
                    job.setJob_id(jobId);
                    if (alive == 1) { // adicionar na lista só se o registro tiver vivo
                        jobs.add(job);
                    }
                }
                arq.close();
                dis.close();

                System.out.println("\tARQUIVO TEMPORARIO (" + i + "):\n");
                for (Job job : jobs) {
                    System.out.println(job);
                }
            }
        } catch (Exception e){
            System.err.println("Erro em SecondaryToPrimary.java: " + e);
        }
    }

    /**
     * Função para debug
     */
    public static void filePointerPrint(int[] filePointer){
        for(int i = 0; i < filePointer.length; i++){
            System.out.printf("filePointer[%d] = %d\n", i, filePointer[i]);
        }
    }

    /**
     * Função para debug
     */
    public static void printdoido(Job[] arr){
        System.out.println("Array: ");
        for(int i = 0; i < arr.length; i++){
            if(arr[i] != null){
                System.out.printf("[%d] = %d\n", i, arr[i].getJob_id());
            }
        }
    }
}
