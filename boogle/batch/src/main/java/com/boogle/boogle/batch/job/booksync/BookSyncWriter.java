package com.boogle.boogle.batch.job.booksync;


import com.boogle.boogle.book.domain.document.BookDocument;
import com.boogle.boogle.book.infra.BookSearchRepository;
import lombok.RequiredArgsConstructor;


import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "search.es.enabled", havingValue = "true")
public class BookSyncWriter implements ItemWriter<BookDocument> {

    private final BookSearchRepository bookSearchRepository;

    @Override
    public void write(Chunk<? extends BookDocument> chunk) throws Exception {
        if (!chunk.isEmpty()){
            bookSearchRepository.saveAll((List<BookDocument>) chunk.getItems());
        }
    }
}
