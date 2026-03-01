package com.boogle.boogle.book.infra;

import com.boogle.boogle.book.api.dto.BookSearchRequest;
import com.boogle.boogle.book.domain.Book;
import com.boogle.boogle.book.domain.ProductType;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

import com.querydsl.core.types.OrderSpecifier;

import static com.boogle.boogle.book.domain.QBook.book;
import static com.boogle.boogle.book.domain.QCategory.category;


@RequiredArgsConstructor
public class BookRepositoryImpl implements BookRepositoryCustom { // DB전용 구현체

    private final JPAQueryFactory  queryFactory;

    @Override
    public Page<Book> searchBooks(BookSearchRequest request, Pageable pageable) {

        // 실제 데이터 조회 쿼리 (N+1 방지를 위해 Category fetchJoin)
        List<Book> content = queryFactory
                .selectFrom(book)
                .leftJoin(book.category, category).fetchJoin()
                .where(
                        keywordCondition(request.keyword(), request.searchConditions()),
                        priceCondition(request.priceRange()),
                        dateCondition(request.dateRange()),
                        categoryCondition(request.categoryDepth2()),
                        productTypeCondition(request.productType())
                )
                .orderBy(sortCondition(request.sort()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 카운트 쿼리 (페이징 처리를 위해 필요)
        JPAQuery<Long> countQuery = queryFactory
                .select(book.count())
                .from(book)
                // 카운트 쿼리에서는 데이터 패치가 불필요하므로 fetchJoin()을 쓰지 않습니다.
                .leftJoin(book.category, category)
                .where(
                        keywordCondition(request.keyword(), request.searchConditions()),
                        priceCondition(request.priceRange()),
                        dateCondition(request.dateRange()),
                        categoryCondition(request.categoryDepth2()),
                        productTypeCondition(request.productType())
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    // 1. 키워드 조건 (제목, 저자, 출판사)
    // -> I/O를 유발하는 설명(TEXT타입) 삭제
    private BooleanExpression keywordCondition(String keyword, List<String> conditions) {

        if (!StringUtils.hasText(keyword)) return null;
        String kw = keyword.trim();
        // 체크박스 없을 때 기본 검색
        if (conditions == null || conditions.isEmpty()) {
            return book.title.containsIgnoreCase(kw)
                    .or(book.author.containsIgnoreCase(kw))
                    .or(book.publisher.containsIgnoreCase(kw));
        }

        BooleanExpression result = null;
        for (String cond : conditions) {
            BooleanExpression exp = null;
            if ("title".equals(cond)) exp = book.title.containsIgnoreCase(kw);
            else if ("author".equals(cond)) exp = book.author.containsIgnoreCase(kw);
            else if ("publisher".equals(cond)) exp = book.publisher.containsIgnoreCase(kw);

            if (exp != null) {
                result = (result == null) ? exp : result.or(exp);
            }
        }
        return result != null ? result : book.title.containsIgnoreCase(kw);
    }

    // 2. 가격 필터 (예: "10000-30000" 또는 "~10000")
    private BooleanExpression priceCondition(String priceRange) {
        if (!StringUtils.hasText(priceRange)) return null;
        try {
            String[] prices = priceRange.split("-");
            int min = Integer.parseInt(prices[0]);
            int max = (prices.length > 1 && !prices[1].equals("0")) ? Integer.parseInt(prices[1]) : Integer.MAX_VALUE;
            return book.price.between(min, max);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // 3. 출간일 필터 (LocalDate 타입에 맞게 파싱)
    private BooleanExpression dateCondition(String dateRange) {
        if (!StringUtils.hasText(dateRange)) return null;
        try {
            // 프론트에서 "YYYY-MM-DD" 형태로 온다고 가정
            LocalDate fromDate = LocalDate.parse(dateRange);
            return book.publishedDate.goe(fromDate);
        } catch (Exception e) {
            return null;
        }
    }

    // 4. 카테고리 필터 (Category 엔티티 조인)
    private BooleanExpression categoryCondition(String categoryDepth2) {
        return StringUtils.hasText(categoryDepth2) ? category.categoryDepth2.eq(categoryDepth2) : null;
    }

    // 5. 상품 종류 필터 (Enum 타입 안전 변환)
    private BooleanExpression productTypeCondition(String productType) {
        if (!StringUtils.hasText(productType)) return null;
        try {
            return book.productType.eq(ProductType.valueOf(productType.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // 6. 정렬 조건
    private OrderSpecifier<?>[] sortCondition(String sort) {
        if (!StringUtils.hasText(sort)) return new OrderSpecifier[]{book.id.desc()};

        switch (sort) {
            case "title":
                return new OrderSpecifier[]{book.title.asc()};
            case "price_asc":
                return new OrderSpecifier[]{book.price.asc()};
            case "latest":
                return new OrderSpecifier[]{book.publishedDate.desc()};
            case "accuracy":
            default:
                // DB는 ES처럼 유사도(Score)가 없으므로 최신 등록순으로 타협
                return new OrderSpecifier[]{book.id.desc()};
        }
    }
}
