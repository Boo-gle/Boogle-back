package com.boogle.boogle.book.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;

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

    @ManyToOne
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

    public Book(Category category, String title, String standardId, String author, String translator, String publisher, LocalDate publishedDate, String description, Integer price, ProductType productType, String thumbnailUrl, Integer seriesId, String seriesName) {
        this.category = category;
        this.title = title;
        this.standardId = standardId;
        this.author = author;
        this.translator = translator;
        this.publisher = publisher;
        this.publishedDate = publishedDate;
        this.description = description;
        this.price = price;
        this.productType = productType;
        this.thumbnailUrl = thumbnailUrl;
        this.seriesId = seriesId;
        this.seriesName = seriesName;
    }
}
