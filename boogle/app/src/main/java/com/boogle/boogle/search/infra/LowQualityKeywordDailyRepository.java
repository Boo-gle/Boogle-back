package com.boogle.boogle.search.infra;

import com.boogle.boogle.search.domain.LowQualityKeywordDaily;
import com.boogle.boogle.search.domain.SynonymStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LowQualityKeywordDailyRepository extends JpaRepository<LowQualityKeywordDaily, Long> {
    Optional<LowQualityKeywordDaily> findByEventDateAndSearchKeyword(LocalDate eventDate, String searchKeyword);

    // 관리자 대시보드 상태 필터링 + 페이징 조회 (테이블 용)
    Page<LowQualityKeywordDaily> findByStatus(SynonymStatus status, Pageable pageable);

    // 관리자 대시보드 등록예정(SCHEDULED) 전체 데이터 조회 (CSV 추출 용)
    List<LowQualityKeywordDaily> findByStatus(SynonymStatus status);

    // 관리자 대시보드 주간 범위
    List<LowQualityKeywordDaily> findByStatusAndEventDateBetween(SynonymStatus status, LocalDate startDate, LocalDate endDate);
}
