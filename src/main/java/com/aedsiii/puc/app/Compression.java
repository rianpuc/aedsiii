package com.aedsiii.puc.app;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.aedsiii.puc.model.Huffman;
import com.aedsiii.puc.model.LZW;

public abstract class Compression {
    public static int getNextSnapshotIndex(String snapshotDir) {
        File dir = new File(snapshotDir);
        if (!dir.exists()) dir.mkdirs();
        File[] files = dir.listFiles();
        int max = 0;
        Pattern pattern = Pattern.compile("binary_dbHuffmanCompressao(\\d+)\\.db");
        for (File f : files) {
            Matcher matcher = pattern.matcher(f.getName());
            if (matcher.matches()) {
                int num = Integer.parseInt(matcher.group(1));
                max = Math.max(max, num);
            }
        }
        return max + 1;
    }
    public static void compress(String path) throws IOException{
        Path caminho = Paths.get(path);
        byte[] db_bytes = Files.readAllBytes(caminho);
        int nextIndex = getNextSnapshotIndex("snapshots");
        double originalSize = db_bytes.length;
        // Huffman
        long start = System.nanoTime();
        Huffman.encode(db_bytes, nextIndex);
        long end = System.nanoTime();
        double elapsedHuff = (end - start) / 1_000_000.0;
        Path huffPath = Paths.get("snapshots/binary_dbHuffmanCompressao" + nextIndex + ".db");
        byte[] huffBytes = Files.readAllBytes(huffPath);
        double huffSize = huffBytes.length;
        double huffRatio = originalSize / huffSize;
        double huffGain = (1 - (huffSize / originalSize)) * 100;
        // LZW
        start = System.nanoTime();
        LZW.encode(db_bytes, nextIndex);
        end = System.nanoTime();
        double elapsedLZW = (end - start) / 1_000_000.0;
        Path lzwPath = Paths.get("snapshots/binary_dbLZWCompressao" + nextIndex + ".db");
        byte[] lzwBytes = Files.readAllBytes(lzwPath);
        double lzwSize = lzwBytes.length;
        double lzwRatio = originalSize / lzwSize;
        double lzwGain = (1 - (lzwSize / originalSize)) * 100;
        // Resultados
        System.out.printf("Compressão #%d concluída.\n", nextIndex);
        System.out.println("Resultados:");
        System.out.printf("\n[Huffman]\nTempo: %.2f ms\nTamanho original: %.0f bytes\nTamanho comprimido: %.0f bytes\nGanho: %.2f%%\nRatio: %.2f\n",
            elapsedHuff, originalSize, huffSize, huffGain, huffRatio);
        System.out.printf("\n[LZW]\nTempo: %.2f ms\nTamanho original: %.0f bytes\nTamanho comprimido: %.0f bytes\nGanho: %.2f%%\nRatio: %.2f\n",
            elapsedLZW, originalSize, lzwSize, lzwGain, lzwRatio);
        System.out.println("\nComparativo de desempenho:");
        if (huffGain > lzwGain) {
            System.out.printf("- Melhor compressão: Huffman (%.2f%% vs %.2f%%)\n", huffGain, lzwGain);
        } else if (lzwGain > huffGain) {
            System.out.printf("- Melhor compressão: LZW (%.2f%% vs %.2f%%)\n", lzwGain, huffGain);
        } else {
            System.out.println("- Compressão equivalente.");
        }
        if (elapsedHuff < elapsedLZW) {
            System.out.printf("- Mais rápido: Huffman (%.2f ms vs %.2f ms)\n", elapsedHuff, elapsedLZW);
        } else if (elapsedLZW < elapsedHuff) {
            System.out.printf("- Mais rápido: LZW (%.2f ms vs %.2f ms)\n", elapsedLZW, elapsedHuff);
        } else {
            System.out.println("- Velocidade equivalente.");
        }
        System.out.println("\n");
    }
    public static void decompress(int version) throws IOException{
        byte[] huffDecoded, lzwDecoded;
        long start, end;
        double huffTime, lzwTime;

        // Descompressão Huffman
        start = System.nanoTime();
        huffDecoded = Huffman.decode(version);
        end = System.nanoTime();
        huffTime = (end - start) / 1_000_000.0;

        // Descompressão LZW
        start = System.nanoTime();
        lzwDecoded = LZW.decode(version);
        end = System.nanoTime();
        lzwTime = (end - start) / 1_000_000.0;

        // Cria arquivos temporários dos descomprimidos
        Path huffDecodedPath = Paths.get("decompressedHuffman" + version + ".db");
        Files.write(huffDecodedPath, huffDecoded);

        Path lzwDecodedPath = Paths.get("decompressedLZW" + version + ".db");
        Files.write(lzwDecodedPath, lzwDecoded);

        // Mostra resultados
        System.out.println("\n[Huffman]");
        System.out.printf("Tempo de descompressão: %.2f ms\n", huffTime);
        System.out.println("\n[LZW]");
        System.out.printf("Tempo de descompressão: %.2f ms\n", lzwTime);

        // Escolher o melhor e sobrescrever banco original
        Path finalDbPath = Paths.get("binary_db.db");
        if (huffTime < lzwTime) {
            Files.copy(huffDecodedPath, finalDbPath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("\nHuffman foi mais rápido. Banco de dados substituído por versão Huffman.");
        } else if (lzwTime < huffTime) {
            Files.copy(lzwDecodedPath, finalDbPath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("\nLZW foi mais rápido. Banco de dados substituído por versão LZW.");
        } else {
            Files.copy(huffDecodedPath, finalDbPath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("\nTempos iguais. Huffman escolhido por padrão.");
        }
        Files.deleteIfExists(huffDecodedPath);
        Files.deleteIfExists(lzwDecodedPath);
        System.gc();
    }
}
