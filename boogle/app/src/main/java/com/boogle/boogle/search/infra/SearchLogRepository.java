package com.boogle.boogle.search.infra;

import com.boogle.boogle.search.domain.SearchLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface SearchLogRepository extends JpaRepository<SearchLog,Long> {

    // 특정 기간 전체 검색 수 (오늘이나 어제)
    long countBySearchedAtBetween(Instant start, Instant end);

    // 특정 기간 검색 실패 수 (resultCount == 0)
    long countByResultCountAndSearchedAtBetween(int resultCount, Instant start, Instant end);

    // 오늘 전체 로그 (시간대별 통계용)
    List<SearchLog> findBySearchedAtBetween(Instant todayStart, Instant todayEnd);

}
