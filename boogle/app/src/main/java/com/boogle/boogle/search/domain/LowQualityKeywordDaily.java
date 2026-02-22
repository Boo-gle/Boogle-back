package com.boogle.boogle.search.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

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
    private OffsetDateTime lastSeenAt;

    private LocalDate lastIngestedOn;

    private String suggestedKeyword;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SynonymStatus status; // UNPROCESSED(기본값), EXCLUDED, SCHEDULED

    public LowQualityKeywordDaily(LocalDate eventDate, String searchKeyword, String suggestedKeyword) {

        if(searchKeyword == null || searchKeyword.isBlank()){
            throw new IllegalArgumentException("searchKeyword must not be blank");
        }

        this.eventDate = eventDate;
        this.searchKeyword = searchKeyword;
        this.dailyCount = 1;
        this.lastSeenAt = OffsetDateTime.now(ZoneId.of("Asia/Seoul"));
        this.suggestedKeyword = suggestedKeyword;
        this.status = SynonymStatus.UNPROCESSED;
    }

    public void markKeyword(String suggestedKeyword) {
        // lastSeenAt: 인서트/업데이트 시점
        this.lastSeenAt = OffsetDateTime.now(ZoneId.of("Asia/Seoul"));

        // suggestedKeyword: 팀원이 넘긴 추천 키워드 저장(최신값으로 덮어쓰기)
        if(suggestedKeyword != null && !suggestedKeyword.isBlank()){
            this.suggestedKeyword = suggestedKeyword;
        }

        // dailyCount: 있으면 +1
        this.dailyCount += 1;
    }

    // 관리자 대시보드 상태 변경용
    public void updateStatus(SynonymStatus newStatus) {
        if (newStatus != null) {
            this.status = newStatus;
        }
    }

}
