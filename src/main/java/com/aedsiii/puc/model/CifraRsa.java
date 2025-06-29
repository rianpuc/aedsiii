package com.aedsiii.puc.model;
import java.math.BigInteger;

public class CifraRsa {

    private final BigInteger n;
    private final BigInteger e; // Componente da chave pública
    private final BigInteger d; // Componente da chave privada

    /**
     * Construtor que gera as chaves RSA a partir de dois números primos.
     *
     * @param p O primeiro número primo.
     * @param q O segundo número primo.
     */
    public CifraRsa(BigInteger p, BigInteger q) {
        this.n = p.multiply(q);
        BigInteger phi = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE));
        this.e = new BigInteger("65537"); // Usando o padrão determinístico

        if (!phi.gcd(this.e).equals(BigInteger.ONE)) {
            throw new ArithmeticException("O expoente 'e' padrão (65537) não é coprimo com phi.");
        }
        this.d = this.e.modInverse(phi);
    }

    public byte[] cifrar(byte[] mensagemBytes) {
        // 1. Converte o array de bytes em um único número (positivo)
        BigInteger mensagemComoNumero = new BigInteger(1, mensagemBytes);

        // 2. Aplica a fórmula: C = M^e mod n
        BigInteger cifradoComoNumero = mensagemComoNumero.modPow(this.e, this.n);
        
        // 3. Converte o número gigante de volta para um array de bytes
        return cifradoComoNumero.toByteArray();
    }

    public byte[] decifrar(byte[] mensagemCifradaBytes) {
        // 1. Converte o array de bytes cifrado de volta para um número
        BigInteger cifradoComoNumero = new BigInteger(mensagemCifradaBytes);
        
        // 2. Aplica a fórmula: M = C^d mod n
        BigInteger decifradoComoNumero = cifradoComoNumero.modPow(this.d, this.n);

        // 3. Converte o número decifrado de volta para o array de bytes original
        return decifradoComoNumero.toByteArray();
    }
    
    // Getters para visualização
    public BigInteger getN() { return n; }
    public BigInteger getE() { return e; }
    public BigInteger getD() { return d; }
}