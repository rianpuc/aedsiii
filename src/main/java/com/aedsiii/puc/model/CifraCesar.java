package com.aedsiii.puc.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class CifraCesar {
    private static final int ASCII_BASE = 32;
    private static final int ASCII_LIMITE = 126;
    private static final int TAMANHO_ALFABETO = ASCII_LIMITE - ASCII_BASE + 1;
    private static final int TAMANHO_ALFABETO_BYTE = 256;

    /**
     * Função central que processa um único byte.
     * Aplica o shift somente se o byte estiver no range desejado.
     *
     * @param b     O byte a ser processado.
     * @param shift O deslocamento a ser aplicado.
     * @return O byte processado (cifrado ou original).
     */
    private static byte processarByte(byte b, int shift) {
        // Converte o byte para um int de 0-255 para facilitar as comparações
        int valorOriginal = b & 0xFF;
        // VERIFICAÇÃO CONDICIONAL: só mexe se estiver no range imprimível
        if (valorOriginal >= ASCII_BASE && valorOriginal <= ASCII_LIMITE) {
            // Lógica de César Generalizada que já vimos:
            // 1. Normaliza para um índice de 0 a 94
            int indice = valorOriginal - ASCII_BASE;
            // 2. Aplica o shift e o módulo com o tamanho do nosso alfabeto (95)
            int novoIndice = (indice + shift) % TAMANHO_ALFABETO;
            // 3. Corrige para o caso de resultados negativos
            if (novoIndice < 0) {
                novoIndice += TAMANHO_ALFABETO;
            }
            // 4. Converte o índice de volta para o valor ASCII correto e depois para byte
            return (byte) (ASCII_BASE + novoIndice);
        } else {
            // Se o byte está fora do range, retorna ele mesmo, sem modificação.
            return b;
        }
    }
    public static byte[] cifrar(byte[] dadosBytes, int shift) {
        byte[] resultado = new byte[dadosBytes.length];
        for (int i = 0; i < dadosBytes.length; i++) {
            resultado[i] = processarByte(dadosBytes[i], shift);
        }
        return resultado;
    }

    public static void cifrarArquivo(String pathArquivo, int shift) {
        Path originalPath = Path.of(pathArquivo);
        // Cria um caminho para um arquivo temporário na mesma pasta
        Path tempPath = Path.of(pathArquivo + ".tmp");
        try {
            // 1. Ler todos os bytes do arquivo original
            byte[] bytesOriginais = Files.readAllBytes(originalPath);
            // 2. Chamar a função de cifragem que opera em bytes
            byte[] bytesCifrados = cifrar(bytesOriginais, shift);
            // 3. Escrever os bytes cifrados no arquivo temporário
            Files.write(tempPath, bytesCifrados);
            // 4. Substituir o arquivo original pelo temporário.
            // A opção REPLACE_EXISTING garante que ele sobrescreva se o original existir.
            Files.move(tempPath, originalPath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Arquivo '" + pathArquivo + "' cifrado com sucesso!");
        } catch (IOException e) {
            System.err.println("Ocorreu um erro ao cifrar o arquivo: " + e.getMessage());
            // Se algo deu errado, é uma boa prática tentar deletar o arquivo temporário
            try {
                Files.deleteIfExists(tempPath);
            } catch (IOException cleanupException) {
                System.err.println("Erro adicional ao limpar arquivo temporário: " + cleanupException.getMessage());
            }
        }
    }

    public static byte[] decifrarArquivo(String pathArquivo, int shift){
        Path originalPath = Path.of(pathArquivo);
        // Cria um caminho para um arquivo temporário na mesma pasta
        try {
            byte[] bytesOriginais = Files.readAllBytes(originalPath);
            byte[] bytesDecifrados = decifrar(bytesOriginais, shift);
            return bytesDecifrados;
        } catch (IOException e) {
            System.err.println("Ocorreu um erro ao decifrar o arquivo: " + e.getMessage());
        }
        return null;
    }

    /**
     * Decifra um array de bytes, modificando apenas os bytes no range [32, 126].
     * Simplesmente chama a função de cifragem com o shift negativo.
     */
    public static byte[] decifrar(byte[] arquivoCifrado, int shift) {
        // Decifrar é o mesmo que cifrar com o deslocamento inverso.
        return cifrar(arquivoCifrado, -shift);
    }
}
