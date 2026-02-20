package com.boogle.boogle.book.api.dto;

import jakarta.validation.constraints.NotBlank;

public record BookSearchRequest( // 검색어랑 페이징 정보 요청
        @NotBlank(message = "검색어를 입력해주세요.")
         String keyword,
         Integer page,
         Integer size
) {

    public BookSearchRequest {
        if (page == null) page = 0;
        if (size == null) size = 10;
        if (size > 100) size = 100; // 상한 제한
    }
}
