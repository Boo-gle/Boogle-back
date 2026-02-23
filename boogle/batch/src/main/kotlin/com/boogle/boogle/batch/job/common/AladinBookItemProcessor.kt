package com.boogle.boogle.batch.job.common

import com.boogle.boogle.batch.external.dto.AladinBookItemDto
import com.boogle.boogle.book.domain.Book
import com.boogle.boogle.book.domain.Category
import org.springframework.batch.infrastructure.item.ItemProcessor

class AladinBookItemProcessor : ItemProcessor<AladinBookItemDto, Book> {

    override fun process(dto: AladinBookItemDto): Book? {
        val isbn13 = dto.isbn13?.trim()
        val title = dto.title
        val author = dto.author
        val publisher = dto.publisher
        val pubDate = dto.pubDate
        val mallType = dto.mallType
        val categoryId = dto.categoryId ?: 0

        // 필수값 없으면 스킵 (Spring Batch: processor에서 null 반환 시 해당 아이템 필터링)
        if (isbn13.isNullOrBlank() || title.isNullOrBlank() || author.isNullOrBlank() || publisher.isNullOrBlank() || pubDate.isNullOrBlank() || mallType.isNullOrBlank()) {
            return null
        }

        val category = Category.createCategory(categoryId, dto.categoryName)

        return Book(
            category,
            title,
            isbn13,
            author,
            publisher,
            pubDate,
            dto.description,
            dto.priceStandard,
            dto.mallType,
            dto.cover,
            dto.seriesInfo?.seriesId,
            dto.seriesInfo?.seriesName
        )
    }
}