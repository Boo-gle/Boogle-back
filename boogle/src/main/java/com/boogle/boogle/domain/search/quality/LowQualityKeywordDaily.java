package com.boogle.boogle.domain.search.quality;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor
@Table(
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_event_date_and_keyword",
                        columnNames = {"event_date", "search_keyword"}
                )
        }
)
public class LowQualityKeywordDaily {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "low_quality_keyword_daily_id")
    private Long id;

    @Column(nullable = false)
    private LocalDate eventDate;

    @Column(nullable = false)
    private String searchKeyword;

    @Column(nullable = false)
    private Integer dailyCount;

    @Column(nullable = false)
    private Instant lastSeenAt;

    private LocalDate lastIngestedOn;

    private String suggestedKeyword;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SynonymStatus status; // UNPROCESSED(기본값), EXCLUDED, SCHEDULED

    public LowQualityKeywordDaily(String searchKeyword, Instant lastSeenAt, String suggestedKeyword) {
        this.eventDate = LocalDate.now();
        this.searchKeyword = searchKeyword;
        this.dailyCount = 1;
        this.lastSeenAt = lastSeenAt == null ? Instant.now() : lastSeenAt;
        this.suggestedKeyword = suggestedKeyword;
        this.status = SynonymStatus.UNPROCESSED;
    }
}
