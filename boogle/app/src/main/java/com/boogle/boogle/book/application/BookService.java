package com.boogle.boogle.book.application;

import com.boogle.boogle.book.domain.Book;
import com.boogle.boogle.book.infra.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;

    public List<Book> searchByKeyword(String keyword) {
        return bookRepository.searchByKeyword(keyword);
    }

}
