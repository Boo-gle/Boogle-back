package com.boogle.boogle.book.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Arrays;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "categories")
public class Category {

    private static final String DEFAULT_MALL_TYPE = "UNKNOWN";

    @Id
    @Column(name = "category_id")
    private Integer id;

    @Column(nullable = false)
    private String mallType;

    private String categoryDepth1;
    private String categoryDepth2;
    private String categoryDepth3;
    private String categoryPath;

    public static Category createCategory(int id, String categoryPath) {
        Category category = new Category();
        category.id = id;
        category.applyCategoryPath(categoryPath);
        return category;
    }

    /**
     * @param rawCategoryPath
     *
     * 알라딘의 categoryName - 전체 경로 문자열
     * 예: 국내도서>소설/시/희곡>판타지/환상문학>외국판타지/환상소설
     */
    private void applyCategoryPath(String rawCategoryPath) {
        this.categoryPath = rawCategoryPath;

        if (rawCategoryPath == null || rawCategoryPath.isBlank()) {
            this.mallType = DEFAULT_MALL_TYPE;
            this.categoryDepth1 = null;
            this.categoryDepth2 = null;
            this.categoryDepth3 = null;
            return;
        }

        String[] parts = Arrays.stream(rawCategoryPath.split(">"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toArray(String[]::new);

        this.mallType = parts.length > 0 ? parts[0] : DEFAULT_MALL_TYPE;
        this.categoryDepth1 = parts.length > 1 ? parts[1] : null;
        this.categoryDepth2 = parts.length > 2 ? parts[2] : null;
        this.categoryDepth3 = parts.length > 3 ? parts[3] : null;
    }

}
