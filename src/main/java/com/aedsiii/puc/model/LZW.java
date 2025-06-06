package com.aedsiii.puc.model;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class LZW {
    /**
     * Funcao que preenche o dicionario inicial de codificacao com os bytes e suas posicoes
     * @param dicionario - HashMap que vai guardar a sequencia de bytes e sua posicao
     */
    public static void startingEncodingDictionary(HashMap<ByteSequence, Integer> dicionario){
        //System.out.println("Dicionario inicial: ");
        int j = 0;
        for(int i = -128; i < 128; i++){
            //System.out.printf("Byte: %d (char %c) | Pos: %d\n", i, i, entries);
            dicionario.put(new ByteSequence((byte)i), j++);
        }
    }
    /**
     * Funcao que preenche o dicionario inicial de decodificacao com suas posicoes e suas respectivas sequencias de bytes
     * @param dicionario - HashMap que vai guardar a posicao e sua sequencia de bytes
     */
    public static void startingDecodingDictionary(HashMap<Integer, ByteSequence> dicionario){
        int j = 0;
        for(int i = -128; i < 128; i++){
            dicionario.put(j++, new ByteSequence((byte)i));
        }
        //System.out.println("Decoding dictionary");
    }
    // public static void testando(HashMap<ByteSequence, Integer> dicionario){
    //     for(int i = 32; i < 128; i++){
    //         ByteSequence cobaia = new ByteSequence((byte)i);
    //         System.out.println("O byte " + i + " existe no Hash? " + dicionario.containsKey(cobaia));
    //     }
    // }
    /**
     * Funcao que vai ler todo o array de bytes, e entao pegar as sequencias de bytes concatenadas e salvar no Hash
     * para entao, retornar um Array de inteiros com todas as posicoes em sequencia das sequencias de bytes.
     * @param dicionario - HashMap ja inicializado com os bytes padroes
     * @param bytes - Array de bytes do arquivo original
     * @return
     */
    public static ArrayList<Integer> gerandoCodigo(HashMap<ByteSequence, Integer> dicionario, byte[] bytes){
        ArrayList<Integer> res = new ArrayList<>(); // Inicializando ArrayList de inteiros para salvar as posicoes
        int prox = 256; // Auxiliar pois existem 255 bytes no dicionario, entao o proximo eh 256
        ByteSequence ant = new ByteSequence(new byte[0]); // Auxiliar para salvar o ByteSequence lido anterior
        ByteSequence att = null; // Auxiliar para salvar o atual
        for(byte b : bytes) { // Iterando sob todos os bytes do arquivo original
            //System.out.printf("O(s) byte(s) abaixo existem no Hash?\n");
            att = new ByteSequence(ant, b); // Faco um novo ByteSequence com o anterior e o atual
            //System.out.print(att);
            if(dicionario.containsKey(att)) { // Procuro se existe a sequencia nova que fiz no dicionario, caso existir, salvo no auxiliar para tentar pegar a maior possivel
                //System.out.println("Sim, pegando maior string.");
                ant = att;
            } else { // Caso nao exista, adiciono a posicao do anterior no ArrayList, e crio uma nova ocorrencia com o ByteSequence atual no dicionario
                //System.out.println("Nao, entao o codigo eh: " + dicionario.get(ant));
                res.add(dicionario.get(ant));
                //System.out.println(att);
                dicionario.put(att, prox++);
                ant = new ByteSequence(b);
            }
        }
        if(!ant.isEmpty()){ // Se nao estiver vazio, coloco a ultima ocorrencia no arraylist
            res.add(dicionario.get(ant));
        }
        return res;
    }
    /**
     * Funcao que decodifica um arquivo LZW e retorna um ByteSequence decodificado (arquivo original)
     * @param dicionario - HashMap que salva a posicao e o bytesequence correspondente a esse inteiro
     * @param codigo - Array de inteiros com a sequencia codificada
     * @return
     */
    public static ByteSequence decodificandoCodigo(HashMap<Integer, ByteSequence> dicionario, ArrayList<Integer> codigo){
        int prox = 256; // Auxiliar pois existem 255 bytes no dicionario, entao o proximo eh 256
        int primeiroCodigo = codigo.get(0); // Pegando a primeira ocorrencia
        ByteSequence anterior = new ByteSequence(dicionario.get(primeiroCodigo)); // Auxiliar inicializando a primeira ocorrencia
        ByteSequence res = new ByteSequence(anterior); // Auxiliar que vai ser o retorno com todos os ByteSequences
        for(int i = 1; i < codigo.size(); i++){ 
            int codigoAtual = codigo.get(i); // Pegando em ordem, todos os inteiros
            ByteSequence entrada;
            if(dicionario.containsKey(codigoAtual)) // Caso esse codigo possua um ByteSequence ja encontrado, o referencia
                entrada = dicionario.get(codigoAtual);
            else entrada = new ByteSequence(anterior, anterior.getData()[0]);
            ByteSequence novo = new ByteSequence(anterior, entrada.getData()[0]); // Criando uma nova sequencia com a anterior e o primeiro byte da proxima
            res = new ByteSequence(res, entrada);
            dicionario.put(prox++, novo); // Salvando no dicionario
            anterior = entrada; // Retomando da onde parou
        }
        return res;
    }
    /**
     * Funcao que codifica um array de bytes e salva em um arquivo comprimido
     * @param bytes - Array de bytes do arquivo original
     * @param index - Numero do arquivo a ser salvo
     * @throws IOException
     */
    public static void encode(byte[] bytes, int index) throws IOException{
        HashMap<ByteSequence, Integer> dicionarioCodificacao = new HashMap<>(); 
        startingEncodingDictionary(dicionarioCodificacao); // Inicializando o dicionario de codificacao
        ArrayList<Integer> res = gerandoCodigo(dicionarioCodificacao, bytes); // Gerando um array de inteiros com todos os codigos em ordem
        DataOutputStream dos = new DataOutputStream(new FileOutputStream("snapshots/binary_dbLZWCompressao" + index + ".db")); // Criando o arquivo
        int tamanho = dicionarioCodificacao.size(); // Pegando o tamanho do dicionario
        int bits = (int)Math.ceil(Math.log(tamanho) / Math.log(2)); // Descobrindo quantos bits serao necessarios para salvar todas as posicoes
        dos.writeByte((byte)bits); // Salvando a quantidade de bits
        writeEncoded(dos, res, bits); // Escrevendo, bit a bit, com o tamanho de bits especificado, no arquivo
        dos.close();
    }
    /**
     * Funcao que decodifica um arquivo selecionado
     * @param version - Versao do arquivo a ser decodificado
     * @return Array de bytes do arquivo original
     * @throws IOException
     */
    public static byte[] decode(int version) throws IOException {
        DataInputStream dis = new DataInputStream(new FileInputStream("snapshots/binary_dbLZWCompressao" + version + ".db")); // Lendo o arquivo escolhido
        int bitLength = (int)dis.readByte(); // Descobrindo quantos bits esta codificado
        //System.out.println("bitlenght: " + bitLength);
        ArrayList<Integer> codigo = new ArrayList<>(); 
        readEncoded(dis, codigo, bitLength); // Lendo bit a bit, conforme a quantidade de bits, para formar um ArrayList<Integer> com os codigos em ordem
        //System.out.println("Codigo: " + codigo);
        HashMap<Integer, ByteSequence> dicionarioDecodificacao = new HashMap<>();
        startingDecodingDictionary(dicionarioDecodificacao); // Inicializando dicionario de decodificacao
        ByteSequence res = decodificandoCodigo(dicionarioDecodificacao, codigo); // Pegando o ByteSequence do arquivo original apos decodificar o codigo
        byte[] array = res.getData(); // Pegando o array
        return array;
    }
    /**
     * Funcao que escreve bit por bit no arquivo conforme o tamanho especificado
     * @param dos - Arquivo de escrita
     * @param codigo - Array de inteiros que no caso eh o codigo
     * @param bitLength - Tamanho de bits para escrever
     * @throws IOException
     */
    public static void writeEncoded(DataOutputStream dos, ArrayList<Integer> codigo, int bitLength) throws IOException {
        int bitBuffer = 0; // Guardar o bit 
        int bitCount = 0; // Saber quantos ja escreveu
        for (Integer valor : codigo) {
            bitBuffer = (bitBuffer << bitLength) | valor; // Montando um inteiro na quantidade de bits
            bitCount += bitLength; // Somando na quantidade para escrever
            while (bitCount >= 8) { 
                bitCount -= 8; // Diminui por 8 pois cada byte tem 8 bits
                int byteParaEscrever = (bitBuffer >> bitCount) & 0xFF;
                dos.write(byteParaEscrever);
            }
        }
        if (bitCount > 0) {
            int ultimoByte = (bitBuffer << (8 - bitCount)) & 0xFF;
            dos.write(ultimoByte);
        }
        dos.flush();
    }
    /**
     * Funcao que le um arquivo codificado e monta um array de codigos inteiros
     * @param dis - Arquivo de entrada codificado
     * @param codigo - Array de inteiros a ser retornado
     * @param bitLength - Tamanho de bits da mensagem codificada
     * @throws IOException
     */
    public static void readEncoded(DataInputStream dis, ArrayList<Integer> codigo, int bitLength) throws IOException{
        int buffer = 0;
        int bufferLength = 0;
        while (true) {
            int nextByte;
            try {
                nextByte = dis.readUnsignedByte();
            } catch (EOFException e) {
                break;
            }
            buffer = (buffer << 8) | nextByte;
            bufferLength += 8;
            while (bufferLength >= bitLength) {
                int shift = bufferLength - bitLength;
                int value = (buffer >> shift) & ((1 << bitLength) - 1);
                //System.out.println(dis.available());
                codigo.add(value);
                bufferLength -= bitLength;
                buffer = buffer & ((1 << bufferLength) - 1);
            }
        }
        dis.close();
    }
}
