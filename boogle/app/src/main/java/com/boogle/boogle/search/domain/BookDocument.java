package com.boogle.boogle.search.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import static org.springframework.data.elasticsearch.annotations.FieldType.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Document(indexName = "books")
@Setting(settingPath = "elasticsearch/settings.json")
@Mapping(mappingPath = "elasticsearch/mappings.json")
public class BookDocument { // ES에 저장될 구조

    @Id
    @Field(type = Keyword)
    private String id;

    @MultiField(
            mainField = @Field(type = Text, analyzer = "korean_nori_analyzer", searchAnalyzer = "korean_nori_analyzer"),
            otherFields = {
                    @InnerField(suffix = "keyword", type = Keyword),
                    @InnerField(suffix = "autocomplete", type = Text, analyzer = "autocomplete_analyzer")
            }
    )
    private String title;


    @Field(type = Keyword)
    private String titleChosung; // 초성 검색용 필드

    @MultiField(
            mainField = @Field(type = Text, analyzer = "korean_nori_analyzer"),
            otherFields = @InnerField(suffix = "keyword", type = Keyword)
    )
    private String author;

    @MultiField(
            mainField = @Field(type = Text, analyzer = "korean_nori_analyzer"),
            otherFields = @InnerField(suffix = "keyword", type = Keyword)
    )
    private String publisher;

    @Field(type = Text, analyzer = "korean_nori_analyzer")
    private String description;

    @Field(type = Integer)
    private Integer price;

    @Field(type = FieldType.Date, format = DateFormat.year_month_day, name = "publishedDate")
    private String publishedDate;

    @Field(type = Integer)
    private Integer categoryId;

    @Field(type = Keyword)
    private String productType; // DOMESTIC, FOREIGN

    @Field(type = Keyword)
    private String mallType;

    @Field(type = Keyword)
    private String isbn;

    @Field(type = Keyword, index = false) // 검색 제외, 단순 출력용
    private String thumbnailUrl;

}
