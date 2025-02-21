package com.aedsiii.puc.app;

import java.util.ArrayList;

import com.aedsiii.puc.model.Job;

public class Main {
    public static void main(String[] args) {
        ArrayList<Job> jobs = FileParser.parseFile();
        PrimaryToSecondary.toSecondary(jobs);
    }
}