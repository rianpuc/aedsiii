package com.aedsiii.puc.app;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

import com.aedsiii.puc.model.PaginaBTree;
import com.aedsiii.puc.model.RegistroBTree;

public class BTree {
    private int ordem;
    private int max_keys;
    private int max_children;
    public PaginaBTree pagina;
    private RandomAccessFile BTreeFile;
    private String BTreeFilePath;

    // Variáveis de auxílio nas funções recursivas
    private RegistroBTree auxKey;
    private long auxPagina;
    private boolean cresceu;
    private boolean diminuiu;

    /**
     * Cria o arquivo da árvore B.
     * @param ordem máximo de filhos da página.
     * @param BTreeFilePath caminho de armazenamento do arquivo.
     * @param makeNewTree true para criar uma nova árvore, false senão.
     * @throws IOException
     */
    public BTree(int ordem, String BTreeFilePath, boolean makeNewTree) throws IOException {
        System.out.println("Criando arvore de ordem: " + ordem);
        this.ordem = ordem;
        this.max_children = this.ordem;
        this.max_keys = this.ordem - 1;

        this.BTreeFilePath = BTreeFilePath;
        
        this.BTreeFile = new RandomAccessFile(BTreeFilePath, "rw");

        if (makeNewTree) {
            BTreeFile.setLength(0);
        }
        
        // Arquivo vazio
        if (BTreeFile.length() == 0) {
            BTreeFile.seek(0);
            BTreeFile.writeInt(ordem); // Ordem
            BTreeFile.writeLong(-1L); // Endereço da raiz
        }
    }

    /**
     * Verifica se a árvore está vazia.
     * @return true se a árvore estiver vazia, senão false.
     * @throws IOException
     */
    public boolean empty() throws IOException {
        BTreeFile.seek(0);
        long ordemCheck = BTreeFile.readInt();
        return ordemCheck == -1;
    }

    /**
     * Procura um id no arquivo de árvore B.
     * @param id id a ser procurado.
     * @return Registro procurado, com seu id e offset no dataset.
     * @throws IOException
     */
    public RegistroBTree search(int id) throws IOException {
        long offsetRaiz;
        BTreeFile.seek(0);
        BTreeFile.readInt(); // ordem
        offsetRaiz = BTreeFile.readLong();

        // Raíz vazia
        if (offsetRaiz == -1) {
            RegistroBTree registro = new RegistroBTree();
            return registro;
        } else {
            return search(id, offsetRaiz);
        }
    }

    /**
     * Navega recursivamente pela árvore para encontrar um id.
     * @param id id a ser procurado.
     * @param pagina endereço da página a ser acessada.
     * @return Registro procurado, com seu id e offset no dataset.
     * @throws IOException
     */
    private RegistroBTree search(int id, long pagina) throws IOException {
        // ponteiro com valor -1 significa que não há mais para onde percorrer (chave não encontrada)
        if (pagina == -1) {
            return new RegistroBTree(); // possui id -1 e offset -1
        }

        // Move ponteiro para a página
        BTreeFile.seek(pagina);
        // Traz a página para memória primária
        PaginaBTree paginaBT = new PaginaBTree(ordem);
        byte[] buffer = new byte[paginaBT.tamanho_pagina];
        BTreeFile.read(buffer);
        paginaBT.fromByteArray(buffer);

        // Percorrer a página até achar uma key de id maior que a procurada ou não houver mais keys
        int i = 0;
        while (i < paginaBT.keys.size() && id > paginaBT.keys.get(i).id) {
            i++;
        }

        // Verificar se encontrou o id
        if (i < paginaBT.keys.size() && paginaBT.keys.get(i).id == id) {
            return paginaBT.keys.get(i);
        } 
        
        // Verificar se i não ultrapassou o número de ponteiros (mas acho q n vai acontecer)
        if (i < paginaBT.children.size()) {
            // Ir para a próxima página
            return search(id, paginaBT.children.get(i));
        }

        return new RegistroBTree();
    }

    /**
     * Função auxiliar:
     * Insere um registro na árvore B em arquivo.
     * @param registro registro a ser inserido na árvore B.
     * @return true se o registro for inserido com sucesso, senão false.
     * @throws IOException
     */
    public boolean create(RegistroBTree registro) throws IOException {
        BTreeFile.seek(0);
        // O primeiro valor no arquivo é o offset da página raiz.
        BTreeFile.readInt(); // ordem
        long offsetRaiz = BTreeFile.readLong();

        // Váriavel global: registros promovidos no caso de um split serão guardados nessa variável
        auxKey = registro.clone();

        // Variável global: ponteiro para página extra criada em um split
        auxPagina = -1;
        // Variável global: flag para indicar se será necessário criar uma nova página
        cresceu = false;

        boolean inserido = create(offsetRaiz);

        // Testa a necessidade de criação de uma nova raiz.
        if (cresceu) {
            // Nova página criada para ser a nova raiz.
            PaginaBTree newPag = new PaginaBTree(ordem);
            newPag.keys = new ArrayList<>(this.max_keys);
            newPag.keys.add(auxKey);
            newPag.children = new ArrayList<>(this.max_children);
            // O ponteiro da esquerda apontará para a raiz antiga
            newPag.children.add(offsetRaiz);
            // O ponteiro da direita apontará para a página extra criada pelo split
            newPag.children.add(auxPagina);

            // Escreve a nova raiz no fim do arquivo
            long end = BTreeFile.length();
            BTreeFile.seek(end);
            long novaRaiz = BTreeFile.getFilePointer();
            BTreeFile.write(newPag.toByteArray());
            // Reescreve no começo do arquivo o endereço da nova raiz
            BTreeFile.seek(4); // pulando ordem da arvore
            BTreeFile.writeLong(novaRaiz);
            inserido = true;
        }

        return inserido;
    }

    /**
     * Função auxiliar:
     * Navega recursivamente até a página correta de inserção, e insere o novo registro.
     * @param pagina endereço da página a ser acessada.
     * @return true se o novo registro for inserido com sucesso, senão false.
     * @throws IOException
     */
    private boolean create(long pagina) throws IOException {
        /* Se o endereço atual for -1, estamos em um dos filhos de uma folha. 
         * Ou seja, uma nova página deverá ser criada, o que vai aumentar a árvore.
        */
        if (pagina == -1) {
            cresceu = true;
            // Esse valor será alterado para guardar o endereço da nova página.
            auxPagina = -1;
            return false;
        }
        // Move ponteiro para a página
        BTreeFile.seek(pagina);
        // Traz a página para memória primária
        PaginaBTree paginaBT = new PaginaBTree(ordem);
        byte[] buffer = new byte[paginaBT.tamanho_pagina];
        BTreeFile.read(buffer);
        paginaBT.fromByteArray(buffer);

        int i = 0;
        // Percorrer a página até achar uma key de id maior que a que será inserida ou não houver mais keys
        while (i < paginaBT.keys.size() && auxKey.id > paginaBT.keys.get(i).id) {
            i++;
        }

        // Se encontrar um id igual, o elemento já está inserido
        if (i < paginaBT.keys.size() && paginaBT.keys.get(i).id == auxKey.id) {
            cresceu = false;
            return false;
        }

        // Ir para a próxima página
        boolean inserido = create(paginaBT.children.get(i));

        // Se a árvore não estiver em processo de crescimento, encerra.
        if (!cresceu) {
            return inserido;
        }

        // Se a página ainda não estiver cheia, insere a nova chave nela
        if (paginaBT.keys.size() < max_keys) {
            /*
             * Insere o novo elemento e seu ponteiro para a direita nos indexes indicados.
             * Se for necessário deslocamento na lista, o método add() faz isso automaticamente.
             */
            paginaBT.keys.add(i, auxKey);
            paginaBT.children.add(i+1, auxPagina);

            // Escreve a página atualizada no arquivo
            BTreeFile.seek(pagina);
            BTreeFile.write(paginaBT.toByteArray());

            // Encerra o processo de crescimento e retorna
            cresceu = false;
            return true;
        }

        /* Se a página estiver cheia, será feito um split.
         * newPag irá conter a metade da direita da página,
         * e esses valores serão eliminados da página atual para conter apenas os valores da esquerda.
         */
        PaginaBTree newPag = new PaginaBTree(ordem);
        int meio = max_keys / 2;
        newPag.children.add(paginaBT.children.get(meio));
        for (int j = 0; j < (max_keys - meio); j++) {
            // os elementos são puxados implicitamente para a esquerda ao serem removidos pelo remove()
            // então (meio) irá conter o próximo elemento a ser removido
            newPag.keys.add(paginaBT.keys.remove(meio));
            newPag.children.add(paginaBT.children.remove(meio + 1));
        }

        // Inserção e promoção
        // Primeiro caso: inserção na página da esquerda
        if (i <= meio) {
            paginaBT.keys.add(i, auxKey);
            paginaBT.children.add(i+1, auxPagina);

            /* Se a página for folha, seleciona o primeiro elemento
             * da pagina da direita para ser promovido
             */
            if (paginaBT.children.get(0) == -1) { // primeiro ponteiro == -1 significa que a página é uma folha
                auxKey = newPag.keys.remove(0);
                newPag.children.remove(0);
            }
            // Caso contrário, promove o maior elemento da página da esquerda
            else {
                auxKey = paginaBT.keys.remove(paginaBT.keys.size() - 1);
                paginaBT.children.remove(paginaBT.children.size() - 1);
            }
        }

        // Segundo caso: inserção na página da direita
        else {
            int j = max_keys - meio;
            // Encontrar posição de inserção
            while (auxKey.id < newPag.keys.get(j - 1).id) {
                j--;
            }
            newPag.keys.add(j, auxKey);
            newPag.children.add(j + 1, auxPagina);

            // Seleciona o primeiro elemento da página da direita para ser promovido
            auxKey = newPag.keys.remove(0);
            newPag.children.remove(0);
        }

        // A nova página, que é a metade da direita da antiga, será escrita no fim do arquivo
        auxPagina = BTreeFile.length();
        BTreeFile.seek(auxPagina);
        BTreeFile.write(newPag.toByteArray());

        // A página antiga sofreu um split. Ela será substituida pela sua metade da esquerda
        BTreeFile.seek(pagina);
        BTreeFile.write(paginaBT.toByteArray());

        return true;
    }

    public boolean delete(RegistroBTree registro) throws IOException {
        BTreeFile.seek(0);
        BTreeFile.readInt(); // ordem
        long offsetRaiz = BTreeFile.readLong(); // endereço da raiz

        // Váriavel global: checagem de remoção de uma página da árvore
        diminuiu = false;

        boolean excluido = delete(registro, offsetRaiz);

        // Eliminação da raiz
        if (excluido && diminuiu) {
            BTreeFile.seek(offsetRaiz);
            // Traz página pra memória primária
            PaginaBTree paginaBT = new PaginaBTree(ordem);
            byte[] buffer = new byte[paginaBT.tamanho_pagina];
            BTreeFile.read(buffer);
            paginaBT.fromByteArray(buffer);

            // Atualiza o ponteiro de raiz para a nova raiz
            if (paginaBT.keys.size() == 0) {
                BTreeFile.seek(4); // pula ordem da árvore
                BTreeFile.writeLong(paginaBT.children.get(0));
            }
        }

        return excluido;
    }

    private boolean delete(RegistroBTree registro, long pagina) throws IOException {
        boolean excluido = false;
        // Váriavel para guardar índice do ponteiro de uma página possivelmente excluída
        int diminuido;

        // Se estamos em -1, então estamos no filho de uma folha, o que significa que o registro não foi encontrado
        if (pagina == -1) {
            diminuiu = false;
            return false;
        }

        // Trazendo página pra memória primária
        BTreeFile.seek(pagina);
        PaginaBTree paginaBT = new PaginaBTree(ordem);
        byte[] buffer = new byte[paginaBT.tamanho_pagina];
        BTreeFile.read(buffer);
        paginaBT.fromByteArray(buffer);

        int i = 0;
        while (i < paginaBT.keys.size() && registro.id > paginaBT.keys.get(i).id) {
            i++;
        }

        // Caso 1: O elemento está em uma folha e a folha mantém 50% de ocupação
        if (i < paginaBT.keys.size() && paginaBT.children.get(0) == -1 && registro.id == paginaBT.keys.get(i).id) {
            System.out.println("Caso 1: Removendo elemento de uma folha com ocupação suficiente.");
            paginaBT.keys.remove(i);
            paginaBT.children.remove(i + 1);

            // Atualiza a página no arquivo
            BTreeFile.seek(pagina);
            BTreeFile.write(paginaBT.toByteArray());

            diminuiu = paginaBT.keys.size() < paginaBT.min_keys;
            return true;
        }

        // Caso 2: O elemento não está em uma folha, trocá-lo pelo antecessor
        if (i < paginaBT.keys.size() && paginaBT.children.get(0) != -1 &&  registro.id == paginaBT.keys.get(i).id) {
            System.out.println("Caso 2: Substituindo elemento pelo antecessor.");
            long endPaginaAntecessor = paginaBT.children.get(i);
            PaginaBTree paginaAntecessor = new PaginaBTree(ordem);

            // Navega até o antecessor (último elemento da subárvore esquerda)
            while (endPaginaAntecessor != -1) {
                BTreeFile.seek(endPaginaAntecessor);
                BTreeFile.read(buffer);
                paginaAntecessor.fromByteArray(buffer);
                endPaginaAntecessor = paginaAntecessor.children.get(paginaAntecessor.keys.size());
            }

            // Substitui o registro pelo antecessor
            RegistroBTree antecessor = paginaAntecessor.keys.get(paginaAntecessor.keys.size() - 1);
            paginaBT.keys.set(i, antecessor);

            // Remove o antecessor recursivamente
            delete(antecessor, paginaBT.children.get(i));

            // Atualiza a página no arquivo
            BTreeFile.seek(pagina);
            BTreeFile.write(paginaBT.toByteArray());

            return true;
        }

        // Caso 3 e 4: Continua a busca recursivamente
        excluido = delete(registro, paginaBT.children.get(i));
        diminuido = i;

        // Se a página filha ficou com menos de 50% de ocupação
        if (diminuiu) {
            long endPaginaFilha = paginaBT.children.get(diminuido);
            PaginaBTree paginaFilha = new PaginaBTree(ordem);
            BTreeFile.seek(endPaginaFilha);
            BTreeFile.read(buffer);
            paginaFilha.fromByteArray(buffer);

            // Páginas irmãs
            long endPaginaEsq = -1;
            long endPaginaDir = -1;
            PaginaBTree paginaEsq = null;
            PaginaBTree paginaDir = null;

            // Carrega a página irmã esquerda, se existir
            if (diminuido > 0) {
                endPaginaEsq = paginaBT.children.get(diminuido - 1);
                BTreeFile.seek(endPaginaEsq);
                BTreeFile.read(buffer);
                paginaEsq = new PaginaBTree(ordem);
                paginaEsq.fromByteArray(buffer);
            }

            // Carrega a página irmã direita, se existir
            if (diminuido < paginaBT.keys.size()) {
                endPaginaDir = paginaBT.children.get(diminuido + 1);
                BTreeFile.seek(endPaginaDir);
                BTreeFile.read(buffer);
                paginaDir = new PaginaBTree(ordem);
                paginaDir.fromByteArray(buffer);
            }

            // Caso 3: Redistribuição com a página irmã
            if (paginaEsq != null && paginaEsq.keys.size() > paginaBT.min_keys) {
                System.out.println("Caso 3: Redistribuição com a página irmã esquerda.");
                paginaFilha.keys.add(0, paginaBT.keys.get(diminuido - 1));
                paginaBT.keys.set(diminuido - 1, paginaEsq.keys.remove(paginaEsq.keys.size() - 1));
                if (paginaFilha.children.get(0) != -1) {
                    paginaFilha.children.add(0, paginaEsq.children.remove(paginaEsq.children.size()));
                }
            } else if (paginaDir != null && paginaDir.keys.size() > paginaBT.min_keys) {
                System.out.println("Caso 3: Redistribuição com a página irmã direita.");
                paginaFilha.keys.add(paginaBT.keys.get(diminuido));
                paginaBT.keys.set(diminuido, paginaDir.keys.remove(0));
                if (paginaFilha.children.get(0) != -1) {
                    paginaFilha.children.add(paginaDir.children.remove(0));
                }
            }

            // Caso 4: Fusão com a página irmã
            else if (paginaEsq != null) {
                System.out.println("Caso 4: Fusão com a página irmã esquerda.");
                paginaEsq.keys.add(paginaBT.keys.remove(diminuido - 1));
                paginaEsq.keys.addAll(paginaFilha.keys);
                paginaEsq.children.addAll(paginaFilha.children);
                paginaBT.children.remove(diminuido);
            } else if (paginaDir != null) {
                System.out.println("Caso 4: Fusão com a página irmã direita.");
                paginaFilha.keys.add(paginaBT.keys.remove(diminuido));
                paginaFilha.keys.addAll(paginaDir.keys);
                paginaFilha.children.addAll(paginaDir.children);
                paginaBT.children.remove(diminuido + 1);
            }

            // Atualiza as páginas no arquivo
            BTreeFile.seek(pagina);
            BTreeFile.write(paginaBT.toByteArray());
            BTreeFile.seek(endPaginaFilha);
            BTreeFile.write(paginaFilha.toByteArray());
            if (paginaEsq != null) {
                BTreeFile.seek(endPaginaEsq);
                BTreeFile.write(paginaEsq.toByteArray());
            }
            if (paginaDir != null) {
                BTreeFile.seek(endPaginaDir);
                BTreeFile.write(paginaDir.toByteArray());
            }

            // Verifica se a página pai ficou com menos de 50% de ocupação
            diminuiu = paginaBT.keys.size() < paginaBT.min_keys;
        }

        return excluido;
    }

    /**
     * Printa a árvore B.
     * @throws IOException
     */
    public void print() throws IOException {
        BTreeFile.seek(4); // pulando ordem da árvore
        long offsetRaiz = BTreeFile.readLong();
        // Printa o offset da raiz
        System.out.println("Raiz: " + String.format("%04d", offsetRaiz));
        // Se o offset da raiz foir -1, a árvore está vazia.
        if (offsetRaiz != -1) {
            print(offsetRaiz);
        }
        System.out.println();
    }
    /**
     * Função auxiliar:
     * Percorre recursivamente as páginas para printá-las
     * @param pagina endereço da página a ser acessada
     * @throws IOException
     */
    private void print(long pagina) throws IOException {
        // Filho de folha alcançado. Volta recursivamente para a página anterior.
        if (pagina == -1) {
            return;
        }
        int i;

        // Move ponteiro para a página
        BTreeFile.seek(pagina);
        // Traz a página para a memória primária
        PaginaBTree paginaBT = new PaginaBTree(ordem);
        byte[] buffer = new byte[paginaBT.tamanho_pagina];
        int bytesLidos = BTreeFile.read(buffer);

        if (bytesLidos != buffer.length) {
            throw new IOException("Erro ao ler a página. Bytes esperados: " + buffer.length + ", lidos: " + bytesLidos);
        }

        paginaBT.fromByteArray(buffer);

        // Offset da página e seu número de elementos
        String endereco = String.format("%04d", pagina);
        System.out.println("Endereço da página: " + endereco + ", elementos na página: " + paginaBT.keys.size() + ":");
        // Percorre a página, printando ponteiros e registros.
        for (i = 0; i < paginaBT.keys.size(); i++) {
            System.out.print("[" + String.format("%04d", paginaBT.children.get(i)) + "] " + paginaBT.keys.get(i) + " ");
        }
        // i está posicionado no index do ponteiro que falta pra imprimir
        if (i > 0) {
            System.out.print("[" + String.format("%04d", paginaBT.children.get(i)) + "]");
        }
        else { // página vazia
            System.out.print("[-001]");
        }
        // imprime espaços vazios caso a página não esteja cheia
        for (; i < max_keys; i++) {
            System.out.print(" ------------------- [-001]");
        }
        System.out.println();
        System.out.println();

        // se a página não for uma folha, ainda há mais a ser imprimido
        if (paginaBT.children.get(0) != -1) {
            // para cada ponteiro, imprime sua página correspondente
            for (i = 0; i < paginaBT.keys.size(); i++) {
                print(paginaBT.children.get(i));
            }
            // último ponteiro faltante da direita
            print(paginaBT.children.get(i));
        }
    }
}