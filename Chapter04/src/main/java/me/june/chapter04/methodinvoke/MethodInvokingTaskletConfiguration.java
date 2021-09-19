package me.june.chapter04.methodinvoke;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.step.tasklet.MethodInvokingTaskletAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableBatchProcessing
@SpringBootApplication
public class MethodInvokingTaskletConfiguration {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job methodInvokingJob() {
        return this.jobBuilderFactory.get("methodInvokingJob")
            .start(methodInvokingStep())
            .build();
    }

    @Bean
    public Step methodInvokingStep() {
        return this.stepBuilderFactory.get("methodInvokingStep")
            .tasklet(methodInvokingTasklet(null))
            .build();
    }

    /**
     * TargetObject 의 메소드가 ExitStatus 를 반환하지 않는다면, ExitStatus.COMPLETED 를 반환한다.
     */
    @StepScope
    @Bean
    public MethodInvokingTaskletAdapter methodInvokingTasklet(
        @Value("#{jobParameters['message']}") String message
    ) {
        MethodInvokingTaskletAdapter adapter = new MethodInvokingTaskletAdapter();
        adapter.setTargetObject(service());
        adapter.setTargetMethod("serviceMethod");
        adapter.setArguments(new String[] {message});
        return adapter;
    }

    @Bean
    public CustomerService service() {
        return new CustomerService();
    }

    static class CustomerService {

        public void serviceMethod(String message) {
            System.out.println(String.format("Service method was called... message = %s", message));
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(MethodInvokingTaskletConfiguration.class, args);
    }
}
