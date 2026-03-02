package com.boogle.boogle.book.application;

import com.boogle.boogle.book.api.dto.*;
import org.springframework.data.domain.Page;

import java.util.List;

public interface BookSearchService {

    BookSearchListResponse search(BookSearchRequest request);

    List<SuggestionResponse> getSuggestions(String keyword);

//    AggregationResponse getCategoryAggregations(String keyword);
}
