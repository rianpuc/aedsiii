package com.aedsiii.puc.app;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

/*
 * Classe para abrir uma interface de seleção de arquivo
 */
public class FileChooser {
    public static String getCSVPath() {
        String filepath = "";
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Arquivo CSV (.csv)", "csv");
        chooser.setFileFilter(filter);
        chooser.setCurrentDirectory(new File("."));
        int result = chooser.showOpenDialog(chooser);
        if(result == JFileChooser.APPROVE_OPTION){
            File selectedFile = chooser.getSelectedFile();
            filepath = selectedFile.getAbsolutePath();
        }
        return filepath;
    }
    public static String getBinPath() {
        String filepath = "";
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Arquivo Bin (.db)", "db");
        chooser.setFileFilter(filter);
        chooser.setCurrentDirectory(new File("."));
        int result = chooser.showOpenDialog(chooser);
        if(result == JFileChooser.APPROVE_OPTION){
            File selectedFile = chooser.getSelectedFile();
            filepath = selectedFile.getAbsolutePath();
        }
        return filepath;
    }
}
