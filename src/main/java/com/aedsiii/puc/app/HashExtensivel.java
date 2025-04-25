package com.aedsiii.puc.app;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

import com.aedsiii.puc.model.RegistroHashExtensivel;

public class HashExtensivel {
    String nomeArquivoDiretorio;
    String nomeArquivoCestos;
    RandomAccessFile arqDiretorio;
    RandomAccessFile arqCestos;
    int quantidadeDadosPorCesto;
    Diretorio diretorio;
    public class Cesto {
        short quantidadeMaxima; // quantidade máxima de elementos que o cesto pode conter
        short bytesPorElemento; // tamanho fixo de cada elemento em bytes
        short bytesPorCesto; // tamanho fixo do cesto em bytes

        byte profundidadeLocal; // profundidade local do cesto
        short quantidade; // quantidade de elementos presentes no cesto
        ArrayList<RegistroHashExtensivel> elementos; // sequência de elementos armazenados

        public Cesto(int qtdmax) throws Exception {
        this(qtdmax, 0);
        }

        public Cesto(int qtdmax, int pl) throws Exception {
            if (qtdmax > 32767)
                throw new Exception("Quantidade máxima de 32.767 elementos");
            if (pl > 127)
                throw new Exception("Profundidade local máxima de 127 bits");
            profundidadeLocal = (byte) pl;
            quantidade = 0;
            quantidadeMaxima = (short) qtdmax;
            elementos = new ArrayList<RegistroHashExtensivel>(quantidadeMaxima);
            bytesPorElemento = RegistroHashExtensivel.size();
            bytesPorCesto = (short) (bytesPorElemento * quantidadeMaxima + 3);
        }

        public byte[] toByteArray() throws Exception {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeByte(profundidadeLocal);
            dos.writeShort(quantidade);
            int i = 0;
            while (i < quantidade) {
                dos.write(elementos.get(i).toByteArray());
                i++;
            }
            byte[] vazio = new byte[bytesPorElemento];
            while (i < quantidadeMaxima) {
                dos.write(vazio);
                i++;
            }
            return baos.toByteArray();
        }

        public void fromByteArray(byte[] ba) throws Exception {
            ByteArrayInputStream bais = new ByteArrayInputStream(ba);
            DataInputStream dis = new DataInputStream(bais);
            profundidadeLocal = dis.readByte();
            quantidade = dis.readShort();
            int i = 0;
            System.out.println("Profundidade Local: " + profundidadeLocal + " Qtd de elementos: " + quantidade);
            elementos = new ArrayList<RegistroHashExtensivel>(quantidadeMaxima);
            byte[] dados = new byte[bytesPorElemento];
            RegistroHashExtensivel elem;
            while (i < quantidadeMaxima) {
                dis.read(dados);
                elem = new RegistroHashExtensivel();
                elem.fromByteArray(dados);
                System.out.println("Elemento " + i + " : " + elem);
                elementos.add(elem);
                i++;
            }
        }

        // Inserir elementos no cesto
        public boolean create(RegistroHashExtensivel elem) {
            if (full())
                return false;
            int i = quantidade - 1; // posição do último elemento no cesto
            while (i >= 0 && elem.id < elementos.get(i).id)
                i--;
            elementos.add(i + 1, elem);
            quantidade++;
            return true;
        }

        // Buscar um elemento no cesto
        public RegistroHashExtensivel read(int chave) {
            System.out.println("To pegando o elemento no cesto");
            if (empty()){
                //System.out.println("Cesto vazio");
                return null;
            }
            int i = 0;
            //System.out.println("Procurando no cesto, i = " + i);
            while (i < quantidade){
                if (i < quantidade && chave == elementos.get(i).id){
                    return elementos.get(i);
                }
                //RegistroHashExtensivel e = elementos.get(i);
                // System.out.println("Atual: " + e);
                // System.out.println("ID: " + e.id + " Offset: " + e.offset);
                i++;
            }
                //RegistroHashExtensivel e = elementos.get(i);
                // System.out.println("Atual: " + e);
                // System.out.println("ID: " + e.id + " Offset: " + e.offset);
            return null;
        }

        // atualizar um elemento do cesto
        public boolean update(RegistroHashExtensivel elem) {
            if (empty())
                return false;
            int i = 0;
            while (i < quantidade && elem.hashCode() > elementos.get(i).hashCode())
                i++;
            if (i < quantidade && elem.hashCode() == elementos.get(i).hashCode()) {
                elementos.set(i, elem);
                return true;
            } else
                return false;
        }

        // pagar um elemento do cesto
        public boolean delete(int chave) {
            if (empty())
                return false;
            int i = 0;
            while (i < quantidade && chave > elementos.get(i).hashCode())
                i++;
            if (chave == elementos.get(i).hashCode()) {
                elementos.remove(i);
                quantidade--;
                return true;
            } else
                return false;
        }

        public boolean empty() {
            return quantidade == 0;
        }

        public boolean full() {
            return quantidade == quantidadeMaxima;
        }

        public String toString() {
            String s = "Profundidade Local: " + profundidadeLocal + "\nQuantidade: " + quantidade + "\n| ";
            int i = 0;
            while (i < quantidade) {
                s += elementos.get(i).toString() + " | ";
                i++;
            }
            while (i < quantidadeMaxima) {
                s += "- | ";
                i++;
                }
                return s;
            }

        public int size() {
            return bytesPorCesto;
        }

    }
    protected class Diretorio {
        byte profundidadeGlobal;
        protected short cestos_quantidade;
        long[] enderecos;
        public Diretorio() {
            profundidadeGlobal = 0;
            cestos_quantidade = 0;
            enderecos = new long[1];
            enderecos[0] = 0;
        }
        public Diretorio(int n) {
            profundidadeGlobal = 0;
            cestos_quantidade = (short)n;
            enderecos = new long[1];
            enderecos[0] = 0;
        }

        public boolean atualizaEndereco(int p, long e) {
            if (p > Math.pow(2, profundidadeGlobal))
                return false;
            enderecos[p] = e;
            return true;
        }

        public byte[] toByteArray() throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeByte(profundidadeGlobal);
            dos.writeShort(cestos_quantidade);
            int quantidade = (int) Math.pow(2, profundidadeGlobal);
            int i = 0;
            while (i < quantidade) {
                dos.writeLong(enderecos[i]);
                i++;
            }
            return baos.toByteArray();
        }

        public void fromByteArray(byte[] ba) throws IOException {
            ByteArrayInputStream bais = new ByteArrayInputStream(ba);
            DataInputStream dis = new DataInputStream(bais);
            profundidadeGlobal = dis.readByte();
            cestos_quantidade = dis.readShort();
            int quantidade = (int) Math.pow(2, profundidadeGlobal);
            enderecos = new long[quantidade];
            int i = 0;
            while (i < quantidade) {
                enderecos[i] = dis.readLong();
                i++;
            }
        }

        public String toString() {
            String s = "\nProfundidade global: " + profundidadeGlobal;
            int i = 0;
            int quantidade = (int) Math.pow(2, profundidadeGlobal);
            while (i < quantidade) {
                s += "\n" + i + ": " + enderecos[i];
                i++;
            }
            return s;
        }

        protected long endereço(int p) {
            if (p > Math.pow(2, profundidadeGlobal))
                return -1;
            return enderecos[p];
        }

        protected boolean duplica() {
            if (profundidadeGlobal == 127)
                return false;
            profundidadeGlobal++;
            int q1 = (int) Math.pow(2, profundidadeGlobal - 1);
            int q2 = (int) Math.pow(2, profundidadeGlobal);
            long[] novosEnderecos = new long[q2];
            int i = 0;
            while (i < q1) { // copia o vetor anterior para a primeiro metade do novo vetor
                novosEnderecos[i] = enderecos[i];
                i++;
            }
            while (i < q2) { // copia o vetor anterior para a segunda metade do novo vetor
                novosEnderecos[i] = enderecos[i - q1];
                i++;
            }
            enderecos = novosEnderecos;
            return true;
        }

        // Para efeito de determinar o cesto em que o elemento deve ser inserido,
        // só serão considerados valores absolutos da chave.
        protected int hash(int chave) {
            return Math.abs(chave) % (int) Math.pow(2, profundidadeGlobal);
        }

        // Método auxiliar para atualizar endereço ao duplicar o diretório
        protected int hash2(int chave, int pl) { // cálculo do hash para uma dada profundidade local
            return Math.abs(chave) % (int) Math.pow(2, pl);
        }
    }
    public HashExtensivel(int n, String nd, String nc, boolean createnew) throws Exception {
            quantidadeDadosPorCesto = n;
            nomeArquivoDiretorio = nd;
            nomeArquivoCestos = nc;
            arqDiretorio = new RandomAccessFile(nomeArquivoDiretorio, "rw");
            arqCestos = new RandomAccessFile(nomeArquivoCestos, "rw");

            // Se o diretório ou os cestos estiverem vazios, cria um novo diretório e lista
            // de cestos
            if (createnew || arqDiretorio.length() == 0 || arqCestos.length() == 0) {
                // Cria um novo diretório, com profundidade de 0 bits (1 único elemento)
                diretorio = new Diretorio(n);
                byte[] bd = diretorio.toByteArray();
                arqDiretorio.write(bd);

                // Cria um cesto vazio, já apontado pelo único elemento do diretório
                Cesto c = new Cesto(quantidadeDadosPorCesto);
                bd = c.toByteArray();
                arqCestos.seek(0);
                arqCestos.write(bd);
            } else {
                byte[] bd = new byte[(int) arqDiretorio.length()];
                arqDiretorio.seek(0);
                arqDiretorio.read(bd);
                diretorio = new Diretorio();
                diretorio.fromByteArray(bd);
            }
    }
    public boolean create(RegistroHashExtensivel elem) throws Exception {
        // Carrega TODO o diretório para a memória
        byte[] bd = new byte[(int) arqDiretorio.length()];
        arqDiretorio.seek(0);
        arqDiretorio.read(bd);
        diretorio = new Diretorio();
        diretorio.fromByteArray(bd);

        // Identifica a hash do diretório,
        int i = diretorio.hash(elem.id);

        // Recupera o cesto
        long enderecoCesto = diretorio.endereço(i);
        Cesto c = new Cesto(quantidadeDadosPorCesto);
        byte[] ba = new byte[c.size()];
        arqCestos.seek(enderecoCesto);
        arqCestos.read(ba);
        c.fromByteArray(ba);

        // Testa se a chave já não existe no cesto
        if (c.read(elem.id) != null)
            throw new Exception("Elemento já existe");

        // Testa se o cesto já não está cheio
        // Se não estiver, create o par de chave e dado
        if (!c.full()) {
            // Insere a chave no cesto e o atualiza
            c.create(elem);
            arqCestos.seek(enderecoCesto);
            arqCestos.write(c.toByteArray());
            return true;
        }

        // Duplica o diretório
        byte pl = c.profundidadeLocal;
        if (pl >= diretorio.profundidadeGlobal)
            diretorio.duplica();
        byte pg = diretorio.profundidadeGlobal;

        // Cria os novos cestos, com os seus dados no arquivo de cestos
        Cesto c1 = new Cesto(quantidadeDadosPorCesto, pl + 1);
        arqCestos.seek(enderecoCesto);
        arqCestos.write(c1.toByteArray());

        Cesto c2 = new Cesto(quantidadeDadosPorCesto, pl + 1);
        long novoEndereco = arqCestos.length();
        arqCestos.seek(novoEndereco);
        arqCestos.write(c2.toByteArray());

        // Atualiza os dados no diretório
        int inicio = diretorio.hash2(elem.id, c.profundidadeLocal);
        int deslocamento = (int) Math.pow(2, pl);
        int max = (int) Math.pow(2, pg);
        boolean troca = false;
        for (int j = inicio; j < max; j += deslocamento) {
            if (troca)
                diretorio.atualizaEndereco(j, novoEndereco);
            troca = !troca;
        }

        // Atualiza o arquivo do diretório
        bd = diretorio.toByteArray();
        arqDiretorio.seek(0);
        arqDiretorio.write(bd);

        // Reinsere as chaves do cesto antigo
        for (int j = 0; j < c.quantidade; j++) {
            create(c.elementos.get(j));
        }
        create(elem); // insere o nome elemento
        return true;
    }

    public RegistroHashExtensivel read(int chave) throws Exception {
        System.out.println("Carregando o diretorio: ");
        // Carrega o diretório
        byte[] bd = new byte[(int) arqDiretorio.length()];
        arqDiretorio.seek(0);
        arqDiretorio.read(bd);
        diretorio = new Diretorio();
        diretorio.fromByteArray(bd);
        System.out.println("Diretorio carregado com sucesso.");
        // Identifica a hash do diretório,
        int i = diretorio.hash(chave);
        System.out.println("Encontrado o i: " + i);
        // Recupera o cesto
        long enderecoCesto = diretorio.endereço(i);
        System.out.println("Endereco do cesto: " + enderecoCesto);
        Cesto c = new Cesto(quantidadeDadosPorCesto);
        byte[] ba = new byte[c.size()];
        arqCestos.seek(enderecoCesto);
        arqCestos.read(ba);
        c.fromByteArray(ba);
        return c.read(chave);
    }

    public boolean update(RegistroHashExtensivel elem) throws Exception {
        // Carrega o diretório
        byte[] bd = new byte[(int) arqDiretorio.length()];
        arqDiretorio.seek(0);
        arqDiretorio.read(bd);
        diretorio = new Diretorio();
        diretorio.fromByteArray(bd);

        // Identifica a hash do diretório,
        int i = diretorio.hash(elem.hashCode());

        // Recupera o cesto
        long enderecoCesto = diretorio.endereço(i);
        Cesto c = new Cesto(quantidadeDadosPorCesto);
        byte[] ba = new byte[c.size()];
        arqCestos.seek(enderecoCesto);
        arqCestos.read(ba);
        c.fromByteArray(ba);

        // atualiza o dado
        if (!c.update(elem))
            return false;

        // Atualiza o cesto
        arqCestos.seek(enderecoCesto);
        arqCestos.write(c.toByteArray());
        return true;
    }

    public boolean delete(int chave) throws Exception {
        // Carrega o diretório
        byte[] bd = new byte[(int) arqDiretorio.length()];
        arqDiretorio.seek(0);
        arqDiretorio.read(bd);
        diretorio = new Diretorio();
        diretorio.fromByteArray(bd);

        // Identifica a hash do diretório,
        int i = diretorio.hash(chave);

        // Recupera o cesto
        long enderecoCesto = diretorio.endereço(i);
        Cesto c = new Cesto(quantidadeDadosPorCesto);
        byte[] ba = new byte[c.size()];
        arqCestos.seek(enderecoCesto);
        arqCestos.read(ba);
        c.fromByteArray(ba);

        // delete a chave
        if (!c.delete(chave))
        return false;

        // Atualiza o cesto
        arqCestos.seek(enderecoCesto);
        arqCestos.write(c.toByteArray());
        return true;
    }

    public void print() {
        try {
            byte[] bd = new byte[(int) arqDiretorio.length()];
            arqDiretorio.seek(0);
            arqDiretorio.read(bd);
            diretorio = new Diretorio();
            diretorio.fromByteArray(bd);
            System.out.println("\nDIRETÓRIO ------------------");
            System.out.println(diretorio);

            System.out.println("\nCESTOS ---------------------");
            arqCestos.seek(0);
            while (arqCestos.getFilePointer() != arqCestos.length()) {
                System.out.println("Endereço: " + arqCestos.getFilePointer());
                Cesto c = new Cesto(quantidadeDadosPorCesto);
                byte[] ba = new byte[c.size()];
                arqCestos.read(ba);
                c.fromByteArray(ba);
                System.out.println(c + "\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public short cestosSize() {
        return diretorio.cestos_quantidade;
    }
}
