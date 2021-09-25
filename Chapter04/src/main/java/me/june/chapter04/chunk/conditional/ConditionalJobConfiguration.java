package me.june.chapter04.chunk.conditional;

import javax.sql.DataSource;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.BatchConfigurer;
import org.springframework.batch.core.configuration.annotation.DefaultBatchConfigurer;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@EnableBatchProcessing
@SpringBootApplication
public class ConditionalJobConfiguration {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Bean
    public Tasklet passTasklet() {
        return (stepContribution, chunkContext) -> RepeatStatus.FINISHED;
    }

    @Bean
    public Tasklet successTasklet() {
        return (stepContribution, chunkContext) -> {
            System.out.println("Success!");
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    public Tasklet failTasklet() {
        return (stepContribution, chunkContext) -> {
            System.out.println("Failure!");
            return RepeatStatus.FINISHED;
        };
    }

    /**
     * ExitStatus 를 이용해 다음에 어떤 스탭을 실행할 지 결정하게 할 수 있다. 하지만 이만으론 부족할때가 있음 특정 레코드를 건너뛰었다면 특정 스탭을 실행하지
     * 않게 한다거나 ... 이를 위해 JobExecutionDecider 인터페이스를 제공한다.
     */
    @Bean
    public Job job() {
        // pattern matching 을 지원함
        // ExitStatus 는 **문자열** 이기 때문에 패턴 매칭을 통해 여러 상태로 매칭할 수 있다.
        // ex) C* -> COMPLETE, CORRECT
        return this.jobBuilderFactory.get("conditionalJob")
            .start(firstStep())
            .next(decider())
            .from(decider())
            .on("FAILED")
//            .fail() // 실패시 FAIL 상태로 기록 / 동일한 잡으로 재실행 가능
            .stopAndRestart(successStep()) // 실패시 STOPPED 상태로 기록되며 / 지정한 step 부터 재시작 된다.
//            .to(failureStep()) // 실패할 경우 failureStep
            .from(decider()).on("*").to(successStep()) // 성공할 경우 successStep
            .end()
            .build();
    }

    @Bean
    public Step firstStep() {
        return this.stepBuilderFactory.get("firstStep")
            .tasklet(passTasklet())
            .build();
    }

    @Bean
    public Step successStep() {
        return this.stepBuilderFactory.get("successStep")
            .tasklet(successTasklet())
            .build();
    }

    @Bean
    public Step failureStep() {
        return this.stepBuilderFactory.get("failureStep")
            .tasklet(failTasklet())
            .build();
    }

    @Bean
    public JobExecutionDecider decider() {
        return new RandomDecider();
    }

    @Bean
    public BatchConfigurer batchConfigurer(DataSource dataSource) {
        return new DefaultBatchConfigurer(dataSource) {
            @Override
            public JobLauncher getJobLauncher() {
                return super.getJobLauncher();
            }
        };
    }

    public static void main(String[] args) {
        SpringApplication.run(ConditionalJobConfiguration.class, args);
    }
}
