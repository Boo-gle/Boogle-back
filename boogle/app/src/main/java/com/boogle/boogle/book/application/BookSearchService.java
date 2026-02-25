package com.boogle.boogle.book.application;

import com.boogle.boogle.book.api.dto.AggregationResponse;
import com.boogle.boogle.book.api.dto.BookSearchRequest;
import com.boogle.boogle.book.api.dto.BookSearchResponse;
import com.boogle.boogle.book.api.dto.SuggestionResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface BookSearchService {

    Page<BookSearchResponse> search(BookSearchRequest request);

    List<SuggestionResponse> getSuggestions(String keyword);

    AggregationResponse getCategoryAggregations(String keyword);
}
