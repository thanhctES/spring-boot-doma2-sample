package com.sample.batch;

import javax.sql.DataSource;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.batch.core.jsr.JsrJobParametersConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.sample.batch.jobs.SingleJobCommandLineRunner;

import lombok.val;

@Configuration
@EnableBatchProcessing
public class BatchConfiguration {

    @Value("${application.batch.corePoolSize:2}")
    int corePoolSize = 2;

    @Value("${application.batch.maxPoolSize:64}")
    int maxPoolSize = 64;

    @Bean
    public JobParametersConverter jobParametersConverter(DataSource dataSource) {
        return new JsrJobParametersConverter(dataSource);
    }

    @Bean
    public SingleJobCommandLineRunner commandLineRunner() {
        return new SingleJobCommandLineRunner();
    }

    @Bean
    public ThreadPoolTaskExecutor taskExecutor() {
        val executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        return executor;
    }
}
