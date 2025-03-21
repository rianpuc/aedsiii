package com.aedsiii.puc.app;
import java.io.FileReader;
import java.io.IOException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.List;
import java.util.ArrayList;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.aedsiii.puc.model.Job;

public class FileParser {

    /*
     * Transformação de dados em CSV para lista de objeto
     */
    public static ArrayList<Job> parseFile(){
        ArrayList<Job> jobs = new ArrayList<Job>();
        try{
            String arquivoPath = FileChooser.getCSVPath();
            try (CSVReader reader = new CSVReader(new FileReader(arquivoPath))) {
                List<String[]> records = reader.readAll();
                records.remove(0); // Remoção da linha de cabeçalho
                //int count = 0; // PRA DEBUGAR
                for (String[] row : records) {
                    //if (count >= 1) break; // PRA DEBUGAR
                    Job job = parseJob(row);
                    jobs.add(job);
                    //count++; PRA DEBUGAR
                }
            } catch (IOException | CsvException e) {
                e.printStackTrace();
            }
        } catch (Exception e){
            System.err.println("Erro no FileParser.java: " + e);
        }
        return jobs;
    }

    /*
     * Transformar linha do arquivo CSV em objeto Job
     */
    private static Job parseJob(String[] row){
        Job job = new Job();
        job.setJob_id(Short.parseShort(row[0]));
        job.setExperience(row[1]);
        job.setQualification(row[2]);
        job.setSalary_range(row[3]);
        job.setLocation(row[4]);
        job.setCountry(row[5]);
        job.setLatitude(Float.parseFloat(row[6]));
        job.setLongitude(Float.parseFloat(row[7]));
        job.setWork_type(row[8]);
        job.setCompany_size(Integer.parseInt(row[9]));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate localDate = LocalDate.parse(row[10], formatter);
        job.setJob_posting_date(localDate.atStartOfDay(ZoneOffset.UTC).toInstant());
        job.setPreference(row[11]);
        job.setContact_person(row[12]);
        job.setContact(row[13]);
        job.setJob_title(row[14]);
        job.setRole(row[15]);
        job.setJobPortal(row[16]);
        job.setJob_description(row[17]);
        job.setBenefits(parseList(row[18]));
        job.setSkills(parseList(row[19]));
        job.setResponsibilities(parseList(row[20]));
        job.setCompany(row[21]);

        // Usaremos apenas o website da lista em row[22]
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode jsonNode = objectMapper.readTree(row[22]);
            JsonNode websiteNode = jsonNode.get("Website");
            String website = (websiteNode != null && !websiteNode.asText().isBlank() ? websiteNode.asText() : "N/A"); //verificando se ha algo no campo website
            job.setCompany_profile(website);
        } catch (Exception e) {
            //e.printStackTrace();
            job.setCompany_profile("N/A"); // caso de erro seta pra uma string falando q n tem nada
        }
        
        return job;
    }

    /*
     * Função auxilizar a formação de uma lista
     */
    private static List<String> parseList(String raw) {
        String processed = raw.replaceAll("[{}\"]", ""); // Remover {} e " do campo que conter uma lista
        String[] parts = processed.split(","); // Pegando cada item usando "," como separador
        List<String> parsedList = Arrays.stream(parts) // Arrays.stream(parts) pra poder usar a stream API (pra usar o map e collect ali embaixo)
                                  .map(String::trim) // Dar trim() em cada item na stream pra tirar espaços em branco q tiverem sobrado
                                  .collect(Collectors.toList()); // Pegar os itens da stream e transformar de volta pra uma lista normal
        return parsedList;
    }
}