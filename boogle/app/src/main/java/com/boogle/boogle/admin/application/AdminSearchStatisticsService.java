package com.boogle.boogle.admin.application;

import com.boogle.boogle.admin.api.dto.DailySearchStatsResponse;
import com.boogle.boogle.admin.api.dto.DailySearchStatsResponse.HourlySearchCount;
import com.boogle.boogle.search.domain.SearchLog;
import com.boogle.boogle.search.infra.SearchLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Transactional(readOnly=true)
@RequiredArgsConstructor
public class AdminSearchStatisticsService {

    private final SearchLogRepository searchLogRepository;

    public DailySearchStatsResponse getDailySearchStats(){
        ZoneId zone = ZoneId.of("Asia/Seoul");
        LocalDate today = LocalDate.now(zone);
        LocalDate yesterday = today.minusDays(1);

        // 범위 계산 (오늘 00:00:00 ~ 23:59:59)
        Instant todayStart = today.atStartOfDay(zone).toInstant();
        Instant todayEnd = today.plusDays(1).atStartOfDay(zone).toInstant();

        Instant yesterdayStart = yesterday.atStartOfDay(zone).toInstant();
        Instant yesterdayEnd = todayStart;

        // 오늘의 검색 수
        long todayTotal = searchLogRepository.countBySearchedAtBetween(todayStart, todayEnd);
        long yesterdayTotal = searchLogRepository.countBySearchedAtBetween(yesterdayStart, yesterdayEnd);

        // 검색 실패 수
        long todayFail = searchLogRepository.countByResultCountAndSearchedAtBetween(0, todayStart, todayEnd);
        long yesterdayFail = searchLogRepository.countByResultCountAndSearchedAtBetween(0, yesterdayStart, yesterdayEnd);

        // 검색 실패율, % 연산 로직 0으로 나누기 막음
        double todayFailureRate = (todayTotal == 0) ? 0.0 : ((double) todayFail / todayTotal) * 100;
        double yesterdayFailureRate = (yesterdayTotal == 0) ? 0.0 : ((double) yesterdayFail / yesterdayTotal) * 100;

        // 증감률 연산
        double searchCountGrowthRate = (yesterdayTotal == 0)
                ? (todayTotal > 0 ? 100.0 : 0.0)
                : ((double) (todayTotal - yesterdayTotal) / yesterdayTotal) * 100;

        // 오늘 전체 로그 가져오기
        List<SearchLog> todayLogs = searchLogRepository.findBySearchedAtBetween(todayStart, todayEnd);

        // 시간대별 그룹핑 연산 (Chart.js용)
        List<HourlySearchCount> hourlyStats = getHourlyStatistics(todayLogs, zone);

        return DailySearchStatsResponse.builder()
                .todayTotalSearchCount(todayTotal)
                .yesterdayTotalSearchCount(yesterdayTotal)
                .searchCountGrowthRate(Math.round(searchCountGrowthRate * 10.0) / 10.0)
                .todayFailureRate(Math.round(todayFailureRate * 10.0) / 10.0)
                .yesterdayFailureRate(Math.round(yesterdayFailureRate * 10.0) / 10.0)
                .hourlySearchCounts(hourlyStats)
                .build();
    }

    private List<HourlySearchCount> getHourlyStatistics(List<SearchLog> todayLogs, ZoneId zoneId) {
        // 시간대별 전체 검색 수
        Map<Integer, Long> totalByHour = todayLogs.stream()
                .collect(Collectors.groupingBy(
                        log -> log.getSearchedAt().atZone(zoneId).getHour(),
                        Collectors.counting()
                ));

        // 0시부터 23시까지 빈 시간대 0으로 채워서 반환
        return IntStream.range(0, 24)
                .mapToObj(hour -> new HourlySearchCount(
                        hour,
                        totalByHour.getOrDefault(hour, 0L)
                ))
                .collect(Collectors.toList());
    }
}
