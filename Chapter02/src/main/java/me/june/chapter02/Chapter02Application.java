package me.june.chapter02;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@EnableBatchProcessing
@SpringBootApplication
public class Chapter02Application {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Bean
    public Step step() {
        return this.stepBuilderFactory.get("step1")
            .tasklet((stepContribution, chunkContext) -> {
                System.out.println("Hello, World!!");
                /**
                 * 테스클릿이 FINISHED 상태가 되었음을 알림, CONTINUABLE 상태는 지속되어야 함을 알림
                 * CONTINUABLE 상태를 계속해서 반환한다면 스탭이 지속적으로 실행됨에 유의
                 */
                return RepeatStatus.FINISHED;
            }).build();
    }

    @Bean
    public Job job() {
        return this.jobBuilderFactory.get("job")
            .start(step())
            .build();
    }

    public static void main(String[] args) {
        SpringApplication.run(Chapter02Application.class, args);
    }

}
