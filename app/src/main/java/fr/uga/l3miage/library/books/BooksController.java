package fr.uga.l3miage.library.books;

import fr.uga.l3miage.data.domain.Author;
import fr.uga.l3miage.data.domain.Book;
import fr.uga.l3miage.data.domain.Book.Language;
import fr.uga.l3miage.library.authors.AuthorDTO;
import fr.uga.l3miage.library.authors.AuthorMapper;
import fr.uga.l3miage.library.service.AuthorService;
import fr.uga.l3miage.library.service.BookService;
import fr.uga.l3miage.library.service.EntityNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@RestController
@RequestMapping(value = "/api/v1", produces = "application/json")
public class BooksController {

    private final BookService bookService;
    private final BooksMapper booksMapper;
    private final AuthorService authorService;
    private final AuthorMapper authorMapper;

    @Autowired
    public BooksController(BookService bookService, BooksMapper booksMapper, AuthorService authorService, AuthorMapper authorMapper) {
        this.bookService = bookService;
        this.booksMapper = booksMapper;
        this.authorService = authorService;
        this.authorMapper = authorMapper;
    }

    //retourner tous les livres
    @GetMapping("/books")
    public Collection<BookDTO> books(@RequestParam(value = "q", required = false) String query) {
        Collection<Book> books;
        if (query == null) {
            books = bookService.list();
        } else {
            books = bookService.findByTitle(query);
        }
        return books.stream()
                .map(booksMapper::entityToDTO)
                .toList();
    }

    //retourner un livre par son id
    @GetMapping("/books/{id}")
    public BookDTO book(@PathVariable("id") Long id) {
        try{
            Book book = bookService.get(id);
            return booksMapper.entityToDTO(book);
        }
        catch(EntityNotFoundException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Livre non trouvé.", e);
        }
    }
   
    //créer un nouveau livre d'un auteur
    @PostMapping("/authors/{authorId}/books")
    @ResponseStatus(HttpStatus.CREATED)
    public BookDTO newBook(@PathVariable("authorId") Long authorId, @RequestBody BookDTO bookDto) throws EntityNotFoundException {
        try{
            Author auteur = authorService.get(authorId);
        }
        catch(EntityNotFoundException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "L'auteur n'existe pas.", e);
        }

        //vérifie le titre
        if (bookDto.title() == null || bookDto.title().trim() == "" ){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le titre du livre ne peut pas être vide.");
        }

        //vérifie le isbn
        if (bookDto.isbn() < 100000000){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le code isbn du livre n'est pas correct.");
        }

        //vérifie la langue
        if (bookDto.language() != null){
            boolean correctLanguage = false;
            for (Language language : Language.values()) {
                if (language.name().equalsIgnoreCase(bookDto.language())) {
                    correctLanguage = true;
                    break;
                }
            }
            if (!correctLanguage){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La langue du livre est mauvaise.");
            }
        }

        //vérifie l'année. On considère qu'on peut avoir des dates négatives pour les -JC
        if ( bookDto.year() > Calendar.getInstance().get(Calendar.YEAR)){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'année de sortie du livre n'est pas correcte");
        }

        //on rajoute l'(les) auteur(s) du livre créé (un livre doit obligatoirement avoir au moins un auteur)
        Author auteur = authorService.get(authorId);
        Set<Author> authorSet = new HashSet<>();
        authorSet.add(auteur);
        Book book = booksMapper.dtoToEntity(bookDto);

        book.setId(bookDto.id());
        book.setAuthors(authorSet);

        bookService.save(authorId, book);
        return booksMapper.entityToDTO(book);
    }
    
    //mettre à jour un livre
    @PutMapping("/books/{bookId}")
    public BookDTO updateBook(@PathVariable Long bookId, @RequestBody BookDTO book) throws EntityNotFoundException {
        try{ //vérification de l'existence du livre à modifier
            Book bookOrigine = bookService.get(bookId);
        }
        catch(EntityNotFoundException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Le livre n'existe pas.", e);
        }
        
        if (bookId == book.id()){
            Book bookOrigine = bookService.get(bookId);
            Set<Author> auteursLivreOrigine = bookOrigine.getAuthors();
            //on remet l'(les) même(s) auteur(s) du livre original au livre modifié
            Book modifiedMook = booksMapper.dtoToEntity(book);
            modifiedMook.setAuthors(auteursLivreOrigine);

            Book updatedBook = bookService.update(modifiedMook);
            return booksMapper.entityToDTO(updatedBook);
        }
        else{
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'ancien et le nouvel auteur n'ont pas le même id.");
        }
    }

    //effacer un livre
    @DeleteMapping("/books/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBook(@PathVariable Long id) throws EntityNotFoundException {
        try{ //vérification de l'existence du livre à supprimer
            Book bookTest = bookService.get(id);
        }
        catch(EntityNotFoundException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Le livre n'existe pas.", e);
        }
        bookService.delete(id);
    }

    //ajouter un co-auteur
    @PutMapping("/books/{bookId}/authors")
    public void addAuthor(@PathVariable Long bookId, @RequestBody AuthorDTO author) throws EntityNotFoundException {
        try{ //vérification de l'existence du livre auquel on doit ajouter l'auteur
            Book bookTest = bookService.get(bookId);
        }
        catch(EntityNotFoundException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Le livre n'existe pas.", e);
        }
        Book book = bookService.get(bookId);
        book.addAuthor(authorMapper.dtoToEntity(author));
        bookService.update(book);

    }
}
