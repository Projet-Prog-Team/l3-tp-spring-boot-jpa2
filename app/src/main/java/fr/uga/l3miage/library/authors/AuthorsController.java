package fr.uga.l3miage.library.authors;

import fr.uga.l3miage.data.domain.Author;
import fr.uga.l3miage.data.domain.Book;
import fr.uga.l3miage.library.books.BookDTO;
import fr.uga.l3miage.library.books.BooksMapper;
import fr.uga.l3miage.library.service.AuthorService;
import fr.uga.l3miage.library.service.DeleteAuthorException;
import fr.uga.l3miage.library.service.EntityNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

@RestController
@RequestMapping(value = "/api/v1", produces = "application/json")
public class AuthorsController {

    private final AuthorService authorService;
    private final AuthorMapper authorMapper;
    private final BooksMapper booksMapper;

    @Autowired
    public AuthorsController(AuthorService authorService, AuthorMapper authorMapper, BooksMapper booksMapper) {
        this.authorService = authorService;
        this.authorMapper = authorMapper;
        this.booksMapper = booksMapper;
    }

    @GetMapping("/authors")
    public Collection<AuthorDTO> authors(@RequestParam(value = "q", required = false) String query) {
        Collection<Author> authors;
        if (query == null) {
            authors = authorService.list();
        } else {
            authors = authorService.searchByName(query);
        }
        return authors.stream()
                .map(authorMapper::entityToDTO)
                .toList();
    }

    @GetMapping("/authors/{id}")
    public AuthorDTO author(@PathVariable("id") Long id) throws EntityNotFoundException {
        try{
            Author auteur = authorService.get(id);
            return authorMapper.entityToDTO(auteur);
        }
        catch(EntityNotFoundException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Auteur non trouvé.", e);
        }
    }

    @PostMapping("/authors")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthorDTO newAuthor(@RequestBody AuthorDTO authorDTO) throws EntityNotFoundException {
        Author author = authorMapper.dtoToEntity(authorDTO);
        try{
            Author authorDoublon = authorService.get(author.getId());
        } //si on arrive dans le catch, c'est que l'auteur n'existe pas encore, alors on peut l'insérer.
        catch(EntityNotFoundException e){
            if (author.getFullName() == null || author.getFullName().trim() == ""){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom de l'auteur ne peut pas être vide.");
            }
            author.setId(authorDTO.id());
            authorService.save(author);
            return authorMapper.entityToDTO(author);
        } //sinon, on renvoie une exception
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un auteur avec cet id existe déjà.");
    }

    @PutMapping("/authors/{authorId}")
    public AuthorDTO updateAuthor(@RequestBody AuthorDTO authorDTO, @PathVariable("authorId") Long id) throws EntityNotFoundException {
        Author author = authorMapper.dtoToEntity(authorDTO);
        try{
            Author auteur = authorService.get(id);
        } 
        catch(EntityNotFoundException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "L'auteur à remplacer n'existe pas.");
        }
        // attention AuthorDTO.id() doit être égale à id, sinon la requête utilisateur est mauvaise
        if (id == authorDTO.id()){
            Author authorUpdated = authorService.update(author);
            return authorMapper.entityToDTO(authorUpdated);
        }
        else{
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'ancien et le nouvel auteur n'ont pas le même id.");
        }
        
    }

    @DeleteMapping("/authors/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAuthor(@PathVariable("id") Long id) throws EntityNotFoundException, DeleteAuthorException{
        try{
            Author auteur = authorService.get(id);
        } 
        catch(EntityNotFoundException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "L'auteur à remplacer n'existe pas.");
        }
        Author auteur = authorService.get(id);
        Set<Book> books = auteur.getBooks();
        Collection<BookDTO> booksDTO = booksMapper.entityToDTO(books);
        boolean canBeDeleted = true;
        if (booksDTO != null){
            for (BookDTO bookDTO : booksDTO) {
                Collection<AuthorDTO> auteurs = bookDTO.authors();
                if(auteurs.size()>1){
                    canBeDeleted = false; //ne peut pas être effacé si co-auteur
                }
            }
        }   
        if(canBeDeleted){
            authorService.delete(id);
        }
        else{
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'auteur ne peut pas être effacé car il est co-auteur d'au moins un livre.");
        }

    }
    @GetMapping("/authors/{authorId}/books")
    public Collection<BookDTO> books(@PathVariable("authorId") Long authorId) throws EntityNotFoundException {
        try{
            Author auteur = authorService.get(authorId);
        } 
        catch(EntityNotFoundException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "L'auteur n'existe pas.");
        }
        Author auteur = authorService.get(authorId);
        Set<Book> books = auteur.getBooks();
        Collection<BookDTO> booksDTO = booksMapper.entityToDTO(books);
        return booksDTO; //exception si null ?
    }

}
