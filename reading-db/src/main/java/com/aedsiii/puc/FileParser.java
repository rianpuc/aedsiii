package com.aedsiii.puc;

import java.io.BufferedReader;
import java.io.FileReader;

public class FileParser {
    public static void parseFile(){
        try{
            String arquivoPath = FileChooser.getFilePath();
            BufferedReader arquivo = new BufferedReader(new FileReader(arquivoPath));
            String linha = arquivo.readLine();
            System.out.println(linha);
            arquivo.close();
        } catch (Exception e){
            System.err.println("Erro no FileParser.java: " + e);
        }
    }
}
