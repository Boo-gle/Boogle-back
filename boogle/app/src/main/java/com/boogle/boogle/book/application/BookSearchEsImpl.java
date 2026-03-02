package com.boogle.boogle.book.application;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.SuggestMode;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.search.FieldSuggester;
import co.elastic.clients.elasticsearch.core.search.StringDistance;
import co.elastic.clients.elasticsearch.core.search.SuggestSort;
import co.elastic.clients.elasticsearch.core.search.Suggester;
import co.elastic.clients.json.JsonData;
import com.boogle.boogle.book.api.dto.*;
import com.boogle.boogle.book.domain.document.BookDocument;
import com.boogle.boogle.search.application.LowQualityKeywordDailyService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
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

    // 로거
    private static final Logger searchLogger = LoggerFactory.getLogger("SEARCH_FAILURE");
    private static final Logger accessLogger = LoggerFactory.getLogger("ACCESS_LOG");
    private static final Logger errorLogger = LoggerFactory.getLogger("ERROR_LOG");

    @Override
    public BookSearchListResponse search(BookSearchRequest request) {
        // log - 검색 시간 측정
        long startTime = System.currentTimeMillis();

        // 페이징
        Pageable pageable = PageRequest.of(request.page(), request.size());

        try{
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

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.bool(b -> {

                    String keyword = request.keyword().trim();


                    // ️검색 대상 필드 활성화 결정
                    boolean searchTitle = false;
                    boolean searchAuthor = false;
                    boolean searchPublisher = false;
                    if (request.searchConditions() != null && !request.searchConditions().isEmpty()) {
                        searchTitle = request.searchConditions().contains("title");
                        searchAuthor = request.searchConditions().contains("author");
                        searchPublisher = request.searchConditions().contains("publisher");
                    } else {
                        // 아무것도 선택 안하면 전체 검색
                        searchTitle = true;
                        searchAuthor = true;
                        searchPublisher = true;
                    }


                    // EXACT MATCH (최상위 우선순위)
                    if (searchTitle) {
                        b.should(s -> s.term(t -> t
                                .field("title.keyword")
                                .value(keyword)
                                .boost(8000.0F)
                        ));
                    }

                    if (searchAuthor) {
                        // 정확 매칭
                        b.should(s -> s.term(t -> t
                                .field("author.keyword")
                                .value(keyword)
                                .boost(2100.0F)
                        ));

                        // 부분 검색 (authorSuggest)
                        b.should(s -> s.match(m -> m
                                .field("author.authorSuggest")
                                .query(keyword)
                                .boost(1200.0F)
                        ));

                        // raw 필드 PHRASE MATCH
                        b.should(s -> s.matchPhrase(mp -> mp
                                .field("author.raw")
                                .query(keyword)
                                .boost(2000.0F)
                        ));
                    }

                    if (searchPublisher) {
                        b.should(s -> s.term(t -> t
                                .field("publisher.keyword")
                                .value(keyword)
                                .boost(500.0F)
                        ));
                    }


                    // PHRASE MATCH (decompound none)
                    if (searchTitle) {
                        b.should(s -> s.matchPhrase(mp -> mp
                                .field("title.raw")
                                .query(keyword)
                                .boost(5000.0F)
                        ));
                    }

                    if (searchAuthor) {
                        b.should(s -> s.matchPhrase(mp -> mp
                                .field("author.raw") // Keyword 분석기 필드 사용
                                .query(keyword)
                                .boost(200.0F)
                        ));
                    }


                    // 초성 검색 (autocomplete analyzer 기반)

                    b.should(s -> s.match(m -> m
                            .field("titleChosung")
                            .query(keyword)
                            .boost(300.0F)
                    ));


                    // 일반 매칭 (핵심 필드만!)

                    List<String> generalFields = new ArrayList<>();

                    if (searchTitle) generalFields.add("title^120.0");
                    if (searchPublisher) generalFields.add("publisher^30.0");


                    b.should(s -> s.multiMatch(mm -> mm
                            .query(keyword)
                            .fields(generalFields)
                            .type(TextQueryType.BestFields)
                            .tieBreaker(0.1)
//                            .fuzziness(keyword.length() <= 2 ? "0" : "AUTO")
                                    .operator(Operator.And)
                    ));

                    if (searchAuthor) {
                        b.should(s -> s.match(m -> m
                                .field("author")
                                .query(keyword)
                                .operator(Operator.And) // "카", "자바", "나"가 다 있어야 점수 부여
                                .boost(5.0F) // 점수를 낮게 설정해서 제목 매칭을 방해하지 않게 함
                        ));
                    }

                    // 설명 필드 격리
                    // 전체 검색이거나, 특별히 설명 필드를 포함하는 조건일 때만 동작하게
                    if (request.searchConditions() == null || request.searchConditions().isEmpty()) {
                        b.should(s -> s.match(m -> m
                                .field("description")
                                .query(keyword)
                                .boost(60.0F)
                                .minimumShouldMatch("100%") // '자바'가 정확히 단어로 존재할 때만!
                        ));
                    }



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
                                .field("categoryDepth2")
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
                                        .field("title.spell")
                                        .suggestMode(SuggestMode.Always)   // 항상 제안
                                        .sort(SuggestSort.Score)           // 점수 순 정렬
                                        .minWordLength(2)                  // 2글자 이상 단어 포함
                                        .prefixLength(0)                   // 접두사 제한 없음
                                        .maxEdits(1)                       // 오타 2글자까지 허용
                                        .stringDistance(StringDistance.JaroWinkler) // 한글에 안정적
                                )
                        ))
                ))
                .withSort(sort)
                .withAggregation("category_count", Aggregation.of(a -> a
                        .terms(t -> t.field("categoryDepth2").size(20))
                ))
                .build();

        // BookDocument 타입으로 ES에 보내기
        SearchHits<BookDocument> searchHits = elasticsearchOperations.search(query, BookDocument.class);
        // 집계 데이터 추출
        List<AggregationResponse.AggregationBucket> categoryBuckets = new ArrayList<>();
        if (searchHits.hasAggregations()) {
            var aggregations = (ElasticsearchAggregations) searchHits.getAggregations();
            var aggregate = aggregations.aggregationsAsMap().get("category_count").aggregation().getAggregate();

            if (aggregate.isSterms()) {
                categoryBuckets = aggregate.sterms().buckets().array().stream()
                        .map(bucket -> new AggregationResponse.AggregationBucket(
                                bucket.key().stringValue(),
                                bucket.docCount()
                        ))
                        .collect(Collectors.toList());
            }
        }
        // log - 응답 시간 계산 및 느린 검색 경고 로그
        long duration = System.currentTimeMillis() - startTime;
        if(duration > 1000) {
            searchLogger.warn("event=SLOW_SEARCH keyword={} duration={}ms threshold=1000", request.keyword(), duration);
        }


        // 검색 실행해서 오타 교정어 추출하기
        String keyword = request.keyword().trim();
        String esSuggestedKeyword = null;

        if (searchHits.hasSuggest()) {
            var suggestion = searchHits.getSuggest().getSuggestion("spell-check");
            if (suggestion != null && suggestion.getEntries() != null) {
                StringBuilder suggestedSentence = new StringBuilder();
                boolean hasAnyCorrection = false;

                for (var entry : suggestion.getEntries()) {
                    if (!entry.getOptions().isEmpty()) {
                        // 오타 발견: 첫 번째 추천 단어로 교체
                        suggestedSentence.append(entry.getOptions().get(0).getText()).append(" ");
                        hasAnyCorrection = true;
                    } else {
                        // 정상 단어: 원본 유지
                        suggestedSentence.append(entry.getText()).append(" ");
                    }
                }

                if (hasAnyCorrection) {
                    String fullSuggestion = suggestedSentence.toString().trim();
                    if (!keyword.equals(fullSuggestion)) {
                        esSuggestedKeyword = fullSuggestion;
                    }
                }
            }
        }

        // 검색 결과가 3건 미만일 때 실패어 기록하기
        if (searchHits.getTotalHits() < 3) {
            searchLogger.warn(
                    "event=SEARCH_FAILURE keyword={} totalHits={} duration={}ms source=ES",
                    keyword, searchHits.getTotalHits(), duration
            );
            // 원본 검색어와 위에서 만든 교정어(있는 경우만)를 DB에 저장됨
            lowQualityKeywordDailyService.recordLowQualityKeyword(keyword, esSuggestedKeyword);
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

        // 저품질 판단 및 추천 로직
        double maxScore = searchHits.getMaxScore();
        boolean isRealMatch = false;

        if (!bookSearchList.isEmpty()) {
            BookSearchResponse firstBook = bookSearchList.get(0);
            String firstTitle = firstBook.title().replaceAll(" ", "");
            String cleanKeyword = keyword.replaceAll(" ", "");

            if (cleanKeyword.length() >= 2) {
                for (int i = 0; i < cleanKeyword.length() - 1; i++) {
                    if (firstTitle.contains(cleanKeyword.substring(i, i + 2))) {
                        isRealMatch = true;
                        break;
                    }
                }
            } else {
                isRealMatch = firstTitle.contains(cleanKeyword);
            }
        }

        // 품질 판정: 결과가 없거나, 점수가 낮거나, 실제 매칭되는 단어가 없으면 추천 모드 실행
        boolean isLowQuality = Double.isNaN(maxScore) || maxScore < 2000.0 || !isRealMatch;
        long finalTotalHits;

        if (bookSearchList.isEmpty() || isLowQuality) {
            // ip 정보 함께 기록
            String currentIp = getClientIp();
            accessLogger.info("event=SEARCH_RECOMMEND_MODE keyword={} ip={} suggested={} totalHits={}",
                    keyword, currentIp, (esSuggestedKeyword != null ? esSuggestedKeyword:"NONE"), searchHits.getTotalHits());

            // 최근 6개월간 신간 조회
            String sixMonthsAgo = LocalDate.now().minusMonths(6).toString();

            NativeQuery recommendationQuery = NativeQuery.builder()
                    .withQuery(q -> q.range(r -> r
                            .untyped(u -> u
                                    .field("publishedDate")
                                    .gte(JsonData.of(sixMonthsAgo))
                            )
                    ))
                    .withSort(s -> s.field(f -> f.field("publishedDate").order(SortOrder.Desc)))
                    .withPageable(PageRequest.of(0, request.size()))
                    .build();

            // 기존 저품질 결과 비우기
            bookSearchList.clear();
            SearchHits<BookDocument> recoHits = elasticsearchOperations.search(recommendationQuery, BookDocument.class);

            // 추천 결과 매핑 (score는 0.0)
            bookSearchList = recoHits.getSearchHits().stream()
                    .map(hit -> {
                        BookDocument doc = hit.getContent();
                        return BookSearchResponse.builder()
                                .id(Long.parseLong(doc.getId()))
                                .title(doc.getTitle())
                                .highlightTitle(doc.getTitle())
                                .author(doc.getAuthor())
                                .publisher(doc.getPublisher())
                                .thumbnailUrl(doc.getThumbnailUrl())
                                .price(doc.getPrice())
                                .score(0.0)
                                .categoryDepth2(doc.getCategoryDepth2())
                                .description(doc.getDescription())
                                .highlightDescription(doc.getDescription())
                                .isbn(doc.getIsbn())
                                .publishedDate(doc.getPublishedDate())
                                .mallType(doc.getMallType())
                                .productType(doc.getProductType())
                                .build();
                    }).collect(Collectors.toList());

            // 추천 모드이므로 전체 결과 수는 0건으로 보냄
            finalTotalHits = 0;
        } else {
            // [정상 모드]: 검색 결과 수 유지
            finalTotalHits = searchHits.getTotalHits();
        }

        // 최종 반환 처리
        Page<BookSearchResponse> pageResult = new PageImpl<>(bookSearchList, pageable, finalTotalHits);

        // BookSearchListResponse 생성 시 계산된 finalTotalHits를 직접 주입
        return new BookSearchListResponse(
                pageResult,
                finalTotalHits,      // 여기에 0이 들어가야 프론트에서 "결과 없음" 문구가 뜸
                keyword,
                esSuggestedKeyword,
                categoryBuckets
        );
    } catch (Exception e) {
        // 전체 에러 로그
            errorLogger.error("event=SEARCH_ERROR keyword={} message={}", request.keyword(), e.getMessage());
            return new BookSearchListResponse(new PageImpl<>(new ArrayList<>(), pageable, 0), 0, request.keyword(), null, new ArrayList<>());
        }
    }



    public List<SuggestionResponse> getSuggestions(String keyword) {
        try {
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

        } catch (Exception e) {
            // 키워드랑 예외 객체 (e)를 함께 넘겨 어디서 터졌는지 확인
            errorLogger.error("event=SUGGEST_ERROR keyword={} message={}", keyword, e.getMessage(), e);
            return List.of();   // 에러 발생 시 빈 리스트 반환
        }
    }

    private int getTypePriority(String type) {
        return "TITLE".equals(type) ? 1 : 2;
    }

    // 현재 요청을 보내는 사용자의 IP를 찾아내는 메서드
    private String getClientIp(){
        try{
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            String ip = request.getHeader("x-forwarded-for");
            if(ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)){
                ip = request.getRemoteAddr();
            }
            return ip;
        } catch (Exception e) {
            return "0.0.0.0";
        }
    }

}


