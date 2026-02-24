package com.boogle.boogle.book.application;

import com.boogle.boogle.book.api.dto.AggregationResponse;
import com.boogle.boogle.book.api.dto.SuggestionResponse;
import com.boogle.boogle.book.domain.Book;
import com.boogle.boogle.book.infra.BookRepository;
import com.boogle.boogle.book.api.dto.BookSearchRequest;
import com.boogle.boogle.book.api.dto.BookSearchResponse;
import com.boogle.boogle.search.application.LowQualityKeywordDailyService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "search.es.enabled", havingValue = "false", matchIfMissing = true)
public class BookSearchDbImpl implements BookSearchService {

    private final BookRepository bookRepository;

    private final LowQualityKeywordDailyService lowQualityKeywordDailyService;

    private static final Logger searchLogger = LoggerFactory.getLogger("SEARCH_FAILURE");

    @Override
    public Page<BookSearchResponse> search(BookSearchRequest request) {

        PageRequest pageRequest = PageRequest.of(request.page(), request.size());
        String keyword = request.keyword().trim();
        Page<Book> bookPage = bookRepository.searchBooks(request, pageRequest);

        long totalHits = bookPage.getTotalElements();
        boolean isLowQuality = (totalHits == 0) || (totalHits < 3);

        if (bookPage.isEmpty()) {
            String keyword = request.keyword().trim();
            long totalHits = bookPage.getTotalElements();

            // 로그 로직
            searchLogger.warn(
                    "event=SEARCH_FAIL keyword={} totalHits={} source=DB",
                    keyword, totalHits
            );

            // 저품질 로직
            lowQualityKeywordDailyService.recordLowQualityKeyword(keyword, null);
        }

        return bookPage.map(book -> BookSearchResponse.builder()
                .id(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .publisher(book.getPublisher())
                .thumbnailUrl(book.getThumbnailUrl())
                .price(book.getPrice())
                .description(book.getDescription())
                .categoryDepth2(book.getCategory().getCategoryDepth2())
                .isbn(book.getStandardId())
                .publishedDate(book.getPublishedDate() != null ? book.getPublishedDate().toString() : null)
                .mallType(book.getCategory().getMallType())
                .productType(book.getProductType() != null ? book.getProductType().name() : null)
                .build());
    }

    @Override
    public List<SuggestionResponse> getSuggestions(String keyword) {
        return List.of();
    }

    @Override
    public AggregationResponse getCategoryAggregations(String keyword) {
        return new AggregationResponse(List.of());
    }

}
