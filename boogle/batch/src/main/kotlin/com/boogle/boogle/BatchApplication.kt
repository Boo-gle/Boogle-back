package com.boogle.boogle

import com.boogle.boogle.batch.config.BatchBootstrapConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import

@SpringBootApplication
@Import(BatchBootstrapConfig::class)
class BatchApplication

fun main(args: Array<String>) {
    runApplication<BatchApplication>(*args)
}