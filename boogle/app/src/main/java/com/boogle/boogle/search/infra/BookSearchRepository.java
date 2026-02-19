package com.boogle.boogle.search.infra;

import com.boogle.boogle.search.domain.BookDocument;
import org.springframework.context.annotation.Profile;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

@Profile("search")
public interface BookSearchRepository extends ElasticsearchRepository<BookDocument, String> {


}
