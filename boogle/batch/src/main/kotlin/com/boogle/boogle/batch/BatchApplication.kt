package com.boogle.boogle.batch

import com.boogle.boogle.batch.config.BatchBootstrapConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@SpringBootApplication
@Import(BatchBootstrapConfig::class)
@EnableJpaAuditing
class BatchApplication

fun main(args: Array<String>) {
    runApplication<BatchApplication>(*args)
}