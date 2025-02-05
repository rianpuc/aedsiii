package com.aedsiii.puc.app;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

public class FileChooser {
    public static String getFilePath() {
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
}
