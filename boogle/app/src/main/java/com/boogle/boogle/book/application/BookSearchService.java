package com.boogle.boogle.book.application;

import com.boogle.boogle.book.api.dto.BookSearchRequest;
import com.boogle.boogle.book.api.dto.BookSearchResponse;

import java.util.List;

public interface BookSearchService {

    List<BookSearchResponse> search(BookSearchRequest request);

}
