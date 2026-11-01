package pt.psoft.g1.psoftg1.authormanagement.services;

import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.shared.repositories.PhotoRepository;

/**
 * Granularidade: unitário (caixa transparente).
 * SUT: {@link AuthorServiceImpl}.
 * Técnica: cobertura de ramos, a partir da estrutura do código de {@code partialUpdate}.
 * Doubles: mocks Mockito de {@code AuthorRepository}, {@code BookRepository}, {@code AuthorMapper}
 * e {@code PhotoRepository}.
 */
@ExtendWith(MockitoExtension.class)
class AuthorServiceImplTest {
    @Mock
    private AuthorRepository authorRepository;
    @Mock
    private BookRepository bookRepository;
    @Mock
    private AuthorMapper mapper;
    @Mock
    private PhotoRepository photoRepository;

    @InjectMocks
    private AuthorServiceImpl authorService;

    /**
     * Granularidade: unitário de caixa transparente.
     * SUT: {@link AuthorServiceImpl#partialUpdate}.
     * Técnica: cobertura de ramos (ramo do {@code orElseThrow} quando o autor não existe).
     * Doubles: mock de {@code AuthorRepository} a devolver {@code Optional.empty()}.
     */
    @Test
    void shouldThrowNotFoundExceptionWhenAuthorDoesNotExist() {
        // Arrange
        when(authorRepository.findByAuthorNumber(1L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(NotFoundException.class,
                () -> authorService.partialUpdate(1L, new UpdateAuthorRequest(), 0L));
    }
}
