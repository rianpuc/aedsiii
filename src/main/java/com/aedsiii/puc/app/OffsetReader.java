package com.aedsiii.puc.app;

import java.io.IOException;
import java.io.RandomAccessFile;

import com.aedsiii.puc.model.Job;

/**
 * Classe para recuperar um registro a partir de um offset
 * @param offset endereço do registro no dataset
 * @param datasetPath local de armazenamento do arquivo dataset
 */
public class OffsetReader {
    public static Job readInOffset(long offset, String datasetPath) {
        Job job = new Job();
        try {
            RandomAccessFile raf = new RandomAccessFile(datasetPath, "rw");
            raf.seek(offset);

            raf.readByte(); // lápide
            int recordSize = raf.readInt();
            short jobId = raf.readShort();
            byte[] data = new byte[recordSize - 3];
            raf.readFully(data);
            job = SecondaryToPrimary.deserializeJob(data);
            job.setJob_id(jobId);
 
            raf.close();
        } catch (IOException e) {
            System.err.println("Erro no OffsetReader.java, readInOffset: " + e);
        }
        return job;
    }
}
