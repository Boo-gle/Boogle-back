package com.boogle.boogle.batch.job.booksync;

import com.boogle.boogle.batch.component.SyncTimeManager;
import com.boogle.boogle.book.domain.Book;
import com.boogle.boogle.book.infra.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.infrastructure.item.data.RepositoryItemReader;
import org.springframework.batch.infrastructure.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;

import java.util.Collections;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "search.es.enabled", havingValue = "true")
public class BookSyncReader {

    private final BookRepository bookRepository;
    private final SyncTimeManager syncTimeManager;

    @Bean
    public RepositoryItemReader<Book> bookItemReader() {
        return new RepositoryItemReaderBuilder<Book>()
                .name("bookItemReader")
                .repository(bookRepository)
                .methodName("findByUpdatedAtGreaterThanOrderByUpdatedAtAsc")
                .pageSize(500)
                .arguments(syncTimeManager.getLastSyncTime())
                .sorts(Collections.singletonMap("updatedAt", Sort.Direction.ASC))
                .build();
    }
}
