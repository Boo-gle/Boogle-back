package com.boogle.boogle.batch.job.common

import com.boogle.boogle.batch.common.OpenForProxy
import com.boogle.boogle.book.domain.Book
import com.boogle.boogle.book.domain.Category
import com.boogle.boogle.book.infra.BookRepository
import com.boogle.boogle.book.infra.CategoryRepository
import org.springframework.batch.infrastructure.item.Chunk
import org.springframework.batch.infrastructure.item.ItemWriter

@OpenForProxy
class CategoryAwareBookItemWriter(
    private val categoryRepository: CategoryRepository,
    private val bookRepository: BookRepository,
) : ItemWriter<Book> {

    override fun write(chunk: Chunk<out Book>) {
        val books = chunk.items
        if (books.isEmpty()) return

        // Step A: categoryIds 수집
        val categoryIds: Set<Int> = books
            .mapNotNull { it.category?.id }
            .toSet()

        // Step B: 기존 카테고리 일괄 조회
        val existing = categoryRepository.findAllById(categoryIds)
        val existingMap: Map<Int, Category> = existing.associateBy { it.id }

        // Step C: 없는 카테고리만 insert
        val missingCategories: List<Category> = categoryIds
            .asSequence()
            .filter { it !in existingMap }
            .mapNotNull { id ->
                val anyPath = books.firstOrNull { it.category?.id == id }?.category?.categoryPath
                Category.createCategory(id, anyPath)
            }
            .toList()

        val inserted: List<Category> =
            if (missingCategories.isEmpty()) emptyList()
            else categoryRepository.saveAll(missingCategories)

        // Step D: 최종 Category 맵 만들기 (existing + inserted)
        val categoryMap: Map<Int, Category> =
            (existing + inserted).associateBy { it.id }

        // Step E: 각 Book의 category를 “관리 상태 엔티티”로 교체
        books.forEach { book ->
            val id = book.category.id
            val managed = categoryMap[id] ?: error("Category(id=$id) not found in map")
            book.changeCategory(managed)
        }

        // standardId(isbn13) 기준 upsert를 위한 기존 도서 일괄 조회
        val standardIds: Set<String> = books
            .mapNotNull { it.standardId?.trim() }
            .filter { it.isNotBlank() }
            .toSet()

        val existingBooks =
            if (standardIds.isEmpty()) emptyList()
            else bookRepository.findAllByStandardIdIn(standardIds)

        val existingByStandardId: Map<String, Book> =
            existingBooks.associateBy { it.standardId }

        val toSave: MutableList<Book> = ArrayList(books.size)
        books.forEach { incoming ->
            val key = incoming.standardId
            val managedExisting = existingByStandardId[key]
            if (managedExisting != null) {
                managedExisting.changeCategory(incoming.category)
                managedExisting.updateFrom(incoming)
                toSave.add(managedExisting)
            } else {
                toSave.add(incoming)
            }
        }

        bookRepository.saveAll(toSave)
    }
}