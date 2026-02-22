package com.boogle.boogle.book.api.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record BookSearchRequest( // 검색어랑 페이징 정보 요청
        @NotBlank(message = "검색어를 입력해주세요.")
         String keyword, // title, author, description, publisher
         String productType, // DOMESTIC, FOREIGN, 기본은 통합 검색
         String mallType, // 도서, 음반, dvd, ebook. 기본은 통합 검색
         List<String> searchConditions, // 검색 조건 - 도서명, 작가명, 출판사명
         String categoryDepth2, // 카데고리 - 단일 선택
         String priceRange, // 가격 - ~1만원, 10000-30000 등 문자열로 받아 파싱
         String dateRange, // 출간일 - 3개월, 1년 등 문자열로 받아 날짜 계산
         Integer page,
         Integer size
) {

    public BookSearchRequest {
        if (page == null) page = 0;
        if (size == null) size = 10;
        if (size > 100) size = 100; // 상한 제한
    }
}
