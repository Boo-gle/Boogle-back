package com.boogle.boogle.batch

import com.boogle.boogle.batch.config.BatchBootstrapConfig
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import kotlin.system.exitProcess

@SpringBootApplication
@Import(BatchBootstrapConfig::class)
@EnableJpaAuditing
class BatchApplication

fun main(args: Array<String>) {
    val ctx = runApplication<BatchApplication>(*args)
    val exitCode = SpringApplication.exit(ctx)
    exitProcess(exitCode)
}