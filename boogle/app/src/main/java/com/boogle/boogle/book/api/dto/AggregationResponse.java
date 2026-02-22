package com.boogle.boogle.book.api.dto;

import java.util.List;

public record AggregationResponse(
        List<AggregationBucket> categories // 카테고리별 집계 결과
) {

    public record AggregationBucket(
            String category2,  // 예: "컴퓨터/IT", "소설", "10000원 이하"
            Long count   // 예: 42, 15
    ) {}
}
