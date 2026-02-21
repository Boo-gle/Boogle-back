package com.boogle.boogle.book.application;

import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import com.boogle.boogle.book.api.dto.BookSearchRequest;
import com.boogle.boogle.book.api.dto.BookSearchResponse;
import com.boogle.boogle.book.domain.document.BookDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightFieldParameters;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "search.es.enabled", havingValue = "true")
public class BookSearchEsImpl implements BookSearchService {

    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public Page<BookSearchResponse> search(BookSearchRequest request) {
        // 페이징
        Pageable pageable = PageRequest.of(request.page(), request.size());

        // 가중치 + 오타교정 + 하이라이팅
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.bool(
                        b-> b
                                .should(s -> s.term( // 가중치 1. 키워드 100& 일치면 1순위
                                        t -> t
                                                .field("title.keyword")
                                                .value(request.keyword().trim())
                                                .boost(10.0F)
                                ))
                                .should(s ->s.multiMatch( // 가중치 2, 부분 일치 및 오타 교정 포함
                                        mm -> mm.query(request.keyword().trim())
                                                .fields(
                                                        "title^4.0",
                                                        "author^1.5",
                                                        "publisher",
                                                        "description"
                                                )
                                                .type(TextQueryType.BestFields) // 젤 높은거 채택
                                                .fuzziness("AUTO") // 오타교정
                                ))
                                .minimumShouldMatch("1")
                ))
                .withHighlightQuery(new HighlightQuery(
                        new Highlight(List.of(
                                new HighlightField("title"),
                                new HighlightField("description", HighlightFieldParameters.builder()
                                        .withFragmentSize(150)
                                        .withNumberOfFragments(1)
                                        .build())
                        )), BookDocument.class
                ))
                .withPageable(pageable)
                .build();

        // BookDocument 타입으로 ES에 보내기
        SearchHits<BookDocument> searchHits = elasticsearchOperations.search(query, BookDocument.class);

        // 프론트로 보내기 위해 DTO로 매핑하기
        List<BookSearchResponse> bookSearchList = searchHits.getSearchHits().stream()
                .map(hit -> {
                    BookDocument bookdc = hit.getContent();

                    // 하이라이트 텍스트 추출
                    String highlightedTitle = Optional.ofNullable(hit.getHighlightField("title"))
                            .filter(list -> !list.isEmpty())
                            .map(list -> list.get(0))
                            .orElse(bookdc.getTitle()); // 하이라이팅이 없으면 원본 제목 반환

                    String highlightedDesc = Optional.ofNullable(hit.getHighlightField("description"))
                            .filter(list -> !list.isEmpty())
                            .map(list -> list.get(0))
                            .orElse(bookdc.getDescription()); // 하이라이팅이 없으면 원본 설명 반환

                    return BookSearchResponse.builder()
                            .id(Long.parseLong(bookdc.getId()))
                            .title(bookdc.getTitle())
                            .highlightTitle(highlightedTitle)
                            .author(bookdc.getAuthor())
                            .publisher(bookdc.getPublisher())
                            .thumbnailUrl(bookdc.getThumbnailUrl())
                            .price(bookdc.getPrice())
                            .score((double)hit.getScore())
                            .categoryDepth2(bookdc.getCategoryDepth2())
                            .description(bookdc.getDescription())
                            .highlightDescription(highlightedDesc)
                            .build();
                }).collect(Collectors.toList());

        return new PageImpl<>(bookSearchList, pageable, searchHits.getTotalHits());

    }


}
