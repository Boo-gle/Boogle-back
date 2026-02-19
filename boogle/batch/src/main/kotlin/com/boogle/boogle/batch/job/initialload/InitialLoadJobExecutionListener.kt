package com.boogle.boogle.batch.job.initialload

import org.slf4j.LoggerFactory
import org.springframework.batch.core.job.JobExecution
import org.springframework.batch.core.listener.JobExecutionListener

class InitialLoadJobExecutionListener : JobExecutionListener {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun beforeJob(jobExecution: JobExecution) {
        log.info(
            "Job started. jobName={}, jobExecutionId={}, parameters={}",
            jobExecution.jobInstance.jobName,
            jobExecution.id,
            jobExecution.jobParameters
        )
    }

    override fun afterJob(jobExecution: JobExecution) {
        log.info(
            "Job finished. jobName={}, jobExecutionId={}, batchStatus={}, exitStatus={}",
            jobExecution.jobInstance.jobName,
            jobExecution.id,
            jobExecution.status,
            jobExecution.exitStatus
        )
    }
}