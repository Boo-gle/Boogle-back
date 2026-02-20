package com.boogle.boogle.batch.job.booksync;


import com.boogle.boogle.book.domain.document.BookDocument;
import com.boogle.boogle.book.infra.BookSearchRepository;
import lombok.RequiredArgsConstructor;


import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Profile("search")
@Component
@RequiredArgsConstructor
public class BookSyncWriter implements ItemWriter<BookDocument> {

    private final BookSearchRepository bookSearchRepository;

    @Override
    public void write(Chunk<? extends BookDocument> chunk) throws Exception {
        if (!chunk.isEmpty()){
            bookSearchRepository.saveAll((List<BookDocument>) chunk.getItems());
        }
    }
}
