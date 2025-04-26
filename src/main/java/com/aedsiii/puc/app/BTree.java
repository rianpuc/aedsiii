package com.aedsiii.puc.app;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

import com.aedsiii.puc.model.PaginaBTree;
import com.aedsiii.puc.model.RegistroBTree;

/**
 * Classe que representa uma Árvore B.
 */
public class BTree {
    private int ordem;
    private int max_keys;
    private int max_children;
    public PaginaBTree pagina;
    private RandomAccessFile BTreeFile;
    
    // Variáveis auxiliares
    public RegistroBTree auxKey;
    private long auxPagina;
    private boolean cresceu;
    private boolean diminuiu;
    public boolean antecessoraPendente;
    // OFFSET DO REGISTRO A SER REMOVIDO NO DATASET
    public long auxRemovalOffset;
    // OFFSET DO CAMPO LONG NO REGISTRO BTREE
    public long auxUpdateOffset;

    /**
     * Cria o arquivo da árvore B.
     * @param ordem máximo de filhos da página.
     * @param BTreeFilePath caminho de armazenamento do arquivo.
     * @param makeNewTree true para criar uma nova árvore, false senão.
     * @throws IOException
     */
    public BTree(int ordem, String BTreeFilePath, boolean makeNewTree) throws IOException {
        System.out.println("Criando Árvore B de ordem: " + ordem);
        this.ordem = ordem;
        this.max_children = this.ordem;
        this.max_keys = this.ordem - 1;
        
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

        this.antecessoraPendente = false;
        this.auxRemovalOffset = -1;
        this.auxUpdateOffset = -1;
        System.out.println("Árvore B criada.");
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
     * Função auxiliar:
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
            BTreeFile.seek(pagina);
            BTreeFile.readInt(); // Número de elementos na página
            // Endereços a se skippar
            int keyBytesSkip = RegistroBTree.size() * i;
            // Ponteiros a se skippar
            int pointerBytesSkip = Long.BYTES * (i + 1);
            BTreeFile.skipBytes(keyBytesSkip + pointerBytesSkip);
            // só pra verificar o ID
            System.out.println(BTreeFile.readShort());
            auxUpdateOffset = BTreeFile.getFilePointer();
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

    /**
     * Remove um registro na árvore B em arquivo.
     * @param deleteID id a ser deletado.
     * @return true se for excluído, senão false.
     * @throws IOException
     */
    public boolean delete(int deleteID) throws IOException {
        // System.out.println("Indo deletar: " + deleteID);
        BTreeFile.seek(0);
        BTreeFile.readInt(); // ordem
        long offsetRaiz = BTreeFile.readLong(); // endereço da raiz

        // Váriavel global: checagem de remoção de uma página da árvore
        diminuiu = false;

        boolean excluido = delete((short) deleteID, offsetRaiz);

        // Eliminação da raiz
        if (excluido && diminuiu) {
            BTreeFile.seek(4);
            offsetRaiz = BTreeFile.readLong();
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

    /**
     * Função auxiliar:
     * Navega recursivamente até a página correta de remoção, e remove o novo registro.
     * 
     * Trata os 4 casos de remoção em árvore B.
     * 
     * @param deleteID id a ser deletado. 
     * @param pagina endereço da página a ser acessada.
     * @return true se for excluído, senão false.
     * @throws IOException
     */
    private boolean delete(short deleteID, long pagina) throws IOException {
        boolean excluido = false;
        // Váriavel para guardar índice do ponteiro de uma página possivelmente diminuida
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
        while (i < paginaBT.keys.size() && deleteID > paginaBT.keys.get(i).id) {
            i++;
        }

        // Colocando aq atrás pra ver se resolve o problema em nós internos
        diminuido = i; // índice da página possivelmente excluída/diminuida

        if (i < paginaBT.keys.size() && paginaBT.children.get(0) == -1 && deleteID == paginaBT.keys.get(i).id) {
            // Caso a chave esteja em uma folha (endereço do primeiro ponteiro da pagina = -1)
            // System.out.println("Chave encontrada em uma folha");
            auxRemovalOffset = paginaBT.keys.get(i).offset;
            // Puxa as chaves após a excluída para a esquerda automaticamente
            paginaBT.keys.remove(i);
            paginaBT.children.remove(i+1);

            // Atualiza página na árvore
            BTreeFile.seek(pagina);
            BTreeFile.write(paginaBT.toByteArray());

            // Verificar se a página ainda tem a quantidade mínima de chaves por página
            diminuiu = paginaBT.keys.size() < paginaBT.min_keys;
            return true;
        }

        // Caso 2: chave em nó interno. Ela deve ser substituída por sua antecessora.
        /* INICIO CASO 2 */

        if (i < paginaBT.keys.size() && paginaBT.children.get(0) != -1 && deleteID == paginaBT.keys.get(i).id && antecessoraPendente == false) {
            // Se o endereço do primeiro ponteiro não for -1, então é um nó interno.
            // System.out.println("Chave encontrada em um nó interno.");
            auxRemovalOffset = paginaBT.keys.get(i).offset;
            // System.out.println("Buscando antecessor...");
            long endPaginaAntecessor = paginaBT.children.get(i);
            PaginaBTree paginaAntecessor = new PaginaBTree(ordem);

            // Navega até o antecessor
            while (endPaginaAntecessor != -1) {
                BTreeFile.seek(endPaginaAntecessor);
                BTreeFile.read(buffer);
                paginaAntecessor.fromByteArray(buffer);
                endPaginaAntecessor = paginaAntecessor.children.get(paginaAntecessor.keys.size());
            }

            // Substitui a chave a ser eliminada por sua antecessora.
            RegistroBTree antecessor = paginaAntecessor.keys.get(paginaAntecessor.keys.size() - 1);
            // System.out.println("Antecessor encontrado: " + antecessor.id);
            paginaBT.keys.set(i, antecessor);
            BTreeFile.seek(pagina);
            BTreeFile.write(paginaBT.toByteArray());

            // A chave antecessora agora está pendente para ser removida de onde estava anteriormente.
            antecessoraPendente = true;
            auxKey = antecessor;

            return true;
        }

        /* FIM CASO 2 */

        // Caso a chave ainda não tenha sido encontrada, continua a busca
        delete(deleteID, paginaBT.children.get(i));

        /* TRATAMENTO DE DIMINUIÇÃO DE CHAVES NAS PÁGINAS ABAIXO */
        /* CASOS 3 E 4 */

        // Se a página tiver diminuido pra uma quantidade menor do que a mínima de chaves necessárias, será feita uma fusão
        if (diminuiu) {
            // Estamos atualmente na recursão: na página pai da página diminuida, para poder ter acesso às suas irmãs
            // PaginaBT: página atual
            // PaginaFilha: página com menos chaves do que o necessário

            // Trazendo página com menos chaves do que o necessário para a memória primária
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

            // Se a página diminuída não for a primeira filha, então ela possui irmã esquerda
            if (diminuido > 0) {
                endPaginaEsq = paginaBT.children.get(diminuido - 1);
                BTreeFile.seek(endPaginaEsq);
                BTreeFile.read(buffer);
                paginaEsq = new PaginaBTree(ordem);
                paginaEsq.fromByteArray(buffer);
            }
            // Se a página diminuída não for a última, então ela possui irmã direita
            if (diminuido != paginaBT.keys.size()) {
                endPaginaDir = paginaBT.children.get(diminuido + 1);
                BTreeFile.seek(endPaginaDir);
                BTreeFile.read(buffer);
                paginaDir = new PaginaBTree(ordem);
                paginaDir.fromByteArray(buffer);
            }

            /* INICIO CASO 3 */

            /* Se a página da esquerda existe e possui mais chaves do que o mínimo necessário
             * Etapas:
             * Identificar a maior chave da página esquerda
             * Substituir a chave eliminada pela chave pai
             * Substituir a chave pai pela maior chave da página esquerda
             */ 
            if (paginaFilha.children.get(0) == -1 && paginaEsq != null && paginaEsq.keys.size() > paginaBT.min_keys) {
                // System.out.println("Pagina é folha");
                // System.out.println("Pagina esquerda existe e POSSUI mais chaves do que o mínimo necessário");

                // Guardando a chave pai
                RegistroBTree chaveDoPai = paginaBT.keys.get(diminuido - 1);
                // Substituindo a chave pai pela maior chave da página esquerda
                paginaBT.keys.set(diminuido - 1, paginaEsq.keys.remove(paginaEsq.keys.size() - 1));
                // Removendo ponteiro que não existirá mais na página esquerda
                paginaFilha.children.add(0, paginaEsq.children.remove(paginaEsq.children.size() - 1));
                // Substituindo a chave eliminada pela chave pai
                paginaFilha.keys.add(0, chaveDoPai);
            }

            // Aqui, significa que a página da esquerda não existe

            /* Se a página da direita existe e possui mais chaves do que o mínimo necessário
             * Etapas:
             * Identificar a menor chave da página direita
             * Substituir a chave eliminada pela chave pai
             * Substituir a chave pai pela menor chave da página direita
             */
            else if (paginaFilha.children.get(0) == -1 && paginaDir != null && paginaDir.keys.size() > paginaBT.min_keys) {
                // System.out.println("Pagina é folha");
                // System.out.println("Pagina direita existe e POSSUI mais chaves do que o mínimo necessário");

                // Guardando a chave pai
                RegistroBTree chaveDoPai;
                chaveDoPai = paginaBT.keys.get(diminuido);
                // Substituindo a chave pai pela menor chave da página direita
                paginaBT.keys.set(diminuido, paginaDir.keys.remove(0));
                // Removendo ponteiro que não existirá mais na página direita
                paginaFilha.children.add(paginaDir.children.remove(0));
                // Substituindo a chave eliminada pela chave pai
                paginaFilha.keys.add(chaveDoPai);
            }
            
            /* FIM CASO 3 */

            /* INICIO CASO 4 */

            // Se a página da esquerda existir e não possuir mais chaves do que o mínimo necessário
            else if (paginaEsq != null) {
                // System.out.println("Pagina esquerda existe e não possui mais chaves do que o mínimo necessário");
                // Se a página reduzida não for folha, então o elemento
                // do pai deve descer para o irmão
                if (paginaFilha.children.get(0) != -1) {
                    // System.out.println("Pagina não é folha");
                    paginaEsq.keys.add(paginaBT.keys.remove(diminuido - 1));
                    paginaEsq.children.add(paginaFilha.children.remove(0));
                }
                else {
                    // System.out.println("Pagina é folha");
                    paginaEsq.keys.add(paginaBT.keys.remove(diminuido - 1));
                }
                // Remove ponteiro para a própria página
                paginaBT.children.remove(diminuido);

                // Copia os registros, fazendo a fusão
                paginaEsq.keys.addAll(paginaFilha.keys);
                paginaEsq.children.addAll(paginaFilha.children);
                paginaFilha.keys.clear();
                paginaFilha.children.clear();
            }

            // Se a página da direita existir e não possuir mais chaves do que o mínimo necessário
            else {
                System.out.println("Pagina direita existe e não possui mais chaves do que o mínimo necessário");
                // Se a página reduzida não for folha, então o elemento
                // do pai deve descer para o irmão
                if (paginaFilha.children.get(0) != -1) {
                    // System.out.println("Pagina não é folha");
                    paginaFilha.keys.add(paginaBT.keys.remove(diminuido));
                    paginaFilha.children.add(paginaDir.children.remove(0));
                } else {
                    System.out.println("Pagina é folha");
                    paginaFilha.keys.add(paginaBT.keys.remove(diminuido));
                }
                // Remove ponteiro pra o irmã da direita
                paginaBT.children.remove(diminuido + 1);

                // Copia os registros, fazendo a fusão
                paginaFilha.keys.addAll(paginaDir.keys);
                paginaFilha.children.addAll(paginaDir.children);
                paginaDir.keys.clear();
                paginaDir.children.clear();
            }

            /* FIM CASO 4 */

            // Verifica se a página pai ficou com menos keys do que o número mínimo necessário
            diminuiu = paginaBT.keys.size() < paginaBT.min_keys;

            // Atualiza os demais registros
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
        }
        return excluido;
    }

    /**
     * Função auxiliar de update. Atualiza o offset de um registro atualizado, para refletir seu novo local no dataset.
     * @param datasetOffset Offset do novo local do registro no dataset.
     * @throws IOException
     */
    public void updateOffset(long datasetOffset) throws IOException {
        try {
            BTreeFile.seek(auxUpdateOffset);
            BTreeFile.writeLong(datasetOffset);
        } catch (IOException e) {
            System.out.println("Erro em Btree.updateOffset: " + e);
        }
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