package com.boogle.boogle.book.infra;

import com.boogle.boogle.book.domain.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {

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
    List<Book> searchByKeyword(@Param("keyword") String keyword);

}
