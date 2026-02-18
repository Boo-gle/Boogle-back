package com.boogle.boogle.batch.config

import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration
@ComponentScan(
    basePackages = [ // 배치 코드
        "com.boogle.boogle.batch"
    ]
)
@EntityScan(
    basePackages = [
        "com.boogle.boogle.book.domain",
        "com.boogle.boogle.search.domain"
    ]
)
@EnableJpaRepositories(
    basePackages = [ // Repository 위치
        "com.boogle.boogle.book.infra",
        "com.boogle.boogle.search.infra"
    ]
)
class BatchBootstrapConfig {
}