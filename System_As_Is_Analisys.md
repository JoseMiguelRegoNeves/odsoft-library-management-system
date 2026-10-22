# System-As-Is Analisys

## 1. Introduction and scope

This document describes the current state of the repository `odsoft-library-management-system`, focusing on testing, build practices, deployment and software quality. It describes the system as it is and does not suggest changes. The analysis was carried out on 2026-10-22 on the commit `82d05ce5438a1fc4075d4853d298a7c0e593525e`, which is the only commit on the `main` branch.

The work followed four steps. First, the technology stack was identified from the build and configuration files and from the directory structure. Second, the repository was searched for continuous integration and continuous deployment (CI/CD) definitions and deployment descriptors. Third, the tests were located and classified by reading their sources. Fourth, the existing test suite was run and a coverage tool was attached to it from the command line. Each figure in the report is described as measured (obtained by running a command), read (taken from a file or report) or inferred (concluded from other evidence).

## 2. System overview

The system is a Java 17 web application named `psoft-g1` that implements the back end of a library management system and exposes a REST API. The information in this section was read from `pom.xml`. The project is built with Apache Maven and inherits from `spring-boot-starter-parent` version 3.2.5.

The system is a monolith made of a single Maven module. The code base has 144 main Java files and 23 test Java files. The production code in `src/main/java/pt/psoft/g1/psoftg1` is organised by business area, with the packages `authormanagement`, `bookmanagement`, `genremanagement`, `lendingmanagement`, `readermanagement`, `usermanagement`, `auth`, `shared`, `configuration`, `bootstrapping`, `exceptions` and `external`. Inside each business area, the sub-packages `api`, `model`, `services`, `repositories` and `infrastructure` (spelled `infraestructure` in `readermanagement`) separate controllers, domain model, services and persistence. The business parameters (lending duration of 15 days, fine of 200 cents per day, minimum reader age of 12 and suggestion limit per genre of 2) are set in `src/main/resources/config/library.properties`.

## 3. Implementation and deployment

The build is defined only by `pom.xml`, which configures the `spring-boot-maven-plugin` and the `maven-compiler-plugin` (release 17, with Lombok, MapStruct and the Lombok-MapStruct binding as annotation processors). The Spring Boot plugin packages the application as an executable archive. The repository contains the Maven wrapper scripts `mvnw` and `mvnw.cmd`, but the `.mvn` directory that the wrapper needs does not exist, so the wrapper could not be used. For this reason, Maven 3.9.9 was downloaded separately to run the measurements.

No CI/CD configuration was found. The repository defines no pipeline triggers, stages, quality gates, caching rules, build artefact publication or deployment targets.

Configuration is kept in `.properties` files. The file `src/main/resources/application.properties` contains the datasource credentials, the upload directory and a third-party API key as plain values, and the RSA key pair used to sign tokens is stored as plain files in `src/main/resources`. No external secret management (environment variables, vault or encrypted files) is configured. The test configuration in `src/test/resources/application.properties` uses an in-memory H2 database (`jdbc:h2:mem:testdb`), and `src/test/resources/application-integrationtest.properties` has a single line selecting `jdbc:h2:mem:test`. Apart from the Spring profile `bootstrap`, which loads seed data through the classes in `src/main/java/pt/psoft/g1/psoftg1/bootstrapping`, no separate environments (development, staging or production) are defined. Versioning is limited to the Maven version `0.0.1-SNAPSHOT`, and no release notes or release automation were found.

## 4. Build and testing practices

Tests are run locally through the standard Maven lifecycle (`mvn test`). The `pom.xml` has no entry for `maven-surefire-plugin`, so the defaults inherited from the Spring Boot parent apply. Parallel execution, rerunning of failed tests and other handling of flaky tests are not configured. No coverage plugin, static analysis plugin or test profile is declared. The tracked files contain no pre-commit hooks, and no `.githooks` directory, `husky` configuration or `pre-commit` configuration was found.

The tests use JUnit 5 (Jupiter), provided by `spring-boot-starter-test`, which also brings AssertJ, Hamcrest and Mockito, together with `spring-security-test`. The `pom.xml` also declares the older `junit:junit` artefact in test scope, but no test imports JUnit 4, since all of them import `org.junit.jupiter.api`. Tests that need a Spring context use `@SpringBootTest`, `@DataJpaTest` with `TestEntityManager` or `@WebMvcTest`, and the persistence tests run on the in-memory H2 database configured in `src/test/resources/application.properties`. Test data is created inside the tests themselves: `LendingServiceImplTest` and `LendingRepositoryIntegrationTest` create authors, books, readers and lendings in set-up methods and use `@Transactional`, while the model tests build the domain objects directly. The helper classes `testutils/UserTestDataFactory.java` and `testutils/JsonHelper.java` support the authentication test, which is commented out. The only test double is one `@MockBean` of `AuthorRepository` in `AuthorServiceImplIntegrationTest`, and no containers, fakes or stub servers are used.

The naming conventions are not uniform. Most classes end with `Test` (for example `IsbnTest` and `LendingTest`), the integration tests include `IntegrationTest` in the name (for example `LendingRepositoryIntegrationTest`) and one class starts with `Test` (`TestAuthApi`). The test packages mirror the production packages under `src/test/java/pt/psoft/g1/psoftg1`, with `model`, `services`, `repository` and `api` sub-packages. The method names in the model tests usually start with `ensure` (for example `ensureChecksum13IsCorrect`). Some test files contain comments that point to external tutorials, and two of them (`AuthorServiceImplIntegrationTest` and `AuthorControllerIntegrationTest`) state that they were adapted to JUnit 5 with the help of a generative assistant.

## 5. Test quantity and coverage

### 5.1 Summary indicators

| Indicator | Value |
|---|---|
| Total tests (passing / failing / skipped) | 102 (102 / 0 / 0) |
| Test classes executed (test files in `src/test/java`) | 19 (23) |
| Test execution time | 13.2 s (37.4 s total Maven build) |
| Production code (non-blank lines) | 7375 |
| Test code (non-blank lines) | 1321 |
| Ratio of test code to production code | 0.18 |
| Line coverage | 19.5% (558 of 2868) |
| Branch coverage | 4.5% (117 of 2579) |
| Method coverage | 18.1% (212 of 1174) |

All values in the table were measured by running `mvn test` with JaCoCo (Surefire output, `target/site/jacoco/jacoco.csv` and non-blank line counts over `src/main/java` and `src/test/java`).

### 5.2 Coverage by package

The JaCoCo bundle contains 139 classes. The table shows line, branch and method coverage by package, as measured and aggregated from `jacoco.csv`. The package names are relative to `pt.psoft.g1.psoftg1`.

| Package | Line coverage | Branch coverage | Method coverage |
|---|---|---|---|
| `configuration` | 99.0% (101/102) | n/a | 91.7% (22/24) |
| `genremanagement.model` | 93.3% (14/15) | 100.0% (6/6) | 80.0% (4/5) |
| `lendingmanagement.model` | 84.0% (89/106) | 70.0% (21/30) | 80.6% (25/31) |
| `bookmanagement.model` | 74.1% (80/108) | 55.4% (41/74) | 72.7% (24/33) |
| `authormanagement.model` | 65.9% (29/44) | 43.8% (7/16) | 66.7% (10/15) |
| `shared.model` | 65.4% (34/52) | 44.4% (8/18) | 66.7% (16/24) |
| `readermanagement.model` | 60.5% (69/114) | 40.9% (18/44) | 70.3% (26/37) |
| `usermanagement.model` | 50.8% (32/63) | 44.4% (8/18) | 38.2% (13/34) |
| `lendingmanagement.services` | 39.5% (32/81) | 6.4% (7/110) | 22.4% (11/49) |
| `lendingmanagement.infrastructure.repositories.impl` | 35.9% (14/39) | 0.0% (0/12) | 66.7% (2/3) |
| `shared.services` | 15.5% (15/97) | 1.4% (1/70) | 27.3% (12/44) |
| `authormanagement.services` | 13.2% (9/68) | 0.0% (0/96) | 20.5% (9/44) |
| `usermanagement.api` | 7.0% (3/43) | 0.0% (0/44) | 13.0% (3/23) |
| `external.service` | 6.2% (1/16) | 0.0% (0/38) | 6.2% (1/16) |
| `readermanagement.infraestructure.repositories.impl` | 4.5% (1/22) | 0.0% (0/8) | 50.0% (1/2) |
| `lendingmanagement.api` | 4.4% (5/113) | 0.0% (0/168) | 7.1% (5/70) |
| `bookmanagement.infrastructure.repositories.impl` | 4.3% (1/23) | 0.0% (0/6) | 50.0% (1/2) |
| `usermanagement.infrastructure.repositories.impl` | 5.3% (1/19) | 0.0% (0/6) | 25.0% (1/4) |
| `exceptions` | 4.9% (4/81) | 0.0% (0/22) | 5.9% (2/34) |
| `auth.api` | 4.5% (1/22) | 0.0% (0/22) | 7.7% (1/13) |
| `usermanagement.services` | 3.0% (3/100) | 0.0% (0/151) | 5.2% (3/58) |
| `genremanagement.api` | 2.8% (3/107) | 0.0% (0/152) | 4.7% (3/64) |
| `readermanagement.services` | 1.9% (3/158) | 0.0% (0/314) | 2.7% (3/110) |
| `shared.api` | 1.9% (1/53) | 0.0% (0/22) | 5.0% (1/20) |
| `authormanagement.api` | 1.8% (3/164) | 0.0% (0/168) | 4.2% (3/71) |
| `bookmanagement.api` | 1.8% (3/164) | 0.0% (0/176) | 4.2% (3/71) |
| `genremanagement.services` | 1.8% (1/55) | 0.0% (0/58) | 2.3% (1/43) |
| `readermanagement.api` | 1.2% (3/246) | 0.0% (0/282) | 2.8% (3/107) |
| `genremanagement.infrastructure.repositories.impl` | 1.0% (1/96) | 0.0% (0/8) | 11.1% (1/9) |
| `bookmanagement.services` | 0.7% (1/138) | 0.0% (0/242) | 1.1% (1/88) |
| `bootstrapping` | 0.0% (0/354) | 0.0% (0/198) | 0.0% (0/22) |
| `usermanagement.repositories` | 0.0% (0/2) | n/a | 0.0% (0/2) |
| root package `pt.psoft.g1.psoftg1` | 33.3% (1/3) | n/a | 50.0% (1/2) |

By line coverage, the best covered areas are `configuration` (99.0%), `genremanagement.model` (93.3%) and `lendingmanagement.model` (84.0%). The least covered areas are `bootstrapping` and `usermanagement.repositories` (0.0%), followed by `bookmanagement.services` (0.7%), `genremanagement.infrastructure.repositories.impl` (1.0%) and `readermanagement.api` (1.2%).

### 5.3 Interpretation

Coverage is concentrated in the domain model. The `model` packages for genres, lendings, books, authors, readers and shared types have line coverage between 50.8% and 93.3%, which matches the 89 unit tests aimed at them. The `configuration` package reaches 99.0% only because the `@SpringBootTest` tests load the Spring application context, since no test class targets the configuration classes directly (inferred).

Application services, REST controllers and persistence implementations are almost untested. Every `api` package has line coverage below 8%, and every `services` package is below 14%, except `lendingmanagement.services` (39.5%) and `shared.services` (15.5%). The service and controller layers of books, genres, readers and users have practically no coverage, and the bootstrapping classes, which seed data under the `bootstrap` profile that is not active in the tests, are at 0.0%. The overall branch coverage of 4.5% shows that the conditional logic outside the model classes is almost never executed. The only tests aimed at the REST and security layers (`TestAuthApi` and the empty `AuthorControllerIntegrationTest`) do not execute any code (see Section 6).

## 6. Test classification

The classification counts the 102 tests that were executed. Tests inside commented-out code are not counted, since they are not compiled or run. This applies to the four tests of `TestAuthApi` and to three of the six `@Test` annotations in `LendingServiceImplTest`, which appear inside block comments.

| Granularity level | Testing approach | Test type | Number of tests | Percentage of total | Example test |
|---|---|---|---|---|---|
| Unit | White box | Functional | 89 | 87.3% | `src/test/java/pt/psoft/g1/psoftg1/bookmanagement/model/IsbnTest.java`, `ensureChecksum13IsCorrect` |
| Integration | Grey box | Functional | 13 | 12.7% | `src/test/java/pt/psoft/g1/psoftg1/lendingmanagement/repositories/LendingRepositoryIntegrationTest.java` |

The unit row comprises the model test classes `BirthDateTest` (4), `PhoneNumberTest` (4), `ReaderTest` (9), `PhotoTest` (2), `NameTest` (6), `LendingTest` (14), `LendingNumberTest` (8), `TitleTest` (7), `BookTest` (6), `DescriptionTest` (4), `IsbnTest` (7), `GenreTest` (4), `BioTest` (5) and `AuthorTest` (9). The integration row comprises `LendingServiceImplTest` (3), `LendingRepositoryIntegrationTest` (7), `AuthorServiceImplIntegrationTest` (1), `AuthorRepositoryIntegrationTest` (1) and `PsoftG1ApplicationTests` (1).

## 7. Software quality indicators

Static analysis and linting are not configured. No configuration of a code quality platform (`sonar-project.properties`, Codacy or CodeClimate) exists, so no quality gate is defined.

Dependencies are managed with Maven and the Spring Boot dependency management of version 3.2.5, and some versions are fixed explicitly (springdoc 2.3.0, MapStruct 1.5.5.Final and OWASP sanitizer 20220608.1). Outdated or vulnerable dependencies were not assessed, because no scanner (for example OWASP Dependency-Check or a Dependabot configuration) is configured and none was run. The repository has no configuration for software composition analysis, static application security testing (SAST), dynamic application security testing (DAST) or secret scanning.