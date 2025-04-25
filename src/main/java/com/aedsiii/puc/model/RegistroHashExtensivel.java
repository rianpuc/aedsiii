package com.aedsiii.puc.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class RegistroHashExtensivel {
    public short id;
    public long offset;

    public RegistroHashExtensivel() {
        this.id = -1;
        this.offset = -1L;
    }

    public RegistroHashExtensivel(short id, long offset) {
        this.id = id;
        this.offset = offset;
    }

    /**
     * Retorna o tamanho em bytes de um registro fixo.
     * Atualmente o tamanho é sempre 10 bytes.
     */
    public static short size() {
        return Short.BYTES + Long.BYTES; // short + long = 2 + 8 = 10
    }

    /**
     * Transforma o registro em um vetor de bytes.
     * @throws IOException
     */
    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream ba = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(ba);

        dos.writeShort(this.id);
        dos.writeLong(this.offset);

        return ba.toByteArray();
    }

    /**
     * Deserializa um vetor de bytes para guardar um objeto RegistroBTree na variável usada para chamar este método.
     * @param ba vetor de bytes a ser deserializado.
     * @throws IOException
     */
    public void fromByteArray(byte[] ba) throws IOException {
        ByteArrayInputStream buffer = new ByteArrayInputStream(ba);
        DataInputStream dis = new DataInputStream(buffer);
        this.id = dis.readShort();
        this.offset = dis.readLong();
    }

    /**
     * Clona um registro de árvore B.
     */
    public RegistroHashExtensivel clone() {
        return new RegistroHashExtensivel(this.id, this.offset);
    }

    @Override
    public String toString() {
        String formattedOffset = String.format("%4s", offset).replace(' ', '.');
        return "{id=" + id + ", offset=" + formattedOffset + "}";
    }
}
