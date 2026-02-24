package com.boogle.boogle.book.infra;

import com.boogle.boogle.book.api.dto.BookSearchRequest;
import com.boogle.boogle.book.domain.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookRepositoryCustom {

    Page<Book> searchBooks(BookSearchRequest request, Pageable pageable);
}
