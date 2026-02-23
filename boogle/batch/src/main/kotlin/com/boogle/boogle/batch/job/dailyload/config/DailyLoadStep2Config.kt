package com.boogle.boogle.batch.job.dailyload.config

import com.boogle.boogle.batch.job.dailyload.processor.LowQualityKeywordDailyLastIngestedOnProcessor
import com.boogle.boogle.batch.job.dailyload.writer.LowQualityKeywordDailyLastIngestedOnWriter
import com.boogle.boogle.search.domain.LowQualityKeywordDaily
import com.boogle.boogle.search.infra.LowQualityKeywordDailyRepository
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.infrastructure.item.database.JpaPagingItemReader
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import java.time.ZoneId

@Configuration
class DailyLoadStep2Config {

    @Bean
    @StepScope
    fun lowQualityKeywordDailyLastIngestedOnProcessor(): LowQualityKeywordDailyLastIngestedOnProcessor {
        return LowQualityKeywordDailyLastIngestedOnProcessor(ZoneId.of("Asia/Seoul"))
    }

    @Bean
    @StepScope
    fun lowQualityKeywordDailyLastIngestedOnWriter(
        repository: LowQualityKeywordDailyRepository,
    ): LowQualityKeywordDailyLastIngestedOnWriter {
        return LowQualityKeywordDailyLastIngestedOnWriter(repository)
    }

    @Bean
    fun dailyLoadStep2_updateLastIngestedOn(
        jobRepository: JobRepository,
        tm: PlatformTransactionManager,
        lowQualityKeywordDailyForUpdateReader: JpaPagingItemReader<LowQualityKeywordDaily>,
        lowQualityKeywordDailyLastIngestedOnProcessor: LowQualityKeywordDailyLastIngestedOnProcessor,
        lowQualityKeywordDailyLastIngestedOnWriter: LowQualityKeywordDailyLastIngestedOnWriter,
    ): Step {
        return StepBuilder("dailyLoadStep2_updateLastIngestedOn", jobRepository)
            .chunk<LowQualityKeywordDaily, LowQualityKeywordDaily>(500) // ✅ v6 권장
            .transactionManager(tm)                                    // ✅ v6 권장
            .reader(lowQualityKeywordDailyForUpdateReader)
            .processor(lowQualityKeywordDailyLastIngestedOnProcessor)
            .writer(lowQualityKeywordDailyLastIngestedOnWriter)
            .build()
    }
}