package com.boogle.boogle.batch.job.initialload

import com.boogle.boogle.batch.external.dto.AladinBookItemDto
import com.boogle.boogle.book.domain.Book
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.infrastructure.item.ItemProcessor
import org.springframework.batch.infrastructure.item.ItemReader
import org.springframework.batch.infrastructure.item.ItemWriter
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.web.client.RestClientException
import java.time.format.DateTimeParseException

@Configuration
class InitialLoadJobConfig(
    private val jobRepository: JobRepository,
    @Qualifier("transactionManager")
    private val transactionManager: PlatformTransactionManager,
) {

    private val chunkSize = 50

    @Bean
    fun aladinInitialLoadJob(
        aladinInitialLoadStep: Step,
    ): Job =
        JobBuilder("aladinInitialLoadJob", jobRepository)
            .listener(initialLoadJobExecutionListener())
            .start(aladinInitialLoadStep)
            .build()

    @Bean
    fun aladinInitialLoadStep(
        aladinItemListReader: ItemReader<AladinBookItemDto>,
        aladinItemProcessor: ItemProcessor<AladinBookItemDto, Book>,
        categoryAwareBookWriter: ItemWriter<Book>,
    ): Step =
        StepBuilder("aladinInitialLoadStep", jobRepository)
            .chunk<AladinBookItemDto, Book>(chunkSize)
            .transactionManager(transactionManager)
            .reader(aladinItemListReader)
            .processor(aladinItemProcessor)
            .writer(categoryAwareBookWriter)
            .faultTolerant()
            // API 호출 실패(네트워크/HTTP 등)는 재시도
            .retry(RestClientException::class.java)
            .retryLimit(3)
            // 데이터 포맷 불량(예: 날짜 파싱 실패)은 스킵
            .skip(DateTimeParseException::class.java)
            .skipLimit(50L)
            .build()

    @Bean
    fun initialLoadJobExecutionListener(): InitialLoadJobExecutionListener =
        InitialLoadJobExecutionListener()
}