package com.boogle.boogle.admin.api.dto;

import com.boogle.boogle.search.domain.SynonymStatus;

import java.util.List;

public record LowQualityKeywordDto(
        Long id,
        int rank,                // 순위(페이징 기반)
        String searchKeyword,    // 실패어
        int dailyCount,          // 실패 횟수
        String suggestedKeyword, // ES 추천어
        String status            // 상태 (UNPROCESSED 미처리, EXCLUDED 제외, SCHEDULED 등록예정)
) {

    public record StatusUpdateRequest(
            List<Long> idList,      // 체크박스로 선택한 ID 목록
            SynonymStatus newStatus // 변경하 상태
    ) {}
}
