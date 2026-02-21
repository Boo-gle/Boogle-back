package com.boogle.boogle.book.api;

import com.boogle.boogle.book.api.dto.BookSearchRequest;
import com.boogle.boogle.book.api.dto.BookSearchResponse;
import com.boogle.boogle.book.application.BookSearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/search") // DB LIKE & ES 검색 공용
@RequiredArgsConstructor
public class BookSearchController {

    private final BookSearchService bookSearchService;

    /**
     * 통합 도서 검색 API
     * @param request 검색어
     * @return 프로파일 설정(db/es)에 따른 검색 결과 리스트
     */
    @GetMapping
    public Page<BookSearchResponse> search(
            @Valid
            @ModelAttribute
            BookSearchRequest request
    ) {
        return bookSearchService.search(request);
    }

}
