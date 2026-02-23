package com.boogle.boogle.batch.job.dailyload.config

import com.boogle.boogle.batch.external.dto.AladinBookItemDto
import com.boogle.boogle.batch.job.common.AladinBookItemProcessor
import com.boogle.boogle.batch.job.common.CategoryAwareBookItemWriter
import com.boogle.boogle.book.domain.Book
import com.boogle.boogle.book.infra.BookRepository
import com.boogle.boogle.book.infra.CategoryRepository
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.infrastructure.item.ItemReader
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class DailyLoadStep1Config {

    @Bean
    @StepScope
    fun aladinBookItemProcessor(): AladinBookItemProcessor {
        // ✅ DTO -> Book 변환 (필수값 없으면 null로 필터링)
        return AladinBookItemProcessor()
    }

    @Bean
    @StepScope
    fun categoryAwareBookUpsertWriter(
        categoryRepository: CategoryRepository,
        bookRepository: BookRepository,
    ): CategoryAwareBookItemWriter {
        // ✅ Category 선삽입 + Book(standardId 기준) upsert
        return CategoryAwareBookItemWriter(categoryRepository, bookRepository)
    }

    @Bean
    fun dailyLoadStep1_ingestBooks(
        jobRepository: JobRepository,
        tm: PlatformTransactionManager,
        // ✅ BookSearchReaderConfig에 이미 정의되어 있는 dailyIngestReader(AladinBookSearchItemReader)를 주입받아 사용
        dailyIngestReader: ItemReader<AladinBookItemDto>,
        aladinBookItemProcessor: AladinBookItemProcessor,
        categoryAwareBookUpsertWriter: CategoryAwareBookItemWriter,
    ): Step {
        return StepBuilder("dailyLoadStep1_ingestBooks", jobRepository)
            .chunk<AladinBookItemDto, Book>(50) // ✅ v6 권장: chunk(size)
            .transactionManager(tm)            // ✅ v6 권장: TM 분리 지정
            .reader(dailyIngestReader)
            .processor(aladinBookItemProcessor)
            .writer(categoryAwareBookUpsertWriter)
            .build()
    }
}