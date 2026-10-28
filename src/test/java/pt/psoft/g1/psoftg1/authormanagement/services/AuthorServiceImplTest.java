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
