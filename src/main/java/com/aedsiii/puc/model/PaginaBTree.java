package com.aedsiii.puc.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Representa a página (nodo) em uma árvore B.
 * 
 * @param ordem máximo de filhos da página
 * @param max_children máximo de filhos por página
 * @param min_children mínimo de filhos por página (exceto raiz e folhas)
 * @param min_keys mínimo de chaves por página (exceto pra raiz)
 * @param max_keys máximo de chaves por página
 * @param keys lista de chaves na página
 * @param children lista de ponteiros para os filhos da página
 */
public class PaginaBTree {
    public int ordem;

    public int max_children;
    public int min_children;
    public int min_keys;
    public int max_keys;
    public int tamanho_key;
    public int tamanho_pagina;
    
    public ArrayList<RegistroBTree> keys;
    public ArrayList<Long> children;

    /**
     * Cria uma página de árvore B.
     * @param ordem máximo de filhos da página.
     */
    public PaginaBTree(int ordem) {
        this.ordem = ordem;

        this.max_children = this.ordem;
        this.min_children = (int) Math.ceil(ordem/2.0);
        
        this.min_keys = (int) Math.ceil(ordem/2.0) - 1;
        this.max_keys = ordem - 1;

        this.keys = new ArrayList<>(this.max_keys);
        this.children = new ArrayList<>(this.max_children);

        // Short + Long: job_id e offset no dataset
        this.tamanho_key = RegistroBTree.size();
        // Integer: Número de elementos na página
        // Long * ordem: Ponteiros para os filhos da página
        // tamanho_key * max_keys: Chaves na página
        this.tamanho_pagina = Integer.BYTES + (Long.BYTES * this.max_children) + (this.tamanho_key * this.max_keys);
    }

    /**
     * Transforma a página em um vetor de bytes.
     * @throws IOException
     */
    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream ba = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(ba);

        // Número de elementos na página
        dos.writeInt(this.keys.size());

        int i = 0;
        while (i < this.keys.size()) {
            dos.writeLong(this.children.get(i));
            dos.write(this.keys.get(i).toByteArray());
            i++;
        }

        // Último ponteiro da página
        if (this.children.size() > 0) {
            dos.writeLong(this.children.get(i));
        } else {
            dos.writeLong(-1L);
        }

        // Preencher a página com mais bytes se não estiver com o número máximo de elementos
        byte[] remainingSpace = new byte[this.tamanho_key];
        while (i < this.max_keys) {
            dos.write(remainingSpace);
            dos.writeLong(-1L);
            i++;
        }

        return ba.toByteArray();
    }

    /**
     * Deserializa um vetor de bytes para criar um objeto PaginaBTree.
     * @param buffer vetor de bytes para ser deserializado.
     * @throws IOException
     */
    public void fromByteArray(byte[] buffer) throws IOException {
        if (buffer.length != this.tamanho_pagina) {
            throw new IOException("Tamanho do buffer inválido. Esperado: " + this.tamanho_pagina + ", Recebido: " + buffer.length);
        }

        ByteArrayInputStream ba = new ByteArrayInputStream(buffer);
        DataInputStream dis = new DataInputStream(ba);

        // Número de elementos na página
        int num_keys = dis.readInt();

        this.keys = new ArrayList<>(this.max_keys);
        this.children = new ArrayList<>(this.max_children);

        int i = 0;
        while (i < num_keys) {
            RegistroBTree registro = new RegistroBTree();

            this.children.add(dis.readLong());
            byte[] ba_registro = new byte[tamanho_key];
            dis.readFully(ba_registro);
            registro.fromByteArray(ba_registro);

            this.keys.add(registro);
            i++;
        }
        // Adicionando próximo ponteiro da página, que não foi lido ainda
        // Ou seja, um ponteiro da direita.
        if (this.children.size() < this.max_children) {
            this.children.add(dis.readLong());
        }
        /*
         * A página tem tamanho fixo, então caso não esteja cheia, é preciso skippar os bytes
         * (this.max_keys - i): quantidade de elementos ainda não lidos
         * (this.tamanho_key + Long.BYTES): tamanho de um elemento + ponteiro para o filho
         */
        dis.skipBytes((this.max_keys - i) * (this.tamanho_key + Long.BYTES));
    }
}
