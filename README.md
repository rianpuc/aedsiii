# Trabalho de Algoritmos e Estruturas de Dados III

## Resumo

Este relatório detalha o desenvolvimento de um sistema de gerenciamento de banco de dados em Java, focado na manipulação eficiente de arquivos e na implementação de algoritmos avançados de estrutura de dados. O projeto teve como base a criação e manipulação de um arquivo de dados sequencial em nível de byte (.db), sobre o qual foram implementadas as operações de Create, Read, Update e Delete (CRUD).

Para otimizar o acesso e manipulação dos registros, usamos três estratégias de indexação de arquivos: Árvore B, Hash Extensível e Lista Invertida. Cada índice foi integrado ao sistema para proporcionar consultas e operações de CRUD mais eficientes em comparação com a busca sequencial.

Adicionalmente, implementamos no trabalho técnicas de compactação de dados com a implementação dos algoritmos LZW (Lempel-Ziv-Welch) e Huffman, permitindo a redução do espaço de armazenamento dos registros no arquivo principal. Para a funcionalidade de busca, implementamos os algoritmos de casamento de padrões KMP (Knuth-Morris-Pratt) e Boyer-Moore.

O projeto consolida de forma prática os conceitos teóricos da disciplina de Algoritmos e Estruturas de Dados 3, demonstrando a aplicação e o impacto de diferentes estruturas de dados e algoritmos na construção de um sistema robusto e eficiente para manipulação de arquivos.

### Introdução

Os Trabalhos Práticos de AEDS III têm como objetivo permitir que o aluno implemente a representação de entidades em registros, armazene-os em memória secundária e faça a manipulação destes registros através de acessos sequenciais ou indexados, além de aplicar outros tipos de algoritmos como **indexação**, **compactação**, **casamentro de padrões** e **criptografia**. Esses algoritmos foram estudados e implementados durante o semestre, com entregas separadas em 4 etapas.

## Desenvolvimento

### Etapa 1: Criação da base de dados e manipulação de arquivo sequencial

Na primeira etapa do trabalho, escolhemos uma base de dados de vagas de emprego com 10000 registros com 22 campos cada registro em arquivo .csv e convertemos em arquivo .db incluindo todos os registros. Este arquivo serviu de base para a realização de todo o resto do trabalho. Implementamos inicialmente operações de CRUD varrendo o arquivo sequencialmente, usando leituras e escritas byte a byte.

### Etapa 2: Manipulação de arquivo indexado com árvore B, hash e lista invertida

A manipulação direta de um arquivo sequencial, embora fundamental para o entendimento da persistência de dados, apresenta uma grande desvantagem em termos de desempenho: a falta de eficiência em operações de busca, atualização e exclusão. Em um arquivo puramente sequencial, a única maneira de encontrar um registro específico é percorrer o arquivo desde o início, uma operação de busca linear (complexidade O(n)) que se torna proibitivamente lenta à medida que o volume de dados cresce.

Para superar essa limitação, foram implementadas estruturas de dados chamadas índices. Um índice funciona como um mapa ou um sumário para o arquivo de dados principal: é uma estrutura auxiliar, geralmente menor e mais otimizada, que armazena "ponteiros" para a localização exata dos registros no arquivo .db. Ao invés de varrer o arquivo inteiro, o sistema primeiro consulta o índice para descobrir rapidamente onde o dado desejado se encontra, e então acessa diretamente essa posição.

#### Etapa 2.1: Árvore B

A Árvore B é uma estrutura de dados em árvore auto-balanceada, projetada especificamente para sistemas de gerenciamento de bancos de dados e sistemas de arquivos, onde os dados são lidos e escritos em blocos de disco. Diferente das árvores de busca binária, os nós de uma Árvore B podem ter um número variável e grande de filhos, o que minimiza a altura da árvore e, consequentemente, o número de acessos a disco necessários para localizar um registro.

Suas principais características são a manutenção do balanceamento durante inserções e exclusões e a ordenação das chaves em seus nós. Isso a torna eficiente não apenas para buscas por chave única, mas também para buscas em faixa (por exemplo, "encontrar vagas com salário entre 7 e 10 mil").

#### Etapa 2.2: Hash

A indexação por Hash oferece a abordagem mais rápida possível para buscas por chave exata. A ideia central é utilizar uma função de hash para transformar uma chave de busca diretamente em um endereço ou bucket onde o ponteiro para o registro está localizado. Em um cenário ideal, isso permite o acesso a qualquer registro em tempo constante (O(1)).

Contudo, o hashing tradicional apresenta um problema conhecido como colisão — quando duas chaves diferentes geram o mesmo endereço de hash. O Hash Extensível é uma técnica dinâmica para resolver colisões e permitir que o arquivo de índice cresça e encolha de forma eficiente. Ele utiliza um diretório (uma tabela de ponteiros) que pode duplicar de tamanho conforme necessário, dividindo apenas os buckets que estão cheios, sem exigir a reorganização de todo o arquivo. Essa abordagem garante um desempenho de acesso rápido para operações de CRUD baseadas em uma chave única e se adapta bem a bancos de dados que sofrem muitas inserções e exclusões.

#### Etapa 2.3: Lista invertida

Enquanto a Árvore B e o Hash são idealmente aplicados a campos de chave primária ou única, a Lista Invertida é uma técnica de indexação projetada para buscas textuais ou em campos com valores repetidos (chaves secundárias). Seu funcionamento é análogo ao índice remissivo de um livro: ela cria um dicionário de todos os termos (palavras ou valores) que aparecem nos registros e, para cada termo, armazena uma lista de ponteiros para todos os registros que contêm aquele termo.

Por exemplo, na nossa base de dados de vagas de empregos, uma lista invertida poderia mapear o termo "Developer" para todos os registros no arquivo de dados. Sua grande vantagem é a capacidade de responder de forma eficiente a consultas que envolvem múltiplos termos, como "Software Architect". Por essa razão, a lista invertida é a estrutura de dados central por trás da maioria dos motores de busca da web e de sistemas de busca em texto completo (full-text search).

#### Etapa 2.4: Implementação

Nesta segunda etapa, implementamos o uso dos índices discutidos acima. Na árvore B e no hash, podemos reduzir o tamanho de cada registro para conter apenas um campo de interesse e seu endereço no arquivo de dados. Tendo em vista que os registros que usamos originalmente possuíam 22 campos, a redução para apenas 2 campos (ID e endereço) e utilização de algoritmos de árvore B e hash possibilitaram uma eficiência maior na procura de registros. Para que as pesquisas não sejam feitas apenas por ID, um campo que não revela muita informação sobre um registro, implementamos também listas invertidas para os campos de título da vaga e sua respectiva função.

### Etapa 3: Compactação e casamento de padrões

#### 3.1: Compactação usando LZW e Huffman

A compactação de dados é uma etapa fundamental em sistemas de computação que lidam com grandes volumes de informação, como é o caso de bancos de dados e manipulação de arquivos. O objetivo principal é reduzir a quantidade de bits necessária para representar uma informação, resultando em dois benefícios diretos: redução do espaço de armazenamento em disco e diminuição do tempo de transmissão de dados em redes. Em um sistema que opera sobre um arquivo .db, como o desenvolvido neste trabalho, a aplicação de algoritmos de compactação pode diminuir significativamente o tamanho do arquivo final, otimizando custos de armazenamento.

#### 3.1.1 Huffman:

O algoritmo de Huffman é um método de compactação baseado na frequência estatística dos símbolos (no caso de um texto, os caracteres) no conjunto de dados. A sua lógica central é engenhosa e simples: símbolos que aparecem com mais frequência no arquivo recebem códigos binários mais curtos, enquanto símbolos mais raros recebem códigos binários mais longos.

Para garantir que a descompactação seja possível sem ambiguidades, Huffman utiliza a propriedade de código de prefixo, onde nenhum código de um símbolo é o prefixo do código de outro símbolo. Isso é alcançado através da construção de uma árvore binária, a Árvore de Huffman, que é montada a partir da frequência de cada símbolo. O principal custo associado a este método é a necessidade de armazenar ou transmitir a tabela de frequências (ou a própria árvore) junto com os dados compactados, para que o descompactador possa realizar o processo inverso.

#### 3.1.2 LZW:

Diferente do Huffman, o LZW é um algoritmo baseado em dicionário. Em vez de analisar a frequência de símbolos individuais, o LZW identifica e armazena sequências de símbolos (padrões) que se repetem ao longo do arquivo. Ele constrói um dicionário dinamicamente, tanto durante a compactação quanto na descompactação.

O processo funciona da seguinte maneira: o algoritmo lê o arquivo e vai adicionando novas sequências de caracteres a um dicionário. Quando uma sequência que já está no dicionário é encontrada novamente, o algoritmo a substitui pelo código (ou índice) correspondente a essa entrada no dicionário. A grande vantagem do LZW é que o dicionário não precisa ser armazenado junto com o arquivo. Ele pode ser reconstruído do zero pelo descompactador, que segue a mesma lógica de construção do compressor.

Em resumo, enquanto Huffman explora a redundância na frequência de caracteres individuais, o LZW explora a redundância em sequências de caracteres, tornando-os complementares em suas áreas de maior eficácia.

### 3.2: Casamento de padrões usando KMP e Boyer Moore

Além das buscas estruturadas por meio de chaves e índices, uma funcionalidade essencial em qualquer sistema de dados robusto é a capacidade de realizar buscas por conteúdo livre dentro dos registros. Essa operação, conhecida como casamento de padrões, consiste em encontrar todas as ocorrências de um padrão dentro de um texto maior (o registro ou o arquivo).

#### 3.2.1: KMP:

O algoritmo KMP aprimora a busca ao utilizar o conhecimento sobre as falhas para evitar retrocessos desnecessários no texto. Seu ponto principal é que ele nunca move o ponteiro do texto para trás, apenas o ponteiro do padrão é ajustado. Isso é possível através de um pré-processamento do padrão para criar uma tabela auxiliar, chamada de vetor de prefixos ou LPS (do inglês, Longest Proper Prefix which is also a Suffix).

Funcionamento:

A tabela LPS armazena, para cada posição i do padrão, o tamanho do maior prefixo próprio do padrão p[0...i] que também é um sufixo dessa mesma sub-cadeia. Um "prefixo próprio" é qualquer prefixo que não seja a string inteira.

Quando ocorre uma falha na comparação entre o caractere i do texto e o caractere j do padrão, a abordagem ingênua simplesmente deslocaria o padrão em uma posição. O KMP, em vez disso, consulta lps[j-1]. O valor contido ali indica que não é preciso testar novamente os lps[j-1] caracteres anteriores, pois o algoritmo já sabe que eles correspondem a um prefixo do padrão. Assim, o padrão é deslocado para a frente de forma, alinhando esse prefixo com o sufixo correspondente no texto, e a comparação recomeça a partir da nova posição.

#### 3.2.2: Boyer Moore:

No algoritmo de Boyer Moore, ele inicia a comparação do padrão a partir do seu último caractere, e não do primeiro. Essa abordagem permite, em caso de falha, saltar múltiplos caracteres no texto de uma só vez.

Boyer-Moore utiliza duas heurísticas (regras) para determinar o quão longe o padrão pode ser deslocado:

Heurística do Caractere Ruim: Ao encontrar uma falha de correspondência, o algoritmo observa o caractere no texto que causou a falha (o "caractere ruim"). Ele então desloca o padrão para a direita até que a última ocorrência desse mesmo caractere no padrão se alinhe com o "caractere ruim" no texto. Se o caractere ruim não existir no padrão, o algoritmo pode deslocar o padrão inteiro para depois da posição da falha, resultando em um salto máximo.

Heurística do Sufixo Bom: Esta regra é aplicada quando uma falha ocorre após uma correspondência parcial de um sufixo do padrão. Se uma parte final do padrão (o "sufixo bom") já casou com o texto, o algoritmo procura por outra ocorrência desse mesmo sufixo dentro do padrão. Ele então desloca o padrão para alinhar essa outra ocorrência com o trecho correspondente no texto.

Implementamos ambas as regras, criando suas respectivas tabelas ou vetores necessários, e em cada falha de correspondência, utilizamos a regra que proporcionar maior salto na ocasião.

### 4: Criptografia

Após a estruturação, indexação e compactação dos dados, a etapa final do trabalho é garantir a segurança da informação. A criptografia tem o objetivo de proteger a confidencialidade dos dados, garantindo que apenas partes autorizadas possam ter acesso à informação original.

No contexto deste projeto, a aplicação de algoritmos criptográficos visa proteger o conteúdo do arquivo .db contra acesso não autorizado. Se o arquivo for interceptado ou acessado indevidamente, seu conteúdo permanecerá ilegível sem o conhecimento da chave e do algoritmo de decifragem corretos.

#### 4.1: Criptografia usando Cifra de César

A Cifra de César é um dos mais simples algoritmos de criptografia. Trata-se de uma cifra de substituição, na qual cada letra do texto a ser criptografado é substituída por outra letra que se encontra um número fixo de posições à frente no alfabeto. Esse número fixo de deslocamento é a chave do algoritmo. Por utilizar a mesma chave tanto para criptografar quanto para decifrar (deslocando na direção oposta), ela é classificada como um **algoritmo de chave simétrica**.

#### 4.2: 

## Testes e resultados

## Conclusão

O desenvolvimento deste projeto permitiu a aplicação prática e aprofundada dos algoritmos aprendidos em sala de aula. Cada etapa representou um avanço significativo na eficiência e utilidade do sistema.

A transição da manipulação sequencial para o uso de arquivos indexados com Árvore B, Hash Extensível e Lista Invertida demonstrou de forma clara o impacto das estruturas de dados no desempenho de operações de busca, atualização e exclusão. Foi possível ver na prática a eficiência do acesso indexado em comparação com a busca sequencial.

A implementação dos algoritmos de compactação LZW e Huffman adicionou uma camada de otimização de armazenamento, evidenciando como diferentes abordagens podem ser utilizadas para reduzir a redundância e o tamanho físico dos dados. Além disso, os algoritmos de casamento de padrões KMP e Boyer-Moore permitiram a busca por conteúdo, permitindo a localização eficiente de informações nos registros.

... criptografious
