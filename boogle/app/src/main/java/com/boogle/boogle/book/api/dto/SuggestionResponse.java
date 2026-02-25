package com.boogle.boogle.book.api.dto;

public record SuggestionResponse (
        String title,   // 제목
        String author,  // 저자
        String type     // 강조할 타입 (무엇 때문에 검색되었는가)
){}
