package com.boogle.boogle.book.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // null인 필드는 json 응답에서 제외
public record BookSearchResponse(
        Long id,
        String title,
        String highlightTitle, // ES 검색 강조된 제목
        String author,
        String publisher,
        String thumbnailUrl,
        Integer price,
        Double score, // ES 검색 가중치 점수
        String categoryDepth1
) {

    // DB LIKE & ES 결과 모두 응답함

}
