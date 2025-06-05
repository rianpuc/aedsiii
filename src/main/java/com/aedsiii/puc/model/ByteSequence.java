package com.aedsiii.puc.model;
import java.util.Arrays;

public final class ByteSequence {
    private final byte[] data;

    public ByteSequence(byte data){
        byte[] temp = new byte[1];
        temp[0] = data;
        this.data = temp;
    }

    public ByteSequence(byte[] data) {
        this.data = data;
    }

    public ByteSequence(ByteSequence data){
        this.data = new byte[data.data.length];
        System.arraycopy(data.data, 0, this.data, 0, data.data.length);
    }

    public ByteSequence(ByteSequence prefixo, byte sufixo) {
        this.data = new byte[prefixo.data.length + 1];
        System.arraycopy(prefixo.data, 0, this.data, 0, prefixo.data.length);
        this.data[this.data.length - 1] = sufixo;
    }

    public ByteSequence(ByteSequence prefixo, ByteSequence sufixo){
        this.data = new byte[prefixo.data.length + sufixo.data.length];
        System.arraycopy(prefixo.data, 0, this.data, 0, prefixo.data.length);
        System.arraycopy(sufixo.data, 0, this.data, prefixo.data.length, sufixo.data.length);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ByteSequence that = (ByteSequence) obj;
        return Arrays.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }

    // @Override
    // public String toString(){
    //     System.out.print("{ ");
    //     for(int i = 0; i < data.length; i++){
    //         System.out.print((byte)(data[i]) + " ");
    //     }
    //     System.out.print("}");
    //     return "";
    // }

    public boolean isEmpty(){
        return data.length <= 0;
    }

    public byte[] getData() {
        return data;
    }

}
