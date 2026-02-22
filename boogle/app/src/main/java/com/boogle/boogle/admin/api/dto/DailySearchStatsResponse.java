package com.boogle.boogle.admin.api.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record DailySearchStatsResponse(
        // 오늘의 검색 수 (일일기준)
        long todayTotalSearchCount,
        long yesterdayTotalSearchCount,
        double searchCountGrowthRate, // 전일 대비 증감률%

        // 검색 실패율 (일일기준)
        double todayFailureRate,      // 오늘 실패율%
        double yesterdayFailureRate,  // 어제 실패율%

        // 시간대별 검색량 (일일기준 + Chart.js 혼합 사용)
        List<HourlySearchCount> hourlySearchCounts
) {
    @Builder
    public record HourlySearchCount(
            int hour,        // 0 ~ 23
            long searchCount // 시간대 검색 수
    ) {}

}
