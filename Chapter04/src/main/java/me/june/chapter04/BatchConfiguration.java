package me.june.chapter04;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.BatchConfigurer;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@EnableBatchProcessing
@Configuration
public class BatchConfiguration implements BatchConfigurer {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job job() {
        return this.jobBuilderFactory.get("job")
            .start(step1())
            .build();
    }

    @Bean
    public Step step1() {
        return this.stepBuilderFactory.get("step1")
            .tasklet(helloWorldTasklet())
            .build();
    }

    @Bean
    public Tasklet helloWorldTasklet() {
        return new HelloWorld();
    }

    @Override
    public JobRepository getJobRepository() throws Exception {
        return null;
    }

    @Override
    public PlatformTransactionManager getTransactionManager() throws Exception {
        return null;
    }

    @Override
    public JobLauncher getJobLauncher() throws Exception {
        return null;
    }

    @Override
    public JobExplorer getJobExplorer() throws Exception {
        return null;
    }
}
