package fr.uga.l3miage.library.books;

import fr.uga.l3miage.library.authors.AuthorDTO;
import jakarta.persistence.*;

import java.util.Collection;

public record BookDTO(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        Long id,
        String title,
        long isbn,
        String publisher,
        short year,
        String language,
        Collection<AuthorDTO> authors
) {
}
