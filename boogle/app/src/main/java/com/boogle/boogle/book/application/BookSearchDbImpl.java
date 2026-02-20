package com.boogle.boogle.book.application;

import com.boogle.boogle.book.domain.Book;
import com.boogle.boogle.book.infra.BookRepository;
import com.boogle.boogle.book.api.dto.BookSearchRequest;
import com.boogle.boogle.book.api.dto.BookSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Profile("db")
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookSearchDbImpl implements BookSearchService {

    private final BookRepository bookRepository;

    @Override
    public List<BookSearchResponse> search(BookSearchRequest request) {

        PageRequest pageRequest = PageRequest.of(request.page(), request.size());
        Page<Book> bookPage = bookRepository.searchByKeyword(request.keyword().trim(), pageRequest);


        return bookPage.stream()
                .map(book -> BookSearchResponse.builder()
                        .id(book.getId())
                        .title(book.getTitle())
                        .author(book.getAuthor())
                        .publisher(book.getPublisher())
                        .thumbnailUrl(book.getThumbnailUrl())
                        .price(book.getPrice())
                        .build())
                .toList();
    }

}
