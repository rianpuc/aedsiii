package com.aedsiii.puc.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class Rsa {
    /**
     * Cifra um arquivo usando o RsaManual, processando um byte de cada vez.
     *
     * @param path O caminho do arquivo a ser cifrado.
     * @param p    O primeiro primo para gerar a chave.
     * @param q    O segundo primo para gerar a chave.
     */
    public static void cifrarArquivoRsaManualEmBlocos(String path, BigInteger p, BigInteger q) throws Exception {
        Path originalPath = Path.of(path);
        Path tempPath = Path.of(path + ".tmp");

        CifraRsa rsa = new CifraRsa(p, q);
        byte[] bytesOriginais = Files.readAllBytes(originalPath);

        // Usamos um ByteArrayOutputStream para construir o arquivo cifrado em memória.
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {

            // Loop para processar cada byte do arquivo original
            for (byte b : bytesOriginais) {
                // Cifra um único byte
                // Para isso, colocamos o byte em um array de tamanho 1
                byte[] blocoCifrado = rsa.cifrar(new byte[]{b});

                // Escreve o tamanho do bloco cifrado (como um short, 2 bytes)
                dos.writeShort(blocoCifrado.length);
                // Escreve o bloco cifrado em si
                dos.write(blocoCifrado);
            }
            
            // Pega o resultado final do buffer de memória
            byte[] arquivoCifradoFinal = baos.toByteArray();

            // Salva o arquivo cifrado de forma segura
            Files.write(tempPath, arquivoCifradoFinal);
            Files.move(tempPath, originalPath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Arquivo cifrado em blocos com RSA Manual com sucesso!");
        }
    }

    /**
     * Decifra um arquivo que foi cifrado com o método de blocos do RsaManual.
     *
     * @param path O caminho do arquivo a ser decifrado.
     * @param p    O primeiro primo para gerar a chave.
     * @param q    O segundo primo para gerar a chave.
     * @return O array de bytes com os dados decifrados.
     */
    public static byte[] decifrarArquivoRsaManualEmBlocos(String path, BigInteger p, BigInteger q) throws Exception {
        CifraRsa rsa = new CifraRsa(p, q);
        byte[] bytesCifrados = Files.readAllBytes(Path.of(path));

        // Usa um ByteArrayOutputStream para construir o arquivo decifrado em memória
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytesCifrados);
                DataInputStream dis = new DataInputStream(bais);
                ByteArrayOutputStream baosResultado = new ByteArrayOutputStream()) {
            
            // Enquanto houver dados para ler no fluxo de bytes cifrados
            while (dis.available() > 0) {
                // 1. Lê o tamanho do próximo bloco (é um short, 2 bytes)
                short tamanhoBloco = dis.readShort();
                
                // 2. Lê os bytes exatos daquele bloco
                byte[] blocoCifrado = new byte[tamanhoBloco];
                dis.readFully(blocoCifrado);

                // 3. Decifra o bloco para obter o byte original
                byte[] blocoDecifrado = rsa.decifrar(blocoCifrado);

                // 4. Escreve o resultado (o byte original) no nosso buffer de saída
                baosResultado.write(blocoDecifrado);
            }

            // Retorna o buffer de saída como um array de bytes completo
            return baosResultado.toByteArray();
        }
    }
    public static byte[] decifrarArquivoRsaManualEmBlocos(byte[] bytesCifrados, BigInteger p, BigInteger q) throws Exception {
        CifraRsa rsa = new CifraRsa(p, q);

        // Usa um ByteArrayOutputStream para construir o arquivo decifrado em memória
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytesCifrados);
                DataInputStream dis = new DataInputStream(bais);
                ByteArrayOutputStream baosResultado = new ByteArrayOutputStream()) {
            
            // Enquanto houver dados para ler no fluxo de bytes cifrados
            while (dis.available() > 0) {
                // 1. Lê o tamanho do próximo bloco (é um short, 2 bytes)
                short tamanhoBloco = dis.readShort();
                
                // 2. Lê os bytes exatos daquele bloco
                byte[] blocoCifrado = new byte[tamanhoBloco];
                dis.readFully(blocoCifrado);

                // 3. Decifra o bloco para obter o byte original
                byte[] blocoDecifrado = rsa.decifrar(blocoCifrado);

                // 4. Escreve o resultado (o byte original) no nosso buffer de saída
                baosResultado.write(blocoDecifrado);
            }

            // Retorna o buffer de saída como um array de bytes completo
            return baosResultado.toByteArray();
        }
    }
}
