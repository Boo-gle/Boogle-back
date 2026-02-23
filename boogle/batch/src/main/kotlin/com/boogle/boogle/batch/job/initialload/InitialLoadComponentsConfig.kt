package com.boogle.boogle.batch.job.initialload

import com.boogle.boogle.batch.external.dto.AladinBookItemDto
import com.boogle.boogle.batch.job.common.AladinBookItemProcessor
import com.boogle.boogle.batch.job.common.CategoryAwareBookItemWriter
import com.boogle.boogle.book.domain.Book
import com.boogle.boogle.book.infra.BookRepository
import com.boogle.boogle.book.infra.CategoryRepository
import org.springframework.batch.infrastructure.item.ItemProcessor
import org.springframework.batch.infrastructure.item.ItemWriter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class InitialLoadComponentsConfig(
    private val categoryRepository: CategoryRepository,
    private val bookRepository: BookRepository,
) {

    @Bean
    fun aladinItemProcessor(): ItemProcessor<AladinBookItemDto, Book> =
        AladinBookItemProcessor()

    @Bean
    fun categoryAwareBookWriter(): ItemWriter<Book> =
        CategoryAwareBookItemWriter(
            categoryRepository = categoryRepository,
            bookRepository = bookRepository,
        )
}