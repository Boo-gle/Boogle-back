package com.boogle.boogle.book.infra;

import com.boogle.boogle.book.domain.document.BookDocument;
import org.springframework.context.annotation.Profile;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

@Profile("search")
public interface BookSearchRepository extends ElasticsearchRepository<BookDocument, String> {


}
