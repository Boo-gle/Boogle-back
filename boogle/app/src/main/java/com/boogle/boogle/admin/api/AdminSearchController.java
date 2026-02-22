package com.boogle.boogle.admin.api;

import com.boogle.boogle.admin.api.dto.DailySearchStatsResponse;
import com.boogle.boogle.admin.api.dto.LowQualityKeywordDto;
import com.boogle.boogle.admin.application.AdminSearchStatisticsService;
import com.boogle.boogle.admin.application.LowQualityKeywordAdminService;
import com.boogle.boogle.search.domain.SynonymStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/admin/search")
@RequiredArgsConstructor
public class AdminSearchController {

    private final AdminSearchStatisticsService statisticsService;
    private final LowQualityKeywordAdminService keywordAdminService;

    // 오늘의 검색 통계 ( 검색 수 / 검색 실패율 / 시간대별 통계량(Chart.js))
    @GetMapping("/stats")
    public ResponseEntity<DailySearchStatsResponse> getDailyStats() {
        DailySearchStatsResponse response = statisticsService.getDailySearchStats();
        return ResponseEntity.ok(response);
    }

    // 검색 실패 분석 테이블 (순위/키워드/실패횟수(Chart.js혼합)/es추천어)
    // 페이지당 10개, 실패횟수(dailyCount) 내림차순 정렬 -> 순위 가능
    @GetMapping("/low-quality")
    public ResponseEntity<Page<LowQualityKeywordDto>> getLowQualityKeywords(
            @RequestParam(required = false) SynonymStatus status,
            @PageableDefault(size = 10, sort = "dailyCount", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<LowQualityKeywordDto> response = keywordAdminService.getKeywords(status, pageable);
        return ResponseEntity.ok(response);
    }

    // 상태 일괄 변경 (체크박스 기능)
    @PatchMapping("/low-quality/status")
    public ResponseEntity<Void> updateKeywordStatus(@RequestBody LowQualityKeywordDto.StatusUpdateRequest request) {
        keywordAdminService.updateStatusBulk(request.idList(), request.newStatus());
        return ResponseEntity.ok().build();
    }

    // CSV 파일 추출 (등록예정 상태 전용)
    @GetMapping("/low-quality/export")
    public ResponseEntity<byte[]> exportCsv() {
        String csvData = keywordAdminService.exportScheduledKeywordsToCsv();

        // 오늘 날짜 기준으로 파일명에 동적으로 넣기
        String today = LocalDate.now(ZoneId.of("Asia/Seoul"))
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String fileName = "search_failed_keywords_" + today + ".csv";

        return ResponseEntity.ok()
                // 브라우저가 파일 다운로드로 인식
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                // 한글 깨짐 방지 UTF-8 인코딩
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData.getBytes(StandardCharsets.UTF_8));
    }
}
