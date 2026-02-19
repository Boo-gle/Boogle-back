package com.boogle.boogle.book.infra;

import com.boogle.boogle.book.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
}
