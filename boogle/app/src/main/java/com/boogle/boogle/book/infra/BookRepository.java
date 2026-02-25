package com.boogle.boogle.book.infra;

import com.boogle.boogle.book.domain.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long>, BookRepositoryCustom {

    @Query("""
        select b
        from Book b
        where
            lower(b.title) like lower(concat('%', :keyword, '%'))
            or lower(b.author) like lower(concat('%', :keyword, '%'))
            or lower(b.description) like lower(concat('%', :keyword, '%'))
            or lower(b.translator) like lower(concat('%', :keyword, '%'))
            or lower(b.seriesName) like lower(concat('%', :keyword, '%'))
        """)
    Page<Book> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // ES - 마지막 동기화 시간 이후에 수정된 데이터를 페이징해서 가져오기
    Page<Book> findByUpdatedAtGreaterThanOrderByUpdatedAtAsc(Instant lastSyncTime, Pageable pageable);
    List<Book> findAllByStandardIdIn(Collection<String> standardIds);

    /** Step 1: ISBN 중복 체크용 */
    boolean existsByStandardId(String standardId);
    /** Step 2, 3: 티어별 카테고리 ID 목록으로 도서 조회 */
    List<Book> findByCategoryIdIn(List<Integer> categoryIds);
}
