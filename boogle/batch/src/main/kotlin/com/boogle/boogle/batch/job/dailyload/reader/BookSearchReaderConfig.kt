package com.boogle.boogle.batch.job.dailyload.reader

import com.boogle.boogle.batch.external.client.AladinBookApiClient
import com.boogle.boogle.batch.external.client.AladinBookSearchApiClient
import com.boogle.boogle.batch.external.dto.AladinBookItemDto
import com.boogle.boogle.batch.job.initialload.AladinPagingItemReader
import com.boogle.boogle.search.domain.LowQualityKeywordDaily
import com.boogle.boogle.search.domain.SynonymStatus
import com.boogle.boogle.search.infra.LowQualityKeywordDailyRepository
import jakarta.persistence.EntityManagerFactory
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.infrastructure.item.ItemReader
import org.springframework.batch.infrastructure.item.database.JpaPagingItemReader
import org.springframework.batch.infrastructure.item.database.builder.JpaPagingItemReaderBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Configuration
class BookSearchReaderConfig(
    private val entityManagerFactory: EntityManagerFactory,
    private val aladinBookSearchApiClient: AladinBookSearchApiClient,
) {

    companion object {
        private val KST: ZoneId = ZoneId.of("Asia/Seoul")
    }

    @Bean
    @StepScope
    fun lowQualityKeywordDailyJpaReader(
        @Value("#{jobParameters['minDailyCount'] ?: 3}") minDailyCount: Int,
        @Value("#{jobParameters['targetDate']}") targetDate: String?,
    ): JpaPagingItemReader<LowQualityKeywordDaily> {

        val readDate: LocalDate =
            if (!targetDate.isNullOrBlank()) LocalDate.parse(targetDate)
            else LocalDate.now(KST).minusDays(1)

        return JpaPagingItemReaderBuilder<LowQualityKeywordDaily>()
            .name("lowQualityKeywordDailyJpaReader")
            .entityManagerFactory(entityManagerFactory)
            .pageSize(500)
            .queryString(
                """
                select l
                from LowQualityKeywordDaily l
                where l.eventDate = :readDate
                  and l.dailyCount >= :minDailyCount
                  and l.status = :status
                order by l.dailyCount desc, l.searchKeyword asc
                """.trimIndent()
            )
            .parameterValues(
                mapOf(
                    "readDate" to readDate,
                    "minDailyCount" to minDailyCount,
                    "status" to SynonymStatus.UNPROCESSED,
                )
            )
            .saveState(true)
            .build()
    }

    @Bean
    @StepScope
    fun dailyIngestReader(
        lowQualityKeywordDailyJpaReader: JpaPagingItemReader<LowQualityKeywordDaily>,
    ): AladinBookSearchItemReader {

        return AladinBookSearchItemReader(
            keywordReader = lowQualityKeywordDailyJpaReader,
            aladinBookSearchApiClient = aladinBookSearchApiClient,
            searchTargets = listOf("Book", "Foreign"),
            maxResults = 50,
            maxPages = 4,
        )
    }

    /**
     * Step2 Reader:
     * Step1과 "동일 조건"으로 LowQualityKeywordDaily를 다시 읽습니다.
     * (Step1에서 쓰던 reader를 재사용하면 state 충돌 가능성이 있으니 Bean을 분리)
     */
    @Bean
    @StepScope
    fun lowQualityKeywordDailyForUpdateReader(
        @Value("#{jobParameters['minDailyCount'] ?: 3}") minDailyCount: Int,
        @Value("#{jobParameters['targetDate']}") targetDate: String?,
    ): JpaPagingItemReader<LowQualityKeywordDaily> {

        val readDate: LocalDate =
            if (!targetDate.isNullOrBlank()) LocalDate.parse(targetDate)
            else LocalDate.now(KST).minusDays(1)

        return JpaPagingItemReaderBuilder<LowQualityKeywordDaily>()
            .name("lowQualityKeywordDailyForUpdateReader") // ✅ Step1과 이름 다르게
            .entityManagerFactory(entityManagerFactory)
            .pageSize(500)
            .queryString(
                """
                select l
                from LowQualityKeywordDaily l
                where l.eventDate = :readDate
                  and l.dailyCount >= :minDailyCount
                  and l.status = :status
                order by l.dailyCount desc, l.searchKeyword asc
                """.trimIndent()
            )
            .parameterValues(
                mapOf(
                    "readDate" to readDate,
                    "minDailyCount" to minDailyCount,
                    "status" to SynonymStatus.UNPROCESSED,
                )
            )
            .saveState(true)
            .build()
    }

}