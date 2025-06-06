package com.aedsiii.puc.model;

import java.util.HashMap;
import java.util.Map;

public class BoyerMoore {

    /**
     * Pesquisa ocorrências de um padrão em um texto usando o algoritmo Boyer Moore.
     * 
     * @param padrao padrão a ser procurado
     * @param texto  texto a ser pesquisado
     * @return número de ocorrências do padrão no texto.
     */
    public static int search(String padrao, String texto) {
        int foundCount = 0;
        int m = padrao.length();
        int n = texto.length();

        // Cria tabela hash (DCR) e vetor de deslocamentos (DSB)
        Map<Character, Integer> hashCR = new HashMap<>();
        construirHash(padrao, m, hashCR);
        int[] vetorSB = new int[m];
        sufixoBom(padrao, m, vetorSB);

        int i = m - 1; // índice no texto
        while (i < n) {
            int j = m - 1; // índice no padrão

            // Compara do fim do padrão para o início
            while (j >= 0 && texto.charAt(i - (m - 1 - j)) == padrao.charAt(j)) {
                j--;
            }

            if (j < 0) {
                // Padrão encontrado
                foundCount++;
                i += vetorSB[0]; // Salta pelo sufixo bom para o próximo alinhamento
            } else {
                // Caracter ruim
                char c = texto.charAt(i - (m - 1 - j));
                int saltoCR = hashCR.containsKey(c) ? Math.max(1, j - hashCR.get(c)) : j + 1;
                int saltoSB = vetorSB[j];
                // Faz o melhor salto entre CR e SB
                i += Math.max(saltoCR, saltoSB);
            }
        }

        return foundCount;
    }

    /**
     * Constrói a tabela hash para cálculo dos deslocamentos por caracter ruim
     * 
     * @param padrao padrão a ser calculado
     * @param m      length do padrão
     * @param hash   tabela a receber a construção
     */
    private static void construirHash(String padrao, int m, Map<Character, Integer> hash) {
        // Ultimo caracter é ignorado
        int i = m - 2;
        while (i > -1) {
            hash.putIfAbsent(padrao.charAt(i), i);
            i--;
        }
    }

    /**
     * Constrói o vetor para cálculo dos deslocamentos por sufixo bom
     * 
     * @param padrao padrão a ser calculado
     * @param m      length do padrão
     * @param arr    array a receber os cálculos dos deslocamentos
     */
    private static void sufixoBom(String padrao, int m, int arr[]) {
        // Ultimo caracter é sempre 1
        arr[m - 1] = 1;
        int i = m - 1;
        int j = m - 2;
        while (j > -1) {
            if (i > 0) {
                // String sufixoBom = padrao.substring(i, m-1);
                String sufixoBom = padrao.substring(i, m);
                char precedente = padrao.charAt(i - 1);
                // String searchString = padrao.substring(0, i);
                int lastIndex = encontrarSufixoBom(sufixoBom, padrao, precedente);

                // Caso 1: sufixo bom existe precedido por caracter diferente
                if (lastIndex != -1 && lastIndex != i && lastIndex != 0 && padrao.charAt(lastIndex - 1) != precedente) {
                    arr[j] = i - lastIndex;
                    // Caso 2: sufixo bom existe no prefixo
                } else if (padrao.indexOf(sufixoBom) == 0) {
                    arr[j] = i;
                } else {
                    // Vai diminuindo o tamanho do sufixo até ver se encaixa no prefixo
                    boolean found = false;
                    while (sufixoBom.length() > 1 && found == false) {
                        sufixoBom = sufixoBom.substring(1, sufixoBom.length());
                        if (padrao.indexOf(sufixoBom) == 0) {
                            arr[j] = padrao.lastIndexOf(sufixoBom);
                            found = true;
                        }
                    }
                    // Caso 3: sufixo bom não existe no prefixo
                    if (found == false) {
                        arr[j] = m;
                    }
                }
            }
            i--;
            j--;
        }
    }

    /**
     * Encontra o index do sufixo bom no padrão
     * 
     * @param sufixo           sufixo a ser pesquisado no padrão
     * @param padrao           padrão sendo calculado
     * @param caracterProibido caracter anterior ao sufixo atual
     * @return index de ocorrência do sufixo sem o caracter ruim como precedente
     */
    private static int encontrarSufixoBom(String sufixo, String padrao, char caracterProibido) {
        int n = padrao.length();
        int m = sufixo.length();

        // Procura da direita para a esquerda, ignorando a primeira (mais à direita)
        // boolean pulouPrimeira = false;

        for (int i = n - m; i >= 0; i--) {
            if (padrao.substring(i, i + m).equals(sufixo)) {
                // Pula a primeira aparição (mais à direita)
                // if (!pulouPrimeira) {
                // pulouPrimeira = true;
                // continue;
                // }

                // Verifica se o caractere precedente é diferente do proibido
                if (i > 0 && padrao.charAt(i - 1) == caracterProibido) {
                    continue;
                }
                return i;
            }
        }
        return -1;
    }
}
