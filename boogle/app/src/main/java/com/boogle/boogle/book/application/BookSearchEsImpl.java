package com.boogle.boogle.book.application;

import co.elastic.clients.elasticsearch._types.SuggestMode;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.search.FieldSuggester;
import co.elastic.clients.elasticsearch.core.search.StringDistance;
import co.elastic.clients.elasticsearch.core.search.Suggester;
import co.elastic.clients.json.JsonData;
import com.boogle.boogle.book.api.dto.AggregationResponse;
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
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
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

        // 정렬 로직 추가
        org.springframework.data.domain.Sort sort;
        String sortParam = request.sort() != null ? request.sort() : "accuracy";

        switch (sortParam) {
            case "title":
                // 상품명순 (한글/영문 오름차순)
                sort = org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "title.keyword");
                break;
            case "price_asc":
                // 저가격순
                sort = org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "price");
                break;
            case "latest":
                // 신상품순 (출간일 내림차순)
                sort = org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "publishedDate");
                break;
            case "accuracy":
            default:
                // 정확도순 (ES 기본 스코어 정렬)
                sort = org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "_score");
                break;
        }

        // 가중치 + 오타교정 + 하이라이팅
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.bool(b -> {

                    // --- 동적 검색 필드 결정 로직 ---
                    List<String> targetFields = new ArrayList<>();

                    // 프론트에서 넘어온 searchConditions(title, author, publisher)가 있는지 확인
                    if (request.searchConditions() != null && !request.searchConditions().isEmpty()) {
                        for (String cond : request.searchConditions()) {
                            if (cond.equals("title")) targetFields.add("title^4.0");
                            if (cond.equals("author")) targetFields.add("author^1.5");
                            if (cond.equals("publisher")) targetFields.add("publisher");
                        }
                    }

                    // 만약 체크박스를 하나도 안 선택했다면 기본 필드 전체 검색
                    if (targetFields.isEmpty()) {
                        targetFields = List.of("title^4.0", "author^1.5", "publisher", "description");
                    }

                    // 검색 로직
                    b.should(s -> s.term(t -> t
                            .field("title.keyword")
                            .value(request.keyword().trim())
                            .boost(10.0F)
                    ));

                    List<String> finalTargetFields = targetFields;

                    b.should(s -> s.multiMatch(mm -> mm
                            .query(request.keyword().trim())
                            .fields(finalTargetFields)
                            .type(TextQueryType.BestFields)
                            .fuzziness("AUTO")
                    ));

                    b.minimumShouldMatch("1");

                   // 가격 필터
                    if (request.priceRange() != null && !request.priceRange().isEmpty()) {
                        String[] prices = request.priceRange().split("-");
                        String min = prices[0];
                        String max = (prices.length > 1 && !prices[1].equals("0"))
                                ? prices[1]
                                : "99999999";

                        b.filter(f -> f.range(r -> r
                                .untyped(u -> u
                                        .field("price")
                                        .gte(JsonData.of(min))
                                        .lte(JsonData.of(max))
                                )
                        ));
                    }

                    // 출간일 필터
                    if (request.dateRange() != null && !request.dateRange().isEmpty()) {
                        b.filter(f -> f.range(r -> r
                                .untyped(u -> u
                                        .field("publishedDate")
                                        .gte(JsonData.of(request.dateRange()))
                                )
                        ));
                    }

                    // 카테고리 필터
                    if (request.categoryDepth2() != null && !request.categoryDepth2().isEmpty()) {
                        b.filter(f -> f.term(t -> t
                                .field("categoryDepth2.keyword")
                                .value(request.categoryDepth2())
                        ));
                    }

                    // 국내/국외 필터
                    if (request.productType() != null && !request.productType().isEmpty()) {
                        b.filter(f -> f.term(t -> t
                                .field("productType") // document 정의에 필드명이 대소문자 맞는지 확인
                                .value(request.productType())
                        ));
                    }

                    return b;

                }))
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
                .withSuggester(Suggester.of(s -> s
                        .suggesters("spell-check", FieldSuggester.of(fs -> fs
                                .text(request.keyword().trim()) // 오타 단어
                                .term(t -> t
                                        .field("title") // 오타를 검사할 기준 필드 (mappings.json 기준)
                                        .suggestMode(SuggestMode.Popular) // 더 자주 검색되는/존재하는 단어로 추천
                                        .minWordLength(2)
                                        .prefixLength(0)
                                        .maxEdits(2)
                                        .stringDistance(StringDistance.Levenshtein)
                                )
                        ))
                ))
                .withSort(sort)
                .build();

        // BookDocument 타입으로 ES에 보내기
        SearchHits<BookDocument> searchHits = elasticsearchOperations.search(query, BookDocument.class);

        // 검색 결과가 0건일 때 실패어 기록
        if (searchHits.getTotalHits() == 0) {
            String keyword = request.keyword().trim();
            String esSuggestedKeyword = null;

            // 엘라스틱서치가 응답에 'Suggest' 결과를 같이 보내줬는지 확인
            // 결과와 상관없이 ES가 추천해준 단어가 있는지 먼저 확인 (무조건 추출)
            if (searchHits.hasSuggest()) {
                // spell-check라는 이름으로 요청했던 서제스터 결과를 가져옴
                var suggestion = searchHits.getSuggest().getSuggestion("spell-check");
                var entries = suggestion.getEntries();

                // 추천 결과가 존재하고, 그 안에 실제 옵션(단어)이 들어있는지 확인
                if (entries != null && !entries.isEmpty() && !entries.get(0).getOptions().isEmpty()) {
                    // 가장 확률이 높은 첫 번째 추천 단어를 꺼냄
                    esSuggestedKeyword = entries.get(0).getOptions().get(0).getText();
                }
            }

            // 2. 팀원이 정의한 "실패어 기록 조건" 확인
            // 상황 A: 검색 결과가 0건일 때
            // 상황 B: 결과는 있지만 품질이 너무 낮을 때 (예: 결과가 3건 미만)
            long totalHits = searchHits.getTotalHits();
            boolean isLowQuality = (totalHits == 0) || (totalHits < 3);

            if (isLowQuality) {
                // 원본 검색어와 ES가 찾은 추천어를 DB에 쌓음
                lowQualityKeywordDailyService.recordLowQualityKeyword(keyword, esSuggestedKeyword);
            }

        }

        // 프론트로 보내기 위해 DTO로 매핑하기
        List<BookSearchResponse> bookSearchList = searchHits.getSearchHits().stream()
                .map(hit -> {
                    BookDocument bookdc = hit.getContent();

                    // 하이라이트 텍스트 추출
                    String highlightedTitle = Optional.ofNullable(hit.getHighlightField("title"))
                            .filter( list -> !list.isEmpty())
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
                            .isbn(bookdc.getIsbn())
                            .publishedDate(bookdc.getPublishedDate())
                            .mallType(bookdc.getMallType())
                            .productType(bookdc.getProductType())
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


    public AggregationResponse getCategoryAggregations(String keyword) {
        String cleanKeyword = keyword != null ? keyword.trim() : "";

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.bool(
                        b -> {
                            if (!cleanKeyword.isEmpty()) {
                                // 메인 검색과 완전히 동일한 조건으로 책을 먼저 찾기
                                b.should(s -> s.term(
                                                t -> t.field("title.keyword").value(cleanKeyword).boost(10.0F)
                                        ))
                                        .should(s -> s.multiMatch(
                                                mm -> mm.query(cleanKeyword)
                                                        .fields("title^4.0",
                                                                "author^1.5",
                                                                "publisher",
                                                                "description")
                                                        .type(TextQueryType.BestFields)
                                                        .fuzziness("AUTO")
                                        ))
                                        .minimumShouldMatch("1");
                            }
                            return b;
                        }
                ))
                .withAggregation("category_count", Aggregation.of(a -> a
                        .terms(t -> t
                                .field("categoryDepth2.keyword")
                                .size(10)
                        )
                ))
                .withMaxResults(0) // 데이터 X 집계만 ㅇ
                .build();

        SearchHits<BookDocument> hits = elasticsearchOperations.search(query, BookDocument.class);

        List<AggregationResponse.AggregationBucket> categoryBuckets = new ArrayList<>();

        if (hits.hasAggregations()) {
            var aggregations = (ElasticsearchAggregations) hits.getAggregations();
            var elcAggregation = aggregations.aggregationsAsMap().get("category_count");

            if (elcAggregation != null) {
                var aggregate = elcAggregation.aggregation().getAggregate();

                if (aggregate.isSterms()) {
                    categoryBuckets = aggregate.sterms().buckets().array().stream()
                            .map(bucket -> new AggregationResponse.AggregationBucket(
                                    bucket.key().stringValue(),
                                    bucket.docCount()
                            ))
                            .collect(Collectors.toList());
                }
            }
        }

        return new AggregationResponse(categoryBuckets);
    }

}


