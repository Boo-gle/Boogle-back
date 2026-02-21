package com.boogle.boogle.search.application;

import com.boogle.boogle.search.domain.LowQualityKeywordDaily;
import com.boogle.boogle.search.infra.LowQualityKeywordDailyRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class LowQualityKeywordDailyService {

    private final LowQualityKeywordDailyRepository repository; // DB 접근 레포지토리
    private final EntityManager entityManager;

    @Transactional
    public void recordLowQualityKeyword(String searchKeyword, String suggestedKeyword) {

        if (searchKeyword == null || searchKeyword.isBlank()) {
            throw new IllegalArgumentException("searchKeyword must not be blank");
        }

        String keyword = searchKeyword.trim();

        // ✅ “조회된 날짜”를 KST 기준으로 고정 (서버 타임존 흔들림 방지)
        LocalDate eventDate = LocalDate.now(ZoneId.of("Asia/Seoul"));

        // 1) 먼저 조회해서 있으면 update
        repository.findByEventDateAndSearchKeyword(eventDate, keyword)
                .ifPresentOrElse(existing -> {
                    // ✅ 엔티티 내부에서 lastSeenAt 갱신 + count++ + suggestedKeyword 갱신
                    existing.markKeyword(suggestedKeyword);
                }, () -> {
                    // 2) 없으면 insert 시도 (동시성 레이스로 유니크 예외 날 수 있음)
                    try {
                        // ✅ saveAndFlush로 지금 시점에 유니크 제약 검증을 강제로 발생시킴
                        repository.saveAndFlush(new LowQualityKeywordDaily(eventDate, keyword, suggestedKeyword));
                    } catch (DataIntegrityViolationException e) {
                        // ✅ 유니크 충돌: 동시에 다른 트랜잭션이 insert 성공한 상황

                        // ⚠️ 예외로 인해 영속성 컨텍스트가 실패 상태가 될 수 있어서 안전하게 비우고 다시 조회
                        entityManager.clear();

                        // 3) 재조회 후 update로 전환 (이게 “재시도 로직”의 핵심)
                        repository.findByEventDateAndSearchKeyword(eventDate, keyword)
                                .orElseThrow(() -> new IllegalStateException("Unique conflict occurred but row not found"))
                                .markKeyword(suggestedKeyword);
                    }
                });
    }

}
