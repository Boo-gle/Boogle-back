package com.boogle.boogle.search.infra;

import com.boogle.boogle.search.domain.LowQualityKeywordDaily;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface LowQualityKeywordDailyRepository extends JpaRepository<LowQualityKeywordDaily, Long> {
    Optional<LowQualityKeywordDaily> findByEventDateAndSearchKeyword(LocalDate eventDate, String searchKeyword);
}
