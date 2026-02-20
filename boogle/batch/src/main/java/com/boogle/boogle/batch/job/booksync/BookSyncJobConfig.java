package com.boogle.boogle.batch.job.booksync;

import com.boogle.boogle.book.domain.Book;

import com.boogle.boogle.book.domain.document.BookDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.PlatformTransactionManager;

@Profile("search")
@Configuration
@RequiredArgsConstructor
public class BookSyncJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private final ItemReader<Book> bookItemReader;
    private final BookSyncProcessor bookSyncProcessor;
    private final BookSyncWriter bookSyncWriter;

    private static final int CHUNK_SIZE = 500;

    @Bean
    public Job bookSyncJob() {
        return new JobBuilder("bookSyncJob", jobRepository)
                .start(bookSyncStep())
                .build();
    }

    // 실제 데이터가 흐르는 단계
    // 빨간줄은 버전업으로 다른 방법으로 바꿀 예정이라는 경고
    @Bean
    public Step bookSyncStep() {
        return new StepBuilder("bookSyncStep", jobRepository)
                .<Book, BookDocument>chunk(CHUNK_SIZE, transactionManager)
                .reader(bookItemReader)
                .processor(bookSyncProcessor)
                .writer(bookSyncWriter)
                .build();
    }
}
