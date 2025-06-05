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
    public static void encode(byte[] bytes, int index) throws IOException {
        HashMap<Byte, Integer> charFreq = getCharFrequency(bytes);
        //System.out.println(charFreq);
        PriorityQueue<HuffmanNode> pq = buildPriorityQueue(charFreq);
        HuffmanNode raiz = buildHuffmanTree(pq);
        HashMap<Byte, String> codigos = new HashMap<>();
        constroiCodigos(raiz, "", codigos);
        //System.out.println(codigos);
        String sequencia = codificar(codigos, bytes);
        DataOutputStream dos = new DataOutputStream(new FileOutputStream("snapshots/binary_dbHuffmanCompressao" + index + ".db"));
        dos.writeInt(charFreq.size());
        for(Map.Entry<Byte, Integer> entry : charFreq.entrySet()){
            dos.writeByte(entry.getKey());
            dos.writeInt(entry.getValue());
        }
        dos.writeLong((long)bytes.length);
        writeEncoded(dos, sequencia);
        dos.close();
    }
    public static byte[] decode(int version) throws IOException{
        DataInputStream dis = new DataInputStream(new FileInputStream("snapshots/binary_dbHuffmanCompressao" + version + ".db"));
        HashMap<Byte, Integer> mapaRecuperado = new HashMap<>();
        int tamanhoMapa = dis.readInt();
        for(int i = 0; i < tamanhoMapa; i++){
            byte b = dis.readByte();
            int freq = dis.readInt();
            mapaRecuperado.put(b, freq);
        }
        HuffmanNode arvore = buildHuffmanTree(buildPriorityQueue(mapaRecuperado));
        long totalBits = dis.readLong();
        LeitorDeBits leitorDeBits = new LeitorDeBits(dis);
        HuffmanNode noAtual = arvore;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for(int i = 0; i < totalBits; i++){
            noAtual = arvore;
            while(noAtual.esquerdo != null && noAtual.direito != null){
                int bit = leitorDeBits.lerProximoBit();
                if(bit == 0){
                    noAtual = noAtual.esquerdo;
                } else {
                    noAtual = noAtual.direito;
                }
            }
            baos.write(noAtual.b);
        }
        dis.close();
        return baos.toByteArray();
    }
    public static void constroiCodigos(HuffmanNode no, String codigo, HashMap<Byte, String> codigos){
        if (no == null){
            return;
        }
        if (no.esquerdo == null && no.direito == null) {
            codigos.put(no.b, codigo);
        }
        constroiCodigos(no.esquerdo, codigo + "0", codigos);
        constroiCodigos(no.direito, codigo + "1", codigos);
    }
    public static HashMap<Byte, Integer> getCharFrequency(byte[] bytes){
        HashMap<Byte, Integer> res = new HashMap<>();
        for(byte b : bytes){
            res.put(b, res.getOrDefault(b, 0) + 1);
        }
        return res;
    }
    public static PriorityQueue<HuffmanNode> buildPriorityQueue(HashMap<Byte, Integer> charFreq){
        PriorityQueue<HuffmanNode> pq = new PriorityQueue<>();
        for(byte b : charFreq.keySet()){
            pq.add(new HuffmanNode(b, charFreq.get(b)));
        }
        return pq;
    }
    public static HuffmanNode buildHuffmanTree(PriorityQueue<HuffmanNode> pq){
        HuffmanNode raiz = null;
        while(pq.size() > 1){
            HuffmanNode esquerdo = pq.poll();
            HuffmanNode direito = pq.poll();
            HuffmanNode pai = new HuffmanNode((byte)0, esquerdo.frequencia + direito.frequencia);
            pai.esquerdo = esquerdo;
            pai.direito = direito;
            pq.add(pai);
        }
        raiz = pq.poll();
        return raiz;
    }
    public static String codificar(HashMap<Byte, String> codigos, byte[] palavra){
        StringBuilder sequenciaCodificada = new StringBuilder();
        for (byte b : palavra){
            String codigo = codigos.get(b);
            for(char cc : codigo.toCharArray()){
                if(cc=='0') sequenciaCodificada.append('0');
                else sequenciaCodificada.append('1');
            }
        }
        String sequencia = sequenciaCodificada.toString();
        return sequencia;  
    }
    public static void writeEncoded(DataOutputStream dos, String coded) throws IOException {
        byte currentByte = 0;
        int bitCount = 0;
        for(char bitChar : coded.toCharArray()){
            currentByte <<= 1;
            if(bitChar == '1') {
                currentByte |= 1;
            }
            bitCount++;
            if(bitCount == 8){
                dos.writeByte(currentByte);
                currentByte = 0;
                bitCount = 0;
            }
        }
        if(bitCount > 0){
            currentByte <<= (8 - bitCount);
            dos.writeByte(currentByte);
        }
    }
}
