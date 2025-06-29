package com.aedsiii.puc.model;
import java.math.BigInteger;

public class RsaUtils {

    /**
     * Recebe um número inicial e encontra o próximo número primo (ou o próprio número, se ele já for primo).
     * @param startNumber O número fornecido pelo usuário.
     * @return Um BigInteger que é um número primo.
     */
    public static BigInteger findNextPrime(String startNumber) {
        BigInteger p = new BigInteger(startNumber);

        // O método isProbablePrime() verifica se o número é provavelmente primo.
        // Uma "certeza" de 100 é mais que suficiente para qualquer propósito prático.
        if (p.isProbablePrime(100)) {
            return p; // O número digitado já é primo, então o retornamos.
        } else {
            // O método nextProbablePrime() retorna o primeiro primo que é maior que o número atual.
            System.out.println("O numero digitado nao e um primo, buscando o proximo.");
            return p.nextProbablePrime();
        }
    }
}