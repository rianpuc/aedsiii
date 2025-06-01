package com.aedsiii.puc.model;
import java.io.DataInputStream;
import java.io.IOException;

public class LeitorDeBits {
    private DataInputStream dis;
    private int currentByte;   // O byte que lemos do arquivo e estamos processando
    private int nextBitIndex;  // O índice do próximo bit a ser lido (de 7 a 0)

    public LeitorDeBits(DataInputStream dis) throws IOException {
        this.dis = dis;
        this.currentByte = 0;
        this.nextBitIndex = -1; // -1 força a leitura do primeiro byte na primeira chamada
    }

    public int lerProximoBit() throws IOException {
        // Se já lemos todos os bits do byte atual, precisamos ler o próximo do arquivo
        if (nextBitIndex < 0) {
            currentByte = dis.readUnsignedByte(); // Lê o próximo byte (0 a 255)
            nextBitIndex = 7; // Começa a ler do bit mais significativo (o da esquerda)
        }

        // Usa mágica de bits para pegar o bit na posição 'nextBitIndex'
        // Ex: byte 10110010, nextBitIndex = 7 -> (byte >> 7) & 1 -> pega o '1' da esquerda
        int bit = (currentByte >> nextBitIndex) & 1;
        nextBitIndex--; // Move o índice para o próximo bit à direita
        return bit;
    }
}