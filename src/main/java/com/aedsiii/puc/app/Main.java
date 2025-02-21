package com.aedsiii.puc.app;

import java.util.ArrayList;

import com.aedsiii.puc.model.Job;

public class Main {
    public static void main(String[] args) {
        ArrayList<Job> jobs = FileParser.parseFile();
        // for (Job job: jobs){
        //     job.mostrar();
        // }
        PrimaryToSecondary.toSecondary(jobs);
        String binaryPath = FileChooser.getBinPath();
        ArrayList<Job> bin_jobs = SecondaryToPrimary.toPrimary(binaryPath);
        for (Job job : bin_jobs) {
            job.mostrar();
        }
    }
}