package com.aedsiii.puc.model;

public class KMP {
    /**
     * Pesquisa ocorrências de um padrão em um texto usando o algoritmo KMP.
     * @param padrao padrão a ser procurado
     * @param texto texto a ser pesquisado
     * @return true se o padrão for encontrado ao menos uma vez, senão false
     */
    public static int search(String padrao, String texto) {
        int foundCount = 0;
        int m = padrao.length();
        int n = texto.length();

        // Vetor para as transições de falha
        int[] transicoesDeFalha = new int[m];
        int j = 0;

        // Constrói o vetor para transições de falha
        criarVetorDeFalhas(padrao, m, transicoesDeFalha);

        int i = 0;
        while (i < n) {
            // Símbolo coincidente
            if (padrao.charAt(j) == texto.charAt(i)) {
                j++;
                i++;
            }
            // Se j == m, então todos os símbolos do padrão foram encontrados em sequência no texto
            if (j == m) {
                System.out.print("\rPadrão encontrado. Index: " + (i-j) + "          ");
                foundCount++;
                // Voltar uma posição para verificar se o caracter no fim do padrão pode ser o início de outro
                j = transicoesDeFalha[j-1];
            }
            // Símbolo não coincidente
            else if (i < n && padrao.charAt(j) != texto.charAt(i)) {
                // Se não estiver no estado inicial, volta uma posição para tentar avanço de estado novamente
                if (j != 0) {
                    j = transicoesDeFalha[j - 1];
                } else {
                    i++;
                }
            }
        }
        return foundCount;
    }

    /**
     * Função auxiliar para criar um vetor de transições de falhas no algoritmo KMP.
     * @param padrao padrão que terá o vetor de falhas criado
     * @param m tamanho do padrão
     * @param arr vetor de armazenamento das transições de falhas que será criado
     * 
     */
    private static void criarVetorDeFalhas(String padrao, int m, int arr[]) {
        int len = 0;
        int i = 1;
        // Primeira posição é sempre 0
        arr[0] = 0;

        while (i < m) {
            // Se o caracter atual estiver no prefixo, aumentar contagem em 1 e inserir no vetor de falhas
            if (padrao.charAt(i) == padrao.charAt(len)) {
                len++;
                arr[i] = len;
                i++;
            }
            else {
                // Se um caracter não fizer parte do prefixo atual, a contagem não volta imediatamente pra zero
                // i não aumenta, para que o caracter atual possa ser testado novamente
                if (len != 0) {
                    len = arr[len-1];
                }
                // Se o caracter atual não estiver no prefixo, colocar 0
                else {
                    arr[i] = 0;
                    i++;
                }
            }
        }
    }
}
