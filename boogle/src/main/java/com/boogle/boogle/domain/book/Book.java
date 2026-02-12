package com.boogle.boogle.domain.book;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Getter @Setter
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

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String standardId; // 알라딘 isbn13

    @Column(nullable = false)
    private String author;

    private String translator;

    private String publisher;
    private LocalDate publishedDate; // 알라딘 pubdate

    private String description;

    private Integer price; // 알라딘 pricestandard

    @Column(nullable = false)
    private Integer categoryId = 0;     // 알라딘 categoryid
    @Column(nullable = false)
    private String categories = "기타";  // 알라딘 categoryname
    private String categoryDepth1;      // 알라딘 1Depth (예: 프로그래밍언어)
    private String categoryDepth2;      // 알라딘 2Depth (예: 자바)

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ProductType productType; // DOMESTIC, FOREIGN

    private String thumbnailUrl;

    private String seriesId;
    private String seriesName;

    @CreatedDate
    private Instant createdDate;

    @LastModifiedDate // DB-> ES 동기화 시 어떤 도서 업데이트됐는지 자동 추적 가능
    private Instant updatedDate;

}
