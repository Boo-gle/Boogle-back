package com.boogle.boogle.admin.application;

import com.boogle.boogle.admin.api.dto.LowQualityKeywordDto;
import com.boogle.boogle.search.domain.LowQualityKeywordDaily;
import com.boogle.boogle.search.domain.SynonymStatus;
import com.boogle.boogle.search.infra.LowQualityKeywordDailyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LowQualityKeywordAdminService { // 검색 실패 분석 영역용

    private final LowQualityKeywordDailyRepository repository;

    // 테이블 페이징 목록 조회 (드롭다운 상태 필터 포함)
    public Page<LowQualityKeywordDto> getKeywords(SynonymStatus status, Pageable pageable) {

        // 상태가 null이면 전체 조회, 아니면 상태별 조회
        Page<LowQualityKeywordDaily> page = (status == null)
                ? repository.findAll(pageable)
                : repository.findByStatus(status, pageable);

        // 페이징 기반 순위(Rank) 계산을 위한 offset
        int offset = pageable.getPageNumber() * pageable.getPageSize();

        return page.map(entity -> new LowQualityKeywordDto(
                entity.getId(),
                offset + page.getContent().indexOf(entity) + 1, // 1위부터 시작하도록 +1
                entity.getSearchKeyword(),
                entity.getDailyCount(),
                entity.getSuggestedKeyword(),
                entity.getStatus().name()
        ));
    }

    // 2. 상태 일괄 변경 (체크박스 기능)
    @Transactional
    public void updateStatusBulk(List<Long> ids, SynonymStatus newStatus) {
        if (ids == null || ids.isEmpty() || newStatus == null) return;

        List<LowQualityKeywordDaily> keywords = repository.findAllById(ids);

        // Dirty Checking용 자동으로 UPDATE
        keywords.forEach(keyword -> keyword.updateStatus(newStatus));
    }

    // 3. CSV 추출 기능 (SCHEDULED 등록예정 상태)
    public String exportScheduledKeywordsToCsv() {
        // 이번 주 월요일, 일요일 날짜 계산
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = today.with(DayOfWeek.SUNDAY);

        // 날짜 범위와 등록예정 상태 기준으로 조회
        List<LowQualityKeywordDaily> scheduledList = repository.findByStatusAndEventDateBetween(
                SynonymStatus.SCHEDULED,
                startOfWeek,
                endOfWeek
        );

        StringBuilder csvBuilder = new StringBuilder();

        // 엑셀 한글 깨짐 방지
        csvBuilder.append('\uFEFF');

        // CSV 헤더 라인
        csvBuilder.append("검색어(실패어),ES추천어,실패횟수\n");

        // CSV 데이터 행 생성
        for (LowQualityKeywordDaily keyword : scheduledList) {
            String searchWord = (keyword.getSearchKeyword() != null) ? keyword.getSearchKeyword() : "";
            String suggestWord = (keyword.getSuggestedKeyword() != null) ? keyword.getSuggestedKeyword() : "";

            csvBuilder.append(searchWord).append(",")
                    .append(suggestWord).append(",")
                    .append(keyword.getDailyCount()).append("\n");
        }
        return csvBuilder.toString();
    }
}
