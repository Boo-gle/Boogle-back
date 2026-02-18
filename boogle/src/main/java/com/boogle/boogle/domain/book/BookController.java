package com.boogle.boogle.domain.book;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @GetMapping("/api/books/db")
    public List<Book> searchBooks(@RequestParam("keyword") String keyword) {
        if(keyword == null || keyword.isBlank()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "keyword는 공백일 수 없습니다.");
        }
        return bookService.searchByKeyword(keyword.trim());
    }

}
