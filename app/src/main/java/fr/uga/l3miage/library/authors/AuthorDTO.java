package fr.uga.l3miage.library.authors;

import jakarta.persistence.*;

public record AuthorDTO(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        Long id,
        String fullName
) {
}
