# Análise do Sistema As-Is

## 1. Introdução e âmbito

Este documento descreve o estado inicial do repositório `odsoft-library-management-system`, com foco nos testes, nas práticas de build, no deployment e na qualidade de software. Descreve o sistema tal como se encontra e não sugere alterações. A análise foi realizada a 2026-10-22 sobre o commit `82d05ce5438a1fc4075d4853d298a7c0e593525e`, que é o único commit da branch `main`.

O trabalho seguiu cinco fases. Primeiro, a stack tecnológica foi identificada a partir dos ficheiros de build e de configuração e da estrutura de diretórios. Na segunda fase, o repositório foi avaliado quanto à existência de processos de CI/CD e de deployment. Na terceira fase, os testes foram localizados e classificados. Na quarta fase, a suite de testes existente foi executada e foi medida a sua cobertura. Por último, foram recolhidos os indicadores de qualidade de software disponíveis no repositório.

## 2. Visão geral do sistema

O sistema é uma aplicação web em Java 17, chamada `psoft-g1`, que implementa o back end de um sistema de gestão de bibliotecas e expõe uma API REST. O projeto é construído com Apache Maven e herda de `spring-boot-starter-parent` na versão 3.2.5.

As principais dependências declaradas no `pom.xml` são Spring Data JPA, Spring Web, Spring Validation, Spring Security com o resource server OAuth2 (autenticação por JSON Web Token, com um par de chaves RSA em `src/main/resources`), Spring WebFlux (usado como cliente web), springdoc-openapi 2.3.0 (documentação OpenAPI e Swagger UI), MapStruct 1.5.5.Final, Lombok 1.18.30 e o OWASP Java HTML sanitizer 20220608.1. A base de dados é uma H2. A configuração principal em `src/main/resources/application.properties` aponta para um servidor H2 (`jdbc:h2:tcp://localhost/~/psoft-g1`), com `spring.jpa.hibernate.ddl-auto=update`, e os ficheiros carregados são guardados no diretório `uploads-psoft-g1`. O sistema acede ainda a um serviço externo, a API Ninjas, através de `external/service` e de `ApiNinjasConfig`.

O sistema consiste num monólito composto por um único módulo Maven. A base de código tem 144 ficheiros Java principais e 23 ficheiros Java de teste. O código de produção em `src/main/java/pt/psoft/g1/psoftg1` está organizado por área de negócio, com os pacotes `authormanagement`, `bookmanagement`, `genremanagement`, `lendingmanagement`, `readermanagement`, `usermanagement`, `auth`, `shared`, `configuration`, `bootstrapping`, `exceptions` e `external`. Dentro de cada área de negócio, os subpacotes `api`, `model`, `services`, `repositories` e `infrastructure` (escrito `infraestructure` em `readermanagement`) separam os controladores, o modelo de domínio, os serviços e a persistência. Os parâmetros de negócio (duração do empréstimo de 15 dias, multa de 200 cêntimos por dia, idade mínima do leitor de 12 anos e limite de sugestões por género de 2) estão definidos em `src/main/resources/config/library.properties`.

## 3. Implementação e deployment

O build é definido apenas pelo `pom.xml`, que configura o `spring-boot-maven-plugin` e o `maven-compiler-plugin` (release 17, com Lombok, MapStruct e o Lombok-MapStruct binding como processadores de anotações). O plugin do Spring Boot empacota a aplicação como um ficheiro executável. O repositório contém os scripts do Maven wrapper `mvnw` e `mvnw.cmd`, mas o diretório `.mvn` de que o wrapper necessita não existe, pelo que o wrapper não pôde ser utilizado.

Não foi encontrada qualquer configuração de CI/CD. O repositório não define triggers de pipeline, stages, quality gates, regras de cache, publicação de artefactos de build nem destinos de deployment. Também não existem Dockerfile, docker-compose, manifestos Kubernetes, charts Helm nem Infrastruture As Code (IaC).

O deployment atual é manual e local. A aplicação é executada na máquina do desenvolvedor, com a base de dados H2 em modo servidor iniciada manualmente (`jdbc:h2:tcp://localhost/~/psoft-g1`, como indicam os comentários de `application.properties`) e com o perfil `bootstrap` ativo por omissão para carregar os dados iniciais. O único ambiente de execução é a máquina local.

A configuração é mantida em ficheiros `.properties`. O ficheiro `src/main/resources/application.properties` contém as credenciais da base de dados, o diretório de uploads e uma chave de API de terceiros em texto simples, e o par de chaves RSA usado para assinar os tokens está guardado como ficheiros simples em `src/main/resources`. Não está configurada nenhuma gestão externa de segredos (variáveis de ambiente, vault ou ficheiros cifrados). A configuração de teste em `src/test/resources/application.properties` usa uma base de dados H2 em memória (`jdbc:h2:mem:testdb`), e o ficheiro `src/test/resources/application-integrationtest.properties` tem uma única linha que seleciona `jdbc:h2:mem:test`. Para além do perfil Spring `bootstrap`, que carrega dados iniciais através das classes em `src/main/java/pt/psoft/g1/psoftg1/bootstrapping`, não estão definidos ambientes separados (desenvolvimento, staging ou produção). O versionamento limita-se à versão Maven `0.0.1-SNAPSHOT`, e não foram encontradas notas de release nem automatização de releases.

## 4. Práticas de build e de teste

### 4.1 Processo de build

O build é manual e local, executado pelo desenvolvedor com o Maven. O ciclo de vida standard compila o código com o `maven-compiler-plugin` (Java 17), com Lombok, MapStruct e o Lombok-MapStruct binding como processadores de anotações, executa os testes com o Surefire (`mvn test`) e, na fase de package, o `spring-boot-maven-plugin` gera o ficheiro executável. O processo não está automatizado: não existe pipeline, build de integração contínua, publicação de artefactos nem cache. Como o wrapper `mvnw` não é utilizado, é necessária uma instalação local do Maven.

### 4.2 Práticas de teste

Os testes são executados localmente através do ciclo de vida standard do Maven (`mvn test`). O `pom.xml` não tem nenhuma entrada para o `maven-surefire-plugin`, pelo que se aplicam as predefinições herdadas do parent do Spring Boot. A execução em paralelo, a repetição de testes falhados e outros mecanismos de tratamento de testes instáveis (flaky) não estão configurados. Não é declarado nenhum plugin de cobertura, nenhum plugin de análise estática nem nenhum perfil de teste. Os ficheiros versionados não contêm pre-commit hooks, e não foi encontrado nenhum diretório `.githooks`, nem configuração de `husky` ou de `pre-commit`.

Os testes usam JUnit 5 (Jupiter), fornecido pelo `spring-boot-starter-test`, que traz também AssertJ, Hamcrest e Mockito, a par do `spring-security-test`. O `pom.xml` declara ainda o artefacto mais antigo `junit:junit` com âmbito de teste, mas nenhum teste importa JUnit 4, uma vez que todos importam `org.junit.jupiter.api`. Os testes que necessitam de contexto Spring usam `@SpringBootTest`, `@DataJpaTest` com `TestEntityManager` ou `@WebMvcTest`, e os testes de persistência correm sobre a base de dados H2 em memória configurada em `src/test/resources/application.properties`. Os dados de teste são criados dentro dos próprios testes: `LendingServiceImplTest` e `LendingRepositoryIntegrationTest` criam autores, livros, leitores e empréstimos em métodos de preparação e usam `@Transactional`, enquanto os testes de modelo constroem diretamente os objetos de domínio. As classes auxiliares `testutils/UserTestDataFactory.java` e `testutils/JsonHelper.java` apoiam o teste de autenticação, que se encontra comentado. A base de dados H2 em memória funciona como fake da base de dados de produção. O único double de um colaborador é o `@MockBean` de `AuthorRepository` em `AuthorServiceImplIntegrationTest`, configurado com `Mockito.when(...).thenReturn(...)` e sem verificação de chamadas, pelo que desempenha o papel de stub. Não são usados contentores nem servidores stub.

As convenções de nomenclatura não são uniformes. A maioria das classes termina em `Test` (por exemplo `IsbnTest` e `LendingTest`), os testes de integração incluem `IntegrationTest` no nome (por exemplo `LendingRepositoryIntegrationTest`) e uma classe começa por `Test` (`TestAuthApi`). Os pacotes de teste espelham os pacotes de produção em `src/test/java/pt/psoft/g1/psoftg1`, com os subpacotes `model`, `services`, `repository` e `api`. Os nomes dos métodos nos testes de modelo começam habitualmente por `ensure` (por exemplo `ensureChecksum13IsCorrect`). Alguns ficheiros de teste contêm comentários que remetem para tutoriais externos, e dois deles (`AuthorServiceImplIntegrationTest` e `AuthorControllerIntegrationTest`) indicam que foram adaptados para JUnit 5 com a ajuda de um assistente generativo.

## 5. Quantidade de testes e cobertura

### 5.1 Indicadores resumo

| Indicador | Valor |
|---|---|
| Total de testes (aprovados / falhados / ignorados) | 102 (102 / 0 / 0) |
| Classes de teste executadas (ficheiros de teste em `src/test/java`) | 19 (23) |
| Tempo de execução dos testes | 13,2 s (37,4 s de build Maven total) |
| Código de produção (linhas não vazias) | 7375 (144 ficheiros) |
| Código de teste (linhas não vazias) | 1321 (23 ficheiros) |
| Rácio entre código de teste e código de produção | 0,18 |
| Cobertura de linhas | 19,5% (558 de 2868) |
| Cobertura de branch | 4,5% (117 de 2579) |
| Cobertura de métodos | 18,1% (212 de 1174) |

Todos os valores da tabela foram medidos através da execução de `mvn test` com JaCoCo (saída do Surefire, `target/site/jacoco/jacoco.csv` e contagem de linhas não vazias sobre `src/main/java` e `src/test/java`), com exceção do rácio, que foi inferido a partir das duas contagens de linhas (1321 / 7375).

### 5.2 Cobertura por pacote

O bundle do JaCoCo contém 139 classes. A tabela mostra a cobertura de linhas, de ramos e de métodos por pacote, tal como foi medida e agregada a partir de `jacoco.csv`. Os nomes dos pacotes são relativos a `pt.psoft.g1.psoftg1`.

| Pacote | Cobertura de linhas | Cobertura de branch | Cobertura de métodos |
|---|---|---|---|
| `configuration` | 99,0% (101/102) | n/d | 91,7% (22/24) |
| `genremanagement.model` | 93,3% (14/15) | 100,0% (6/6) | 80,0% (4/5) |
| `lendingmanagement.model` | 84,0% (89/106) | 70,0% (21/30) | 80,6% (25/31) |
| `bookmanagement.model` | 74,1% (80/108) | 55,4% (41/74) | 72,7% (24/33) |
| `authormanagement.model` | 65,9% (29/44) | 43,8% (7/16) | 66,7% (10/15) |
| `shared.model` | 65,4% (34/52) | 44,4% (8/18) | 66,7% (16/24) |
| `readermanagement.model` | 60,5% (69/114) | 40,9% (18/44) | 70,3% (26/37) |
| `usermanagement.model` | 50,8% (32/63) | 44,4% (8/18) | 38,2% (13/34) |
| `lendingmanagement.services` | 39,5% (32/81) | 6,4% (7/110) | 22,4% (11/49) |
| `lendingmanagement.infrastructure.repositories.impl` | 35,9% (14/39) | 0,0% (0/12) | 66,7% (2/3) |
| `shared.services` | 15,5% (15/97) | 1,4% (1/70) | 27,3% (12/44) |
| `authormanagement.services` | 13,2% (9/68) | 0,0% (0/96) | 20,5% (9/44) |
| `usermanagement.api` | 7,0% (3/43) | 0,0% (0/44) | 13,0% (3/23) |
| `external.service` | 6,2% (1/16) | 0,0% (0/38) | 6,2% (1/16) |
| `readermanagement.infraestructure.repositories.impl` | 4,5% (1/22) | 0,0% (0/8) | 50,0% (1/2) |
| `lendingmanagement.api` | 4,4% (5/113) | 0,0% (0/168) | 7,1% (5/70) |
| `bookmanagement.infrastructure.repositories.impl` | 4,3% (1/23) | 0,0% (0/6) | 50,0% (1/2) |
| `usermanagement.infrastructure.repositories.impl` | 5,3% (1/19) | 0,0% (0/6) | 25,0% (1/4) |
| `exceptions` | 4,9% (4/81) | 0,0% (0/22) | 5,9% (2/34) |
| `auth.api` | 4,5% (1/22) | 0,0% (0/22) | 7,7% (1/13) |
| `usermanagement.services` | 3,0% (3/100) | 0,0% (0/151) | 5,2% (3/58) |
| `genremanagement.api` | 2,8% (3/107) | 0,0% (0/152) | 4,7% (3/64) |
| `readermanagement.services` | 1,9% (3/158) | 0,0% (0/314) | 2,7% (3/110) |
| `shared.api` | 1,9% (1/53) | 0,0% (0/22) | 5,0% (1/20) |
| `authormanagement.api` | 1,8% (3/164) | 0,0% (0/168) | 4,2% (3/71) |
| `bookmanagement.api` | 1,8% (3/164) | 0,0% (0/176) | 4,2% (3/71) |
| `genremanagement.services` | 1,8% (1/55) | 0,0% (0/58) | 2,3% (1/43) |
| `readermanagement.api` | 1,2% (3/246) | 0,0% (0/282) | 2,8% (3/107) |
| `genremanagement.infrastructure.repositories.impl` | 1,0% (1/96) | 0,0% (0/8) | 11,1% (1/9) |
| `bookmanagement.services` | 0,7% (1/138) | 0,0% (0/242) | 1,1% (1/88) |
| `bootstrapping` | 0,0% (0/354) | 0,0% (0/198) | 0,0% (0/22) |
| `usermanagement.repositories` | 0,0% (0/2) | n/d | 0,0% (0/2) |
| pacote raiz `pt.psoft.g1.psoftg1` | 33,3% (1/3) | n/d | 50,0% (1/2) |

Quanto à cobertura de linhas, as áreas mais cobertas são `configuration` (99,0%), `genremanagement.model` (93,3%) e `lendingmanagement.model` (84,0%). As áreas menos cobertas são `bootstrapping` e `usermanagement.repositories` (0,0%), seguidas de `bookmanagement.services` (0,7%), `genremanagement.infrastructure.repositories.impl` (1,0%) e `readermanagement.api` (1,2%).

### 5.3 Interpretação

A cobertura concentra-se no modelo de domínio. Os pacotes `model` de géneros, empréstimos, livros, autores, leitores e tipos partilhados têm cobertura de linhas entre 50,8% e 93,3%, o que corresponde aos 89 testes unitários associados. O pacote `configuration` atinge 99,0% apenas porque os testes `@SpringBootTest` carregam o contexto da aplicação Spring, uma vez que nenhuma classe de teste visa diretamente as classes de configuração (inferido).

Os serviços de aplicação, os controladores REST e as implementações de persistência estão quase sem testes. Todos os pacotes `api` têm cobertura de linhas abaixo de 8%, e todos os pacotes `services` estão abaixo de 14%, exceto `lendingmanagement.services` (39,5%) e `shared.services` (15,5%). As camadas de serviços e de controladores de livros, géneros, leitores e utilizadores praticamente não têm cobertura, e as classes de bootstrapping, que carregam dados iniciais sob o perfil `bootstrap`, o qual não está ativo nos testes, estão a 0,0%. A cobertura global de ramos de 4,5% mostra que a lógica condicional fora das classes de modelo é quase nunca executada. Os únicos testes dirigidos às camadas REST e de segurança (`TestAuthApi` e o `AuthorControllerIntegrationTest` vazio) não executam qualquer código (ver Secção 6).

## 6. Classificação dos testes

A classificação contabiliza os 102 testes que foram executados. Os testes que se encontram em código comentado não são contabilizados, uma vez que não são compilados nem executados. Isto aplica-se aos quatro testes de `TestAuthApi` e a três das seis anotações `@Test` de `LendingServiceImplTest`, que surgem dentro de comentários de bloco.

Cada teste é caracterizado segundo o nível (unitário, integração, sistema, aceitação), a abordagem de acesso ao código (caixa opaca ou caixa transparente) e o atributo de qualidade testado (funcional ou não funcional). A abordagem é indicada com a terminologia opaca e transparente, seguida do termo equivalente em inglês.

| Nível de granularidade | Abordagem de teste | Tipo de teste | Número de testes | Percentagem do total | Teste de exemplo |
|---|---|---|---|---|---|
| Unitário | Caixa transparente (white box) | Funcional | 89 | 87,3% | `src/test/java/pt/psoft/g1/psoftg1/bookmanagement/model/IsbnTest.java`, `ensureChecksum13IsCorrect` |
| Componente ou módulo | Não aplicável | Não aplicável | 0 | 0,0% | Não existe |
| Integração (sem doubles) | Caixa opaca (black box) | Funcional | 12 | 11,8% | `src/test/java/pt/psoft/g1/psoftg1/lendingmanagement/repositories/LendingRepositoryIntegrationTest.java` |
| Híbrido (contexto Spring com repositório simulado) | Caixa opaca (black box) | Funcional | 1 | 1,0% | `AuthorServiceImplIntegrationTest.whenValidId_thenAuthorShouldBeFound` |
| Contrato | Não aplicável | Não aplicável | 0 | 0,0% | Não existe |
| Sistema ou end-to-end | Caixa opaca (black box) | Funcional | 0 | 0,0% | Não existe (`TestAuthApi` está comentado) |
| Aceitação | Caixa opaca (black box) | Funcional | 0 | 0,0% | Não existe |
| Desempenho ou carga | Caixa opaca (black box) | Não funcional | 0 | 0,0% | Não existe |
| Segurança | Caixa opaca (black box) | Não funcional | 0 | 0,0% | Não existe |

Os testes unitários comprovam o comportamento das classes de domínio através da API pública, sem contexto Spring. A classificação como caixa transparente baseia-se no facto de estes testes terem sido escritos com conhecimento da implementação das classes, em particular das regras de validação e dos limites que cada construtor aplica, e de verificarem esse comportamento interno (condições de erro e valores-fronteira). É uma decisão de julgamento, porque os testes só observam valores devolvidos e exceções lançadas e poderiam ser lidos como caixa opaca. Os testes de integração foram classificados como caixa opaca, porque acedem aos serviços e repositórios apenas através das suas interfaces públicas e verificam os resultados obtidos, sem inspecionar a implementação. A unidade considerada é uma classe ou agregado, e nem todos os testes isolam os colaboradores com doubles: `LendingTest` e `BookTest` usam objetos reais de `Book`, `ReaderDetails`, `Author` e `Genre`. Não existem testes unitários de Service ou de Controller com colaboradores substituídos por doubles. As técnicas de desenho presentes são de exemplo, com casos válidos e inválidos (`IsbnTest` com ISBN-10 e ISBN-13 válidos e com checksum inválido) e verificação de limites de comprimento (`DescriptionTest`).

Os testes de integração sem doubles são `LendingServiceImplTest` (3), `LendingRepositoryIntegrationTest` (7), `AuthorRepositoryIntegrationTest` (1) e `PsoftG1ApplicationTests` (1, de arranque do contexto), e usam a base de dados H2 em memória como fake. `AuthorServiceImplIntegrationTest` (1) arranca o contexto Spring mas substitui o repositório por um double, pelo que não cumpre a definição de integração sem doubles e foi contabilizado numa linha própria.

Todos os testes são automáticos, executados localmente com `mvn test` e em ambiente de desenvolvimento, com dados sintéticos criados nos próprios testes.

## 7. Eficácia dos testes

A qualidade das asserções varia entre as classes de teste. Os testes de modelo verificam tipos de exceção (`assertThrows(IllegalArgumentException.class, ...)` em `IsbnTest` e `LendingTest`) e valores exatos (`assertEquals("9782826012092", isbn.toString())`). Noutros testes, a verificação é mínima ou inexistente. `PsoftG1ApplicationTests.contextLoads` contém apenas `assertTrue(true)`, e só falha se o contexto da aplicação não arrancar. Em `AuthorServiceImplIntegrationTest.whenValidId_thenAuthorShouldBeFound`, a asserção está dentro de `found.ifPresent(...)`, pelo que não é executada quando não é devolvido nenhum autor. O repositório simulado define o comportamento de `searchByNameName`, mas o teste chama `findByAuthorNumber`. Em `LendingTest`, os campos `lendingDurationInDays` e `fineValuePerDayInCents` estão anotados com `@Value`, mas a classe corre sem contexto Spring, pelo que ficam com o valor 0 em vez dos 15 dias e dos 200 cêntimos configurados.

A suite inclui testes desativados. `TestAuthApi` está totalmente comentado, `AuthorControllerIntegrationTest` declara uma fatia Spring MVC e um método de preparação mas não tem nenhum método de teste, e três das seis anotações `@Test` de `LendingServiceImplTest` encontram-se dentro de comentários de bloco. Nenhum endpoint REST é verificado por testes automáticos. Os testes comentados não são contabilizados pelo executor de testes como ignorados, que reporta 0 testes ignorados.

Não é utilizado mutation testing. O `pom.xml` não configura nenhuma ferramenta para o efeito (PIT ou equivalente), pelo que não existe mutation score. A cobertura global é de 19,5% de linhas e de 4,5% de ramos (Secção 5.1).

Os casos limite e os caminhos de erro são tratados sobretudo nas classes de modelo. `DescriptionTest` usa um texto longo para verificar o limite de comprimento, `IsbnTest` cobre falhas de checksum em ISBN-10 e em ISBN-13, e `LendingTest` cobre argumentos nulos de livro e de leitor. Nos serviços e nos controladores, os caminhos de erro estão apenas parcialmente testados: `LendingServiceImplTest` verifica `LendingForbiddenException` e `StaleObjectStateException`, mas parte da classe está comentada, e a cobertura dos pacotes de serviços (Secção 5.2) indica que a maioria dos ramos de falha não é executada.

O isolamento e o determinismo dos testes são desiguais. Os testes de modelo são isolados e rápidos, com tempos por classe entre 0,003 s e 1,2 s. Os testes de integração usam uma base de dados H2 em memória por contexto Spring, e `LendingServiceImplTest` é a classe mais lenta (8,9 s) por incluir o arranque do contexto. `LendingServiceImplTest` constrói números de empréstimo a partir de `LocalDate.now().getYear()`, pelo que os valores esperados dependem da data de execução, e o `TestAuthApi` comentado cria nomes de utilizador a partir de `System.currentTimeMillis()`. Não existe tratamento de testes instáveis (flaky) e a execução medida terminou sem falhas, erros nem testes ignorados.

Nenhum teste está associado a um defeito ou issue concreto: o código de teste não contém referências a issues, bugs ou regressões. Os test doubles são pouco utilizados, com um `@MockBean` (stub do repositório) e referências ao Mockito em 14 linhas do código de teste, e a maioria das camadas da aplicação não tem testes.

## 8. Indicadores de qualidade de software

A análise estática não está configurada. O `pom.xml` não declara nenhum linter nem verificador de estilo (por exemplo Checkstyle, PMD ou SpotBugs) e não existe configuração de nenhuma plataforma de qualidade de código (`sonar-project.properties`, Codacy ou CodeClimate), pelo que não está definido nenhum quality gate.

As dependências são geridas com Maven e com a gestão de dependências do Spring Boot na versão 3.2.5, e algumas versões são fixadas explicitamente (springdoc 2.3.0, MapStruct 1.5.5.Final e OWASP sanitizer 20220608.1). A atualidade das dependências não foi avaliada, porque não está configurada nenhuma ferramenta de verificação de versões (por exemplo uma configuração do Dependabot) e nenhuma foi executada.

A complexidade ciclomática foi medida com o JaCoCo: a soma da complexidade dos métodos das 139 classes analisadas é 2464, para 1174 métodos, o que dá uma média de cerca de 2,1 por método (valor inferido a partir de valores medidos). Os maiores ficheiros de produção, por número de linhas, são `bootstrapping/Bootstrapper.java` (536 linhas), `readermanagement/api/ReaderController.java` (352), `bootstrapping/UserBootstrapper.java` (271), `lendingmanagement/model/Lending.java` (261) e `bookmanagement/api/BookController.java` (243). A duplicação de código não foi medida, porque não está configurada nenhuma ferramenta para o efeito. Em `src/main` existe uma ocorrência de `TODO` ou `FIXME`.

O repositório não contém configuração de práticas de revisão de código: não existem `CODEOWNERS`, template de pull request, template de issues nem configuração de proteção de ramos. O histórico do controlo de versões tem um commit (`82d05ce`, com a mensagem "Initial Commit"), um autor e um ramo (`main`), e o repositório tem 400 ficheiros versionados (medido com `git ls-files`).

A documentação é composta pelo `HELP.md` gerado pelo Spring (22 linhas, sem conteúdo específico do projeto), pelos documentos de análise e desenho em `Docs/Phase1` e `Docs/Phase2` (requisitos, diagramas de sequência de sistema, diagramas de sequência, diagramas de classes, modelo de domínio, glossário e mapeamento REST, em Markdown e PlantUML), por uma coleção Postman (`Docs/Psoft-G1.postman_collection.json`) e pela documentação OpenAPI que o springdoc gera em `/api-docs` e `/swagger-ui` (configurada, mas não executada). Não existe ficheiro `README`. A quantidade de comentários e de Javadoc no código não foi quantificada.

## 9. Conclusão

### Implementação e deployment
O sistema é uma aplicação Spring Boot 3.2.5 em Java 17, de módulo único, construída com Maven, com base de dados H2, autenticação por JSON Web Token e um domínio organizado por área de negócio. Não existe pipeline de CI/CD, definição de contentores nem infraestrutura como código. O build e o deployment são manuais e locais, a configuração e as credenciais estão guardadas em ficheiros versionados em plain text e o versionamento limita-se a uma versão snapshot.

### Práticas de build e de teste
O build é feito localmente com o Maven e os testes são executados com `mvn test`, com as predefinições do Surefire e sem paralelismo, tratamento de testes instáveis, hooks nem ferramenta de cobertura. Os testes usam JUnit 5 com suporte de teste do Spring Boot, AssertJ e Mockito, estão em pacotes que espelham o código de produção e usam uma base de dados H2 em memória, com dados criados nos próprios testes.

### Quantidade, cobertura e eficácia dos testes
A suite tem 102 testes executados em 19 classes, todos aprovados, com cerca de 13,2 s de tempo de teste. A cobertura global é de 19,5% de linhas, 4,5% de ramos e 18,1% de métodos, e o rácio entre código de teste e código de produção é 0,18. A cobertura concentra-se nos pacotes de modelo de domínio, enquanto controladores, serviços (exceto parte do serviço de empréstimos) e código de bootstrapping quase não são executados. Dos testes, 89 são unitários (caixa transparente) e 13 são de integração (caixa opaca, um deles híbrido), e não existem testes de sistema, de aceitação, de contrato, de desempenho nem de segurança. As asserções dos testes de modelo são específicas, mas vários outros testes verificam pouco (`assertTrue(true)`, uma asserção condicional, uma classe de teste sem métodos), e uma classe de testes de autenticação e partes de uma classe de testes de serviço estão desativadas por comentários. Não existe mutation testing nem testes ligados a defeitos concretos.

### Indicadores de qualidade
Não existe análise estática, linter, plataforma de qualidade de código nem quality gate. A complexidade ciclomática média é de cerca de 2,1 por método, os maiores ficheiros são classes de bootstrapping e de controladores, a documentação consiste sobretudo em documentos de análise e desenho em `Docs` e na documentação OpenAPI gerada, sem `README`, e o histórico tem um único commit de um único autor.