package com.boogle.boogle.batch.job.initialload

import com.boogle.boogle.batch.external.client.AladinBookApiClient
import com.boogle.boogle.batch.external.dto.AladinBookItemDto
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.infrastructure.item.ItemReader
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Configuration
class InitialLoadReaderConfig(
    private val aladinBookApiClient: AladinBookApiClient
) {

    // 운영 고정 정책(만 건 수집)
    private val searchTargets = listOf("Book", "Foreign")
    private val maxResults = 50
    private val week = 1
    private val maxStart = 20
    private val yearRepeat = 8

    @Bean
    @StepScope
    fun aladinItemListReader(
        @Value("#{jobParameters['runDate']}") runDateParam: String?,
        @Value("#{jobParameters['queryType']}") queryTypeParam: String?,
    ): AladinPagingItemReader {
        val runDate = resolveRunDate(runDateParam)
        val queryType = resolveQueryType(queryTypeParam)

        return AladinPagingItemReader(
            client = aladinBookApiClient,
            queryType = queryType,
            baseYear = runDate.year,
            baseMonth = runDate.monthValue,
            searchTargets = searchTargets,
            maxResults = maxResults,
            week = week,
            maxStart = maxStart,
            yearRepeat = yearRepeat,
        )
    }

    private fun resolveRunDate(runDateParam: String?): LocalDate {
        if (runDateParam.isNullOrBlank()) return LocalDate.now()
        return LocalDate.parse(runDateParam, DateTimeFormatter.ISO_DATE) // yyyy-MM-dd 강제
    }

    private fun resolveQueryType(queryTypeParam: String?): String =
        queryTypeParam?.takeIf { it.isNotBlank() } ?: "Bestseller"
}