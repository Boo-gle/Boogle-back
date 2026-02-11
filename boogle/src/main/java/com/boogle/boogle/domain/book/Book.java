package com.boogle.boogle.domain.book;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
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
    private String standardId;

    @Column(nullable = false)
    private String author;

    private String translator;

    @Column(nullable = false)
    private String publisher;

    @Column(nullable = false)
    private LocalDate publishedDate;

    private String description;

    private Integer price;

    private String categories; // IT/프로그래밍/자바

    @Enumerated(EnumType.STRING)
    private ProductType productType; // BOOK, MAGAZINE

    private String thumbnailUrl;

    private String seriesId;
    private String seriesName;
    private Integer seriesOrder;

    @CreatedDate
    private Instant createdDate;

    private Instant updatedDate;

}
