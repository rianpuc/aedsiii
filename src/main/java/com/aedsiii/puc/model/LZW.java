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
    public static void startingEncodingDictionary(HashMap<ByteSequence, Integer> dicionario){
        //System.out.println("Dicionario inicial: ");
        int j = 0;
        for(int i = -128; i < 128; i++){
            //System.out.printf("Byte: %d (char %c) | Pos: %d\n", i, i, entries);
            dicionario.put(new ByteSequence((byte)i), j++);
        }
    }
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
    public static ArrayList<Integer> gerandoCodigo(HashMap<ByteSequence, Integer> dicionario, byte[] bytes){
        ArrayList<Integer> res = new ArrayList<>();
        int prox = 256;
        ByteSequence ant = new ByteSequence(new byte[0]);
        ByteSequence att = null;
        for(byte b : bytes) {
            //System.out.printf("O(s) byte(s) abaixo existem no Hash?\n");
            att = new ByteSequence(ant, b);
            //System.out.print(att);
            if(dicionario.containsKey(att)) {
                //System.out.println("Sim, pegando maior string.");
                ant = att;
            } else {
                //System.out.println("Nao, entao o codigo eh: " + dicionario.get(ant));
                res.add(dicionario.get(ant));
                //System.out.println(att);
                dicionario.put(att, prox++);
                ant = new ByteSequence(b);
            }
        }
        if(!ant.isEmpty()){
            res.add(dicionario.get(ant));
        }
        return res;
    }
    public static ByteSequence decodificandoCodigo(HashMap<Integer, ByteSequence> dicionario, ArrayList<Integer> codigo){
        int prox = 256;
        int primeiroCodigo = codigo.get(0);
        ByteSequence anterior = new ByteSequence(dicionario.get(primeiroCodigo));
        ByteSequence res = new ByteSequence(anterior);
        for(int i = 1; i < codigo.size(); i++){
            int codigoAtual = codigo.get(i);
            ByteSequence entrada;
            if(dicionario.containsKey(codigoAtual)) 
                entrada = dicionario.get(codigoAtual);
            else entrada = new ByteSequence(anterior, anterior.getData()[0]);
            ByteSequence novo = new ByteSequence(anterior, entrada.getData()[0]);
            res = new ByteSequence(res, entrada);
            dicionario.put(prox++, novo);
            anterior = entrada;
        }
        return res;
    }
    public static void encode(byte[] bytes, int index) throws IOException{
        HashMap<ByteSequence, Integer> dicionarioCodificacao = new HashMap<>();
        startingEncodingDictionary(dicionarioCodificacao);
        ArrayList<Integer> res = gerandoCodigo(dicionarioCodificacao, bytes);
        DataOutputStream dos = new DataOutputStream(new FileOutputStream("snapshots/binary_dbLZWCompressao" + index + ".db"));
        int tamanho = dicionarioCodificacao.size();
        int bits = (int)Math.ceil(Math.log(tamanho) / Math.log(2));
        dos.writeByte((byte)bits);
        writeEncoded(dos, res, bits);
        dos.close();
    }
    public static byte[] decode(int version) throws IOException {
        DataInputStream dis = new DataInputStream(new FileInputStream("snapshots/binary_dbLZWCompressao" + version + ".db"));
        int bitLength = (int)dis.readByte();
        //System.out.println("bitlenght: " + bitLength);
        ArrayList<Integer> codigo = new ArrayList<>();
        readEncoded(dis, codigo, bitLength);
        //System.out.println("Codigo: " + codigo);
        HashMap<Integer, ByteSequence> dicionarioDecodificacao = new HashMap<>();
        startingDecodingDictionary(dicionarioDecodificacao);
        ByteSequence res = decodificandoCodigo(dicionarioDecodificacao, codigo);
        byte[] array = res.getData();
        return array;
    }
    public static void writeEncoded(DataOutputStream dos, ArrayList<Integer> codigo, int bitLength) throws IOException {
        int bitBuffer = 0;
        int bitCount = 0;
        for (Integer valor : codigo) {
            bitBuffer = (bitBuffer << bitLength) | valor;
            bitCount += bitLength;
            while (bitCount >= 8) {
                bitCount -= 8;
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
