package com.boogle.boogle.batch.job.booksync;

import com.boogle.boogle.batch.util.ChosungUtil;
import com.boogle.boogle.book.domain.Book;
import com.boogle.boogle.search.domain.BookDocument;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class BookSyncProcessor implements ItemProcessor<Book, BookDocument> {

    @Override
    public BookDocument process(Book book) throws Exception {

        String convertedMallType = convertMallType(book.getCategory().getMallType());
        if ("UNKNOWN".equals(convertedMallType)) {
            return null;
        }

        return BookDocument.builder()
                .id(book.getId().toString())
                .title(book.getTitle())
                .titleChosung(ChosungUtil.extractChosung(book.getTitle()))
                .author(book.getAuthor())
                .publisher(book.getPublisher())
                .mallType(convertedMallType)
                .productType(book.getProductType().name())
                .build();
    }

    private String convertMallType(String mallType) {
        if (mallType == null || mallType.isBlank()) return "UNKNOWN";
        String lowerType = mallType.toLowerCase();
        if (lowerType.contains("도서")) return "BOOK";
        if (lowerType.contains("음반") || lowerType.contains("뮤직")) return "MUSIC";
        if (lowerType.contains("dvd")) return "DVD";
        if (lowerType.contains("ebook") || lowerType.contains("전자책")) return "EBOOK";
        return "UNKNOWN";
    }

}
