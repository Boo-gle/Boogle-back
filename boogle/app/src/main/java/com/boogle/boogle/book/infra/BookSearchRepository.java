package com.boogle.boogle.book.infra;

import com.boogle.boogle.book.domain.document.BookDocument;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

@ConditionalOnProperty(name = "search.es.enabled", havingValue = "true")
public interface BookSearchRepository extends ElasticsearchRepository<BookDocument, String> {


}
