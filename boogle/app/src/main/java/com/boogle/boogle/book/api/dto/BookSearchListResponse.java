package com.boogle.boogle.book.api.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record BookSearchListResponse(
        Page<BookSearchResponse> page, // 검색된 책 리스트 (페이징 포함)
        long totalCount,
        String originalKeyword,        // 사용자가 처음에 입력한 값 (예: "리악트")
        String correctedKeyword,       // 시스템이 제안/교정한 값 (예: "리액트")
        List<AggregationResponse.AggregationBucket> categories
) {
}
