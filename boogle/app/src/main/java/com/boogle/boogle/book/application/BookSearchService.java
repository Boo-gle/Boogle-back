package com.boogle.boogle.book.application;

import com.boogle.boogle.book.api.dto.BookSearchRequest;
import com.boogle.boogle.book.api.dto.BookSearchResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface BookSearchService {

    Page<BookSearchResponse> search(BookSearchRequest request);

}
