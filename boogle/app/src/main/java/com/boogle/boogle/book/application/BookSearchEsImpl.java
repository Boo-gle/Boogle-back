package com.boogle.boogle.book.application;

import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import com.boogle.boogle.book.api.dto.BookSearchRequest;
import com.boogle.boogle.book.api.dto.BookSearchResponse;
import com.boogle.boogle.book.api.dto.SuggestionResponse;
import com.boogle.boogle.book.domain.document.BookDocument;
import com.boogle.boogle.search.application.LowQualityKeywordDailyService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightFieldParameters;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "search.es.enabled", havingValue = "true")
public class BookSearchEsImpl implements BookSearchService {

    private final ElasticsearchOperations elasticsearchOperations;

    private final LowQualityKeywordDailyService lowQualityKeywordDailyService;

    @Override
    public Page<BookSearchResponse> search(BookSearchRequest request) {
        // 페이징
        Pageable pageable = PageRequest.of(request.page(), request.size());

        // 가중치 + 오타교정 + 하이라이팅
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.bool(
                        b -> b
                                .should(s -> s.term( // 가중치 1. 키워드 100& 일치면 1순위
                                        t -> t
                                                .field("title.keyword")
                                                .value(request.keyword().trim())
                                                .boost(10.0F)
                                ))
                                .should(s -> s.multiMatch( // 가중치 2, 부분 일치 및 오타 교정 포함
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

        // 검색 결과가 0건일 때 실패어 기록
        if (searchHits.getTotalHits() == 0) {
            String keyword = request.keyword().trim();
            // TODO: 다음 단계에서 ES Suggest API 쿼리를 짜서 실제 추천어를 받아올 예정입니다.
            String esSuggestedKeyword = null;

            lowQualityKeywordDailyService.recordLowQualityKeyword(keyword, esSuggestedKeyword);
        }

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
                            .score((double) hit.getScore())
                            .categoryDepth2(bookdc.getCategoryDepth2())
                            .description(bookdc.getDescription())
                            .highlightDescription(highlightedDesc)
                            .build();
                }).collect(Collectors.toList());

        return new PageImpl<>(bookSearchList, pageable, searchHits.getTotalHits());

    }
    public List<SuggestionResponse> getSuggestions(String keyword) {
        String cleanKeyword = keyword.trim().toLowerCase(); // 공백 제거
        if (cleanKeyword.isEmpty()) return List.of(); // 빈 리스트시 반환
        boolean isChosung = cleanKeyword.matches("^[ㄱ-ㅎ]+$"); // 초성 검색 여부 결정

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.bool(b -> { // bool 쿼리
                    // 일반 텍스트 검색 (제목, 저자)
                    if (isChosung) {
                        // 초성이면 초성 필드만 검색
                        b.must(s -> s.matchPhrasePrefix(mpp -> mpp
                                .field("titleChosung")
                                .query(cleanKeyword)
                                .boost(5.0f)
                        ));
                    } else {
                        // 일반 검색은 제목 + 저자만
                        b.must(s -> s.multiMatch(mm -> mm
                                .query(cleanKeyword)
                                .fields("title.suggest^3", "author^2")
                                .type(TextQueryType.PhrasePrefix)
                        ));
                    }
                    return b;
                }))
                .withPageable(PageRequest.of(0, 30))
                .build();

        SearchHits<BookDocument> hits = elasticsearchOperations.search(query, BookDocument.class);

        return hits.getSearchHits().stream()
                .map(hit -> {
                    BookDocument doc = hit.getContent();
                    String fullTitle = doc.getTitle().replaceAll("&quot;", "\"");
//                            .split(" - ")[0]
                    String author = doc.getAuthor() != null ? doc.getAuthor() : "저자 미상";

                    // 기본은 TITLE 타입으로 설정
                    String type = "TITLE";

                    // 만약 제목에는 키워드가 없고, 저자 이름에만 키워드가 있다면 AUTHOR로 표시
                    if (!fullTitle.toLowerCase().contains(cleanKeyword) &&
                            author.toLowerCase().contains(cleanKeyword)) {
                        type = "AUTHOR";
                    }

                    return new SuggestionResponse(fullTitle, author, type);
                })
                .distinct() // 혹시 모를 중복 제거
                .sorted((s1, s2) -> {
                    boolean s1Exact = s1.title().equalsIgnoreCase(cleanKeyword);
                    boolean s2Exact = s2.title().equalsIgnoreCase(cleanKeyword);

                    if (s1Exact && !s2Exact) return -1;
                    if (!s1Exact && s2Exact) return 1;

                    return Integer.compare(getTypePriority(s1.type()), getTypePriority(s2.type()));
                })
                .limit(10)
                .collect(Collectors.toList());

    }

    private int getTypePriority(String type) {
        return "TITLE".equals(type) ? 1 : 2;
    }


}


