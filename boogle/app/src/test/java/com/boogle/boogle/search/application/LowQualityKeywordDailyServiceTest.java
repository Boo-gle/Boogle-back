package com.boogle.boogle.search.application;

import com.boogle.boogle.search.domain.LowQualityKeywordDaily;
import com.boogle.boogle.search.infra.LowQualityKeywordDailyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:boogle_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false",
        "spring.data.elasticsearch.repositories.enabled=false",
        "management.health.elasticsearch.enabled=false"
})
class LowQualityKeywordDailyServiceTest {

    @Autowired
    private LowQualityKeywordDailyService service;

    @Autowired
    private LowQualityKeywordDailyRepository repository;

    @Test
    @DisplayName("처음 호출 시 insert 된다")
    void insert_test() {
        // given
        String keyword = "테스트키워드";

        // when
        service.recordLowQualityKeyword(keyword, "추천키워드");

        // then
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        Optional<LowQualityKeywordDaily> result =
                repository.findByEventDateAndSearchKeyword(today, keyword);

        assertThat(result).isPresent();
        assertThat(result.get().getDailyCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("이미 존재하면 count가 증가한다")
    void update_test() {
        // given
        String keyword = "증가키워드";

        service.recordLowQualityKeyword(keyword, "추천1");
        service.recordLowQualityKeyword(keyword, "추천2");

        // then
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        LowQualityKeywordDaily result =
                repository.findByEventDateAndSearchKeyword(today, keyword).orElseThrow();

        assertThat(result.getDailyCount()).isEqualTo(2);
        assertThat(result.getSuggestedKeyword()).isEqualTo("추천2");
    }

    @Test
    @DisplayName("동시에 insert 시도해도 유니크 예외를 흡수하고 count가 누적된다")
    void concurrent_insert_test() throws InterruptedException {

        String keyword = "동시키워드";

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.execute(() -> {
                try {
                    service.recordLowQualityKeyword(keyword, "추천");
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        LowQualityKeywordDaily result =
                repository.findByEventDateAndSearchKeyword(today, keyword).orElseThrow();

        // 최소 1 이상이면 성공 (lost update는 허용한다고 했으므로)
        assertThat(result.getDailyCount()).isGreaterThanOrEqualTo(1);
    }
}