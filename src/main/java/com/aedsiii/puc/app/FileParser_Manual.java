// package com.aedsiii.puc.app;
// import java.io.File;
// import java.util.Scanner;

// public class FileParser1 {
//     public static void parseFile(){
//         try{
//             String arquivoPath = FileChooser.getFilePath();
//             File arquivo = new File(arquivoPath);
//             Scanner sc = new Scanner(arquivo, "UTF-8");
//             sc.nextLine();
//             while(sc.hasNextLine()){
//                 boolean ratingHasFound = false;
//                 String linha = sc.nextLine().trim();
//                 linha = linha.substring(1);
//                 String[] campos = linha.split(",", 4); //pegando os 3 primeiros campos pois sempre haverao esses
//                 // DECLARACAO DAS VARIAVEIS //
//                 String show_id = campos[0]; 
//                 String type = campos[1]; 
//                 String title = campos[2];
//                 String diretor = "";
//                 String cast = "";
//                 String country = "";
//                 String data = "";
//                 String rating = "";
//                 String duration = "";
//                 String listed_in = "";
//                 String description = ""; //pegando a description pois ela sempre eh a ultima coisa
//                 linha = linha.substring(campos[0].length() + campos[1].length() + campos[2].length() + 3); //fazendo uma nova string com o tamanho dos 3 primeiros campos + as virgulas, para remove-las
//                 int inicio = linha.indexOf("\"");
//                 int fim = linha.indexOf("\"", inicio+1)+1; //tentando pegar a posicao dos diretores
//                 if(linha.charAt(0) == '"'){ //verificando se o primeiro caracter eh um ", caso for, estamos lidando com varios diretores
//                     diretor = linha.substring(inicio, fim);
//                     linha = linha.replace(diretor, "");
//                     linha = linha.substring(1);
//                 } else if(linha.charAt(0) != ','){ //caso for algo diferente de uma virgula, logo eh um caracter, entao temos somente um diretor
//                     campos = linha.split(",", 2);
//                     diretor = campos[0];
//                     linha = linha.substring(campos[0].length()+1);
//                 } else { //se nao for nada, nao ha diretores, entao eu removo-a
//                     linha = linha.substring(1);
//                 }
//                 if(linha.charAt(0) == '"'){ //verifico se ha um ", caso houver quer dizer que estamos nos casts agora
//                     cast = linha.substring(0, linha.indexOf("\"", 1)+1);
//                     linha = linha.replace(cast, "");
//                     linha = linha.substring(1);
//                 } else if(linha.charAt(0) != ','){ //verifico se eh diferente de virgula, pois pode-se tratar de um cast de somente uma pessoa
//                     campos = linha.split(",", 2);
//                     cast = campos[0];
//                     linha = linha.substring(campos[0].length()+1);
//                 } else { //caso nao tiver nada, nao existe cast
//                     linha = linha.substring(1);
//                 }
//                 if(linha.charAt(0) == '"'){ //verifico se existe ", para saber se estamos lidando com varios paises
//                     country = linha.substring(0, linha.indexOf("\"", 1)+1);
//                     linha = linha.replace(country, "");
//                     linha = linha.substring(1);
//                 } else if(linha.charAt(0) != ','){ //verifico se eh diferente de , para saber se eh somente um pais
//                     campos = linha.split(",", 2);
//                     country = campos[0];
//                     linha = linha.substring(campos[0].length()+1);
//                 } else { //se for uma virgula, nao ha pais
//                     linha = linha.substring(1);
//                 }
//                 if(linha.charAt(0) == '"'){ //verifico se existe ", para saber se estamos lidando com varios paises
//                 data = linha.substring(0, linha.indexOf("\"", 1)+1);
//                 linha = linha.replace(data, "");
//                 linha = linha.substring(1);
//                 }else if(linha.charAt(0) != ','){ //verifico se eh diferente de , para saber se existe uma data
//                     campos = linha.split(",", 2);
//                     data = campos[0];
//                     linha = linha.substring(campos[0].length()+1);
//                 } else { //se for uma virgula, nao ha pais
//                     linha = linha.substring(1);
//                 }
//                 campos = linha.split(",", 2); //pegando o release_year que sempre existe, porem nao ha " para diferenciar
//                 String release_year = campos[0];
//                 linha = linha.substring(campos[0].length()+1); //fazendo uma string nova com o tamanho do release_year e a virgula
//                 if(linha.charAt(0) != ','){ //vendo se o primeiro caracter nao eh uma virgula, creio q nao precisa tambem pq ja sai removendo tudo mas ta funcionando e nao mexo
//                     campos = linha.split(",", 2); //separando para pegar
//                     rating = campos[0]; //aqui salvo como "possivel" rating, pois pode nao haver rating
//                     if(rating.contains("min")){ //verifico se tem "min" nessa string, pq se nao, eh bug do banco de dados e preciso passar ele pra variavel correta
//                         duration = rating;
//                         rating = "";
//                         linha = linha.substring(campos[0].length()+2);
//                     } else { //caso contrario, achamos o rating com sucesso
//                         linha = linha.substring(campos[0].length()+1);
//                         ratingHasFound = true;
//                     }
//                 }
//                 if(ratingHasFound){ //isso e so para lidar quando ahcamos o rating, entao pegamos o duration que eh oq vem dps
//                     campos = linha.split(",", 2);
//                     duration = campos[0];
//                     linha = linha.substring(campos[0].length()+1);
//                 }
//                 if(linha.charAt(0) == '"'){ //verificando se estamos lidando ocm varios "listed_in" ou somente um
//                     listed_in = linha.substring(0, linha.indexOf("\"", 1)+1);
//                     linha = linha.substring(0);
//                 } else if(linha.charAt(0) != ','){ //isso eh quando so tem 1 listed_in
//                     campos = linha.split(",", 2);
//                     listed_in = campos[0];
//                     linha = linha.substring(campos[0].length()+1);
//                 }
//                 linha = linha.replace(listed_in, "");
//                 linha = linha.substring(1);
//                 description = linha;
//                 linha = linha.replace(description, "");
//                 if(data.length() <= 20 && data.length() >= 13){
//                     System.out.printf("%s\nShow_ID: %s\nType: %s\nTitle: %s\nDiretor: %s\nCast: %s\nCountry: %s\nData: %s\nRelease Year: %s\nRating: %s\nDuration: %s\nListed in: %s\nDescription: %s\n\n\n", linha, show_id, type, title, diretor, cast, country, data, release_year, rating, duration, listed_in, description);
//                     Data ddd = new Data(data);
//                 }
//                 System.out.printf("%s\nShow_ID: %s\nType: %s\nTitle: %s\nDiretor: %s\nCast: %s\nCountry: %s\nData: %s\nRelease Year: %s\nRating: %s\nDuration: %s\nListed in: %s\nDescription: %s\n\n\n", linha, show_id, type, title, diretor, cast, country, data, release_year, rating, duration, listed_in, description);
//             }
//             sc.close();
//         } catch (Exception e){
//             System.err.println("Erro no FileParser.java: " + e);
//         }
//     }
// }
