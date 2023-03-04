package fr.uga.l3miage.library.books;

import fr.uga.l3miage.data.domain.Author;
import fr.uga.l3miage.data.domain.Book;
import fr.uga.l3miage.data.domain.Book.Language;
import fr.uga.l3miage.library.authors.AuthorDTO;
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

    @Autowired
    public BooksController(BookService bookService, BooksMapper booksMapper, AuthorService authorService) {
       this.bookService = bookService;
        this.booksMapper = booksMapper;
        this.authorService = authorService;
    }

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
   
    @PostMapping("/authors/{authorId}/books")
    @ResponseStatus(HttpStatus.CREATED)
    public BookDTO newBook(@PathVariable("authorId") Long authorId, @RequestBody BookDTO bookDto) throws EntityNotFoundException {
        //Book bookEntity = booksMapper.dtoToEntity(book);
        try{
            Author auteur = authorService.get(authorId);
        }
        catch(EntityNotFoundException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "L'auteur n'existe pas.", e);
        }
        if (bookDto.title() == null || bookDto.title().trim() == "" ){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le titre du livre ne peut pas être vide.");
        }

        //vérifier le isbn
        if (bookDto.isbn() < 100000000){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le code isbn du livre n'est pas correct.");
        }

        //vérifier la langue
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

        //vérifier l'année. On considère qu'on peut avoir des dates négatives pour les -JC
        if ( bookDto.year() > Calendar.getInstance().get(Calendar.YEAR)){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'année de sortie du livre n'est pas correcte");
        }
        Author auteur = authorService.get(authorId);
        Set<Author> authorSet = new HashSet<>();
        authorSet.add(auteur);
        Book book = booksMapper.dtoToEntity(bookDto);

        book.setId(bookDto.id());
        book.setAuthors(authorSet);

        bookService.save(authorId, book);
        return booksMapper.entityToDTO(book);
    }
    
    @PutMapping("/books/{bookId}")
    public BookDTO updateBook(@PathVariable Long bookId, @RequestBody BookDTO book) throws EntityNotFoundException {
        try{ //vérification de l'existence du livre à modifier
            Book bookTest = bookService.get(bookId);
        }
        catch(EntityNotFoundException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Le livre n'existe pas.", e);
        }
        
        if (bookId == book.id()){
            //booksMapper.dtoToEntity(book).getAuthors();
            Book updatedBook = bookService.update(booksMapper.dtoToEntity(book));
            return booksMapper.entityToDTO(updatedBook);
        }
        else{
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'ancien et le nouvel auteur n'ont pas le même id.");
        }
    }

    @DeleteMapping("/books/{id}")
    public void deleteBook(@PathVariable Long id) throws EntityNotFoundException {
        try{ //vérification de l'existence du livre à supprimer
            Book bookTest = bookService.get(id);
        }
        catch(EntityNotFoundException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Le livre n'existe pas.", e);
        }
        bookService.delete(id);
    }

    @PutMapping("/books/{bookId}/authors")
    public void addAuthor(@PathVariable Long bookId, @RequestBody AuthorDTO author) throws EntityNotFoundException {
        try{ //vérification de l'existence du livre auquel on doit ajouter l'auteur
            Book bookTest = bookService.get(bookId);
        }
        catch(EntityNotFoundException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Le livre n'existe pas.", e);
        }
        Book book = bookService.get(bookId);
        booksMapper.entityToDTO(book).authors().add(author);
        bookService.update(bookService.get(bookId));

    }
}
