package me.june.chapter04;

import java.util.List;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.CompositeJobParametersValidator;
import org.springframework.batch.core.job.DefaultJobParametersValidator;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.listener.ExecutionContextPromotionListener;
import org.springframework.batch.core.listener.JobListenerFactoryBean;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

//@EnableBatchProcessing
@SpringBootApplication
public class Chapter04Application {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

//    @Bean
    public Job job() {
        return this.jobBuilderFactory.get("basicJob")
            .start(step1())
            .validator(validator()) // validator 지정
            .incrementer(new DailyJobTimestamper()) // incrementer 지정
            // incrementer 변경시 주의... 이전에 실행되었던 잡의 파라미터도 체크함
//            .listener(new JobLoggerListener()) // interface 기반 listener 추가
            .listener(JobListenerFactoryBean.getListener(new JobLoggerListener())) // 애노테이션 기반 listener 추가
            .build();
    }

//    @Bean
    public Step step1() {
        return this.stepBuilderFactory.get("step1")
            .tasklet(helloWorldTasklet())
            .listener(promotionListener())
            .build();
    }

    @StepScope // StepScope 적용
//    @Bean
    public Tasklet helloWorldTasklet(
        @Value("#{jobParameters['name']}") String name, // Late Binding Step Scope 를 적용해야 한다.
        @Value("#{jobParameters['fileName']}") String fileName
    ) {
        return (stepContribution, chunkContext) -> {
//            String name = (String) chunkContext.getStepContext()
//                .getJobParameters()
//                .get("name");
            System.out.println(String.format("Hello, %s !", name));
            System.out.println(String.format("fileName = %s !", fileName));
            return RepeatStatus.FINISHED;
        };
    }

//    @Bean
//    public JobParametersValidator validator() {
//        DefaultJobParametersValidator validator = new DefaultJobParametersValidator();
//        validator.setRequiredKeys(new String[]{"fileName"});
//        validator.setRequiredKeys(new String[]{"name"});
//        return validator;
//    }

    /**
     * 다수의 유효성 검증기를 사용하는 CompositeJobParametersValidator
     */
//    @Bean
    public CompositeJobParametersValidator validator() {
        CompositeJobParametersValidator validator = new CompositeJobParametersValidator();
        DefaultJobParametersValidator defaultJobParametersValidator = new DefaultJobParametersValidator(
            new String[]{"fileName"},
            new String[]{"name", "currentDate"}
        );
        defaultJobParametersValidator.afterPropertiesSet();
        validator.setValidators(
            List.of(new ParameterValidator(), defaultJobParametersValidator)
        );
        return validator;
    }

//    @Bean
    public Tasklet helloWorldTasklet() {
        return new HelloWorld();
    }

    /**
     * JobExecution 의 Execution Context 를 조작하는 또 다른 방법
     * > StepExecution 의 Execution Context 의 키를 JobExecution 의 Execution Context 로 승격시키는 방법
     * 스탭간의 공유할 데이터가 있지만, 첫 번째 스탭이 성공후 공유하고 싶다거나 할 때 유용하다.
     *
     * 또 다른 방법은 ItemStream 인터페이스를 사용하는 방법
     */
//    @Bean
    public StepExecutionListener promotionListener() {
        /**
         * name 키를 찾으면 JobExecution 의 ExecutionContext 로 복사한다.
         * name 키가 없어도 아무일도 일어나지 않지만, 예외가 발생하도록 구성도 가능하다.
         */
        ExecutionContextPromotionListener listener = new ExecutionContextPromotionListener();
        listener.setKeys(new String[] {"name"});
        return listener;
    }

    public static void main(String[] args) {
        SpringApplication.run(Chapter04Application.class, args);
    }
}
