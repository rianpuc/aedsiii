package com.aedsiii.puc.model;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

class HuffmanNode implements Comparable<HuffmanNode> {
    byte b;
    int frequencia;
    HuffmanNode esquerdo, direito;

    public HuffmanNode(byte b, int f) {
        this.b = b;
        this.frequencia = f;
        esquerdo = direito = null;
    }

    @Override
    public int compareTo(HuffmanNode o) {
        return this.frequencia - o.frequencia;
    }
}

public class Huffman {
    /**
     * Funcao para encodificar um array de bytes utilizando Huffman.
     * @param bytes - Array de bytes do arquivo original a ser codificado
     * @param index - Numero do proximo arquivo de codificacao
     * @throws IOException - Possiveis erros de leitura/escrita
     */
    public static void encode(byte[] bytes, int index) throws IOException {
        HashMap<Byte, Integer> charFreq = getCharFrequency(bytes); // Pegando a frequencia dos caracteres presentes
        //System.out.println(charFreq);
        PriorityQueue<HuffmanNode> pq = buildPriorityQueue(charFreq); // Criando uma priority queue com a frequencia
        HuffmanNode raiz = buildHuffmanTree(pq); // Construindo a arvore com a priority queue
        HashMap<Byte, String> codigos = new HashMap<>(); // Criando o Hash de codigo para cada caracter
        constroiCodigos(raiz, "", codigos); // Construindo os codigos para cada caracter do texto
        //System.out.println(codigos);
        String sequencia = codificar(codigos, bytes); // Codificando a sequencia inteira de todas as ocorrencias na ordem
        DataOutputStream dos = new DataOutputStream(new FileOutputStream("snapshots/binary_dbHuffmanCompressao" + index + ".db")); // Novo arquivo com o index
        dos.writeInt(charFreq.size()); // Escrevendo o tamanho do hash de frequencias
        for(Map.Entry<Byte, Integer> entry : charFreq.entrySet()){ // Iterando pelo hash escrevendo a chave e o valor
            dos.writeByte(entry.getKey());
            dos.writeInt(entry.getValue());
        }
        dos.writeLong((long)bytes.length); // Escrevendo o tamanho do texto codificado
        writeEncoded(dos, sequencia); // Escrevendo de forma codificada todos os bytes bit a bit
        dos.close();
    }
    /**
     * Funcao que decodifica um arquivo de Huffman comprimido.
     * @param version - Numero do arquivo a ser decodificado
     * @return byte[] - Array de bytes decodificado para escrever o arquivo original.
     * @throws IOException
     */
    public static byte[] decode(int version) throws IOException{
        DataInputStream dis = new DataInputStream(new FileInputStream("snapshots/binary_dbHuffmanCompressao" + version + ".db")); // Pegando o arquivo comprimido
        HashMap<Byte, Integer> mapaRecuperado = new HashMap<>(); // Variavel para recuperar o mapa salvo no arquivo
        int tamanhoMapa = dis.readInt(); // Variavel para descobrir o tamanho do mapa e entao fazer um for
        for(int i = 0; i < tamanhoMapa; i++){
            byte b = dis.readByte();
            int freq = dis.readInt();
            mapaRecuperado.put(b, freq); // Lendo o byte e sua frequencia
        }
        HuffmanNode arvore = buildHuffmanTree(buildPriorityQueue(mapaRecuperado)); // Criando uma arvore com a priority queue das frequencias recuperadas do arquivo
        long totalBits = dis.readLong(); // Lendo quantos bits irei ler ao total
        LeitorDeBits leitorDeBits = new LeitorDeBits(dis); // Leitor de bit a bit, tendo em vista que a mensagem esta codificada.
        HuffmanNode noAtual = arvore; // Auxiliar para navegar na arvore
        ByteArrayOutputStream baos = new ByteArrayOutputStream(); // ByteArrayOutputStream para concatenar os bytes decodificados
        for(int i = 0; i < totalBits; i++){
            noAtual = arvore;
            while(noAtual.esquerdo != null && noAtual.direito != null){
                int bit = leitorDeBits.lerProximoBit(); // Navegando na arvore lendo bit a bit
                if(bit == 0){
                    noAtual = noAtual.esquerdo;
                } else {
                    noAtual = noAtual.direito;
                }
            }
            baos.write(noAtual.b); // Quando chegar na folha, concateno o byte correspondente ao array de bytes.
        }
        dis.close();
        return baos.toByteArray(); // Retorando o array de bytes
    }
    /**
     * Funcao recursiva que navega pela arvore inteira construindo o codigo de cada byte
     * @param no - No atual que esta navegando
     * @param codigo - String concatenada do codigo atual
     * @param codigos - HashMap de codigos que vai salvar o codigo de cada byte da arvore.
     */
    public static void constroiCodigos(HuffmanNode no, String codigo, HashMap<Byte, String> codigos){
        if (no == null){
            return;
        }
        if (no.esquerdo == null && no.direito == null) {
            codigos.put(no.b, codigo); // Quando chegar num no folha, retorno o codigo que construiu enquanto foi navegando pela arvore
        }
        constroiCodigos(no.esquerdo, codigo + "0", codigos); // Quando for para a esquerda, concatena 0 no codigo.
        constroiCodigos(no.direito, codigo + "1", codigos); // Quando for para a direita, concatena 1 no codigo.
    }
    /**
     * Funcao que calcula a frequencia de cada ocorrencia de bytes no array
     * @param bytes - Array de bytes para pegar a frequencia de cada um
     * @return HashMap<Byte, Integer> - HashMap com a frequencia em int de cada um dos bytes
     */
    public static HashMap<Byte, Integer> getCharFrequency(byte[] bytes){
        HashMap<Byte, Integer> res = new HashMap<>();
        for(byte b : bytes){
            res.put(b, res.getOrDefault(b, 0) + 1); // Loop para encontrar o caracter no hash, e entao incrementar sua frequencia por 1
        }
        return res;
    }
    /**
     * Funcao que retorna uma PriorityQueue de bytes baseado na frequencia dos mesmos
     * @param charFreq - HashMap com a frequencia em int de cada um dos bytes
     * @return PriorityQueue<HuffmanNode> - Nos da arvore
     */
    public static PriorityQueue<HuffmanNode> buildPriorityQueue(HashMap<Byte, Integer> charFreq){
        PriorityQueue<HuffmanNode> pq = new PriorityQueue<>();
        for(byte b : charFreq.keySet()){
            pq.add(new HuffmanNode(b, charFreq.get(b))); // Criando uma priorityqueue com base na frequencia dos caracteres
        }
        return pq;
    }
    /**
     * Funcao para construir a arvore de Huffman com base na Priority queue
     * @param pq - PriorityQueue para montar a arvore
     * @return Raiz da Arvore
     */
    public static HuffmanNode buildHuffmanTree(PriorityQueue<HuffmanNode> pq){
        HuffmanNode raiz = null;
        while(pq.size() > 1){
            HuffmanNode esquerdo = pq.poll(); // Pegando o primeiro da fila
            HuffmanNode direito = pq.poll(); // Pegando o segundo da fila
            HuffmanNode pai = new HuffmanNode((byte)0, esquerdo.frequencia + direito.frequencia); // Juntando ambos com maior prioridade
            // Referenciando os filhos
            pai.esquerdo = esquerdo;
            pai.direito = direito;
            pq.add(pai); // Adicionando o pai no fim da lista
        }
        raiz = pq.poll(); // A raiz eh o ultimo item que sobrou na lista
        return raiz;
    }
    /**
     * Funcao que retorna o codigo codificado da arvore de huffman
     * @param codigos - Os bytes e os codigos necessarios para acha-los na arvore
     * @param palavra - Array de byte do arquivo original para pegarmos na ordem do arquivo
     * @return
     */
    public static String codificar(HashMap<Byte, String> codigos, byte[] palavra){
        StringBuilder sequenciaCodificada = new StringBuilder(); // Criando uma string para concatenar
        for (byte b : palavra){
            String codigo = codigos.get(b); // Pegando o byte e procurando qual o codigo correspondente para codificar
            for(char cc : codigo.toCharArray()){  // Transformando em um array de char para iterar
                if(cc=='0') sequenciaCodificada.append('0'); // Caso for 0, concatena 0 no codigo final
                else sequenciaCodificada.append('1'); // Caso for 1, concatena 1 no codigo final
            }
        }
        // Retorna a sequencia codificada
        String sequencia = sequenciaCodificada.toString();
        return sequencia;  
    }
    /**
     * Funcao que escreve bit a bit da string codificada no arquivo
     * @param dos - DataOutputStream para o arquivo codificado
     * @param coded - String codificada da arvore
     * @throws IOException
     */
    public static void writeEncoded(DataOutputStream dos, String coded) throws IOException {
        byte currentByte = 0; // Auxiliar para saber o byte que estou
        int bitCount = 0; // Auxiliar para saber quantos bits escrevi
        for(char bitChar : coded.toCharArray()){
            currentByte <<= 1; // Dando shift left para construir um byte
            if(bitChar == '1') {
                currentByte |= 1; // Caso o bit atual for 1, adiciono um 1 no meu bit
            }
            bitCount++; 
            if(bitCount == 8){ // Quando chegar em 8 bits escritos, equivalente a um byte, escrevo o byte atual
                dos.writeByte(currentByte);
                currentByte = 0;
                bitCount = 0;
            }
        }
        if(bitCount > 0){ // Caso o ultimo byte nao tenha conseguido alcancar 8 bits, escreva-o assim mesmo
            currentByte <<= (8 - bitCount);
            dos.writeByte(currentByte);
        }
    }
}
