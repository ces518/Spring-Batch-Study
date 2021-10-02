package me.june.chapter06.rest;

import java.util.Properties;
import lombok.Getter;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@EnableBatchProcessing
@SpringBootApplication
public class RestApplication {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Bean("restJob")
    public Job job() {
        return this.jobBuilderFactory.get("restJob")
            .incrementer(new RunIdIncrementer())
            .start(step1())
            .build();
    }

    @Bean
    public Step step1() {
        return this.stepBuilderFactory.get("step1")
            .tasklet((contribution, chunkContext) -> {
                System.out.println("step1 run!");
                return RepeatStatus.FINISHED;
            })
            .build();
    }

    @RestController
    public static class JobLaunchingController {

        @Autowired
        private JobLauncher jobLauncher;

        @Autowired
        private ApplicationContext context;

        @Autowired
        private JobExplorer explorer;

        @PostMapping("/run")
        public ExitStatus runJob(@RequestBody JobLaunchRequest request) throws Exception {
            Job job = this.context.getBean(request.getName(), Job.class);
            JobParameters jobParameters = new JobParametersBuilder(request.getJobParameters(), explorer)
                .getNextJobParameters(job) // RunIdIncrementor 활성화
                .toJobParameters();
            return this.jobLauncher.run(job, jobParameters).getExitStatus();
        }
    }

    @Getter
    public static class JobLaunchRequest {

        private String name;
        private Properties jobParameters;

        public JobParameters getJobParameters() {
            return new JobParametersBuilder(this.jobParameters)
                .toJobParameters();
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(RestApplication.class, args);
    }
}
