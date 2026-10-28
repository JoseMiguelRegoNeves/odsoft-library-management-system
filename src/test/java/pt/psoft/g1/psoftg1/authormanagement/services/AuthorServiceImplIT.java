package pt.psoft.g1.psoftg1.authormanagement.services;

import org.hibernate.StaleObjectStateException;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Granularidade: Integração.
 * SUT: {@link AuthorService}
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class AuthorServiceImplIT {
    @Autowired
    private AuthorService authorService;
    @MockBean
    private AuthorRepository authorRepository;

    @BeforeEach
    public void setUp() {
        Author alex = new Author("Alex", "O Alex escreveu livros", null);
        List<Author> list = new ArrayList<>();
        list.add(alex);

        Mockito.when(authorRepository.searchByNameName(alex.getName()))
                .thenReturn(list);
    }

    @Test
    public void whenValidId_thenAuthorShouldBeFound() {
        Long id = 1L;
        Optional<Author> found = authorService.findByAuthorNumber(id);
        found.ifPresent(author -> assertThat(author.getId())
                .isEqualTo(id));
    }

    /**
     * Granularidade: Integração de caixa opaca
     */

    @Test
    public void shouldReturnUpdatedAuthorWhenPartialUpdateHasOnlyName() {
        // Arrange
        Author existing = new Author("Alex", "O Alex escreveu livros", null);
        Mockito.when(authorRepository.findByAuthorNumber(1L)).thenReturn(Optional.of(existing));
        Mockito.when(authorRepository.save(Mockito.any(Author.class))).thenAnswer(i -> i.getArgument(0));
        UpdateAuthorRequest request = new UpdateAuthorRequest();
        request.setName("Alexandre");

        // Act
        Author updated = authorService.partialUpdate(1L, request, existing.getVersion());

        // Assert
        assertThat(updated.getName()).isEqualTo("Alexandre");
        assertThat(updated.getBio()).isEqualTo("O Alex escreveu livros");
    }

    /**
     * Granularidade: Integração de caixa transparente
     */

    @Test
    public void shouldNotSaveAuthorWhenVersionIsStale() {
        // Arrange
        Author existing = new Author("Alex", "O Alex escreveu livros", null);
        Mockito.when(authorRepository.findByAuthorNumber(1L)).thenReturn(Optional.of(existing));
        UpdateAuthorRequest request = new UpdateAuthorRequest();
        request.setName("Alexandre");

        // Act + Assert
        assertThrows(StaleObjectStateException.class,
                () -> authorService.partialUpdate(1L, request, 999L));
        Mockito.verify(authorRepository, Mockito.never()).save(Mockito.any(Author.class));
    }
}
