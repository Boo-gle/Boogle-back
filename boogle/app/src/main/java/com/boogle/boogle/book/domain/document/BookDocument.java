package com.boogle.boogle.book.domain.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Document(indexName = "books")
@Setting(settingPath = "elasticsearch/settings.json")
@Mapping(mappingPath = "elasticsearch/mappings.json")
public class BookDocument { // ES에 저장될 구조

    @Id
    private String id;

    // 필드 설정들 json 파일에 작성함
    private String title;
    private String titleChosung; // 초성 검색용 필드
    private String author;
    private String publisher;
    private String description;
    private Integer price;
    private String publishedDate;
    private Integer categoryId;
    private String productType; // DOMESTIC, FOREIGN
    private String mallType;
    private String isbn;
    private String thumbnailUrl; // 검색 제외, 단순 출력용
    private Instant updatedAt; // ES에서 업데이트 시간 추적용

}