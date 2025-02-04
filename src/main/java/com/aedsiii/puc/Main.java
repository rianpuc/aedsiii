package com.aedsiii.puc;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello world!");
        String arquivo = FileChooser.getFilePath();
        System.out.println(arquivo);
    }
}