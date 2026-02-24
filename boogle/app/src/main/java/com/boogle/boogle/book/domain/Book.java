package com.boogle.boogle.book.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "books",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_standard_id",
                        columnNames = "standard_id"
                )
        }
)
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "book_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String standardId; // 알라딘 isbn13

    @Column(nullable = false)
    private String author;

    private String translator;

    @Column(nullable = false)
    private String publisher;

    @Column(nullable = false)
    private LocalDate publishedDate; // 알라딘 pubdate

    @Column(columnDefinition = "TEXT")
    private String description;

    private Integer price; // 알라딘 pricestandard

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ProductType productType; // DOMESTIC, FOREIGN

    private String thumbnailUrl;

    private Integer seriesId;
    private String seriesName;

    @CreatedDate
    @Column(nullable = false)
    private Instant createdAt;

    @LastModifiedDate // DB-> ES 동기화 시 어떤 도서 업데이트됐는지 자동 추적 가능
    private Instant updatedAt;

    public Book(Category category, String title, String standardId, String author, String publisher, String publishedDate, String description, Integer price, String productType, String thumbnailUrl, Integer seriesId, String seriesName) {
        Contributors contributors = splitAuthorAndTranslator(author);

        this.category = category;
        this.title = title;
        this.standardId = standardId;
        this.author = contributors.author;
        this.translator = contributors.translator;
        this.publisher = publisher;
        this.publishedDate = LocalDate.parse(publishedDate);
        this.description = description;
        this.price = price;
        this.productType = productType.trim().equalsIgnoreCase("FOREIGN") ? ProductType.FOREIGN : ProductType.DOMESTIC;
        this.thumbnailUrl = thumbnailUrl;
        this.seriesId = seriesId;
        this.seriesName = seriesName;
    }

    /**
     * 예) "A (지은이), B (엮은이), C (옮긴이)" 형태에서
     * - author: 지은이 + 엮은이
     * - translator: 옮긴이
     */
    public static Contributors splitAuthorAndTranslator(String raw) {
        if (raw == null || raw.isBlank()) {
            return new Contributors(null, null);
        }

        List<String> authors = new ArrayList<>();
        List<String> translators = new ArrayList<>();

        for (String token : raw.split(",")) {
            String part = token.trim();
            if (part.isEmpty()) continue;

            int open = part.lastIndexOf('(');
            int close = part.lastIndexOf(')');

            // "(역할)" 형태가 아니면 author로 처리
            if (open < 0 || close < 0 || close < open) {
                authors.add(part);
                continue;
            }

            String name = part.substring(0, open).trim();
            String role = part.substring(open + 1, close).trim();

            if (role.contains("옮긴이")) {
                translators.add(name);
            } else if (role.contains("지은이") || role.contains("엮은이")) {
                authors.add(name);
            } else {
                authors.add(name);
            }
        }

        String authorJoined = authors.isEmpty() ? null : String.join(", ", authors);
        String translatorJoined = translators.isEmpty() ? null : String.join(", ", translators);
        return new Contributors(authorJoined, translatorJoined);
    }

    public record Contributors(String author, String translator) {}

    /**
     * 배치 Writer에서 category를 "관리 상태(영속 상태) 엔티티"로 교체하기 위한 메서드.
     */
    public void changeCategory(Category managedCategory) {
        if (managedCategory == null) {
            throw new IllegalArgumentException("managedCategory must not be null");
        }
        this.category = managedCategory;
    }

    /**
     * 동일 standardId(ISBN13)인 기존 도서를 외부 데이터로 갱신하는 메서드.
     * - PK(id)는 건드리지 않음
     * - 필요한 필드만 갱신 (정책에 따라 조정)
     */
    public void updateFrom(Book source) {
        if (source == null) {
            throw new IllegalArgumentException("source must not be null");
        }
        this.title = source.title;
        this.author = source.author;
        this.translator = source.translator;
        this.publisher = source.publisher;
        this.publishedDate = source.publishedDate;
        this.description = source.description;
        this.price = source.price;
        this.productType = source.productType;
        this.thumbnailUrl = source.thumbnailUrl;
        this.seriesId = source.seriesId;
        this.seriesName = source.seriesName;
        // category는 Writer에서 managed 엔티티로 교체하는 흐름 유지
    }

    /**
     * Step 2(fulldescription 보강)에서 사용.
     *
     알라딘 ItemLookUp API로 가져온 전체 설명글로 교체합니다.
     */
    public void enrichDescription(String fullDescription) {
        if (fullDescription != null && !fullDescription.isBlank()) {
            this.description = fullDescription;
        }
    }
}
