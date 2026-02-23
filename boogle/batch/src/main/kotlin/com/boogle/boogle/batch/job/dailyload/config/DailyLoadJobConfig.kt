package com.boogle.boogle.batch.job.dailyload.config

import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DailyLoadJobConfig {

    @Bean
    fun dailyLoadJob(
        jobRepository: JobRepository,
        dailyLoadStep1_ingestBooks: Step,
        dailyLoadStep2_updateLastIngestedOn: Step,
    ): Job {
        return JobBuilder("dailyLoadJob", jobRepository)
            // ✅ Step1 성공 후 Step2로 진행
            .start(dailyLoadStep1_ingestBooks)
            .next(dailyLoadStep2_updateLastIngestedOn)
            .build()
    }
}