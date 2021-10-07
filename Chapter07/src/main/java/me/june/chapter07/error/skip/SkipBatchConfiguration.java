package me.june.chapter07.error.skip;

import me.june.chapter07.domain.Customer;
import me.june.chapter07.error.empty.EmptyInputStepFailure;
import me.june.chapter07.error.log.CustomerItemListener;
import me.june.chapter07.legacy.itemreader.CustomerItemReader;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@EnableBatchProcessing
@SpringBootApplication
public class SkipBatchConfiguration {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Bean
    public CustomerItemReader customerItemReader() {
        CustomerItemReader customerItemReader = new CustomerItemReader();
        customerItemReader.setName("customerItemReader");
        return customerItemReader;
    }

    @Bean
    public ItemWriter itemWriter() {
        return items -> items.forEach(System.out::println);
    }

    @Bean
    public Step copyFileStep() {
        return this.stepBuilderFactory.get("copyFileStep")
            .<Customer, Customer>chunk(10)
            .reader(customerItemReader()) // CustomItemReader 를 사용
            .writer(itemWriter())
            .faultTolerant()
//            .skip(ParseException.class) // ParseException 발생시 건너뜀
            .skip(Exception.class).noSkip(ParseException.class) // ParseException 을 제외한 모든 예외는 건너뜀
            .skipLimit(10) // 10번까지 건너뜀
            .listener(customerListener())
            .listener(emptyInputStepFailure())
            .build();
    }

    @Bean
    public CustomerItemListener customerListener() {
        return new CustomerItemListener();
    }

    @Bean
    public EmptyInputStepFailure emptyInputStepFailure() {
        return new EmptyInputStepFailure();
    }

    @Bean
    public Job job() {
        return this.jobBuilderFactory.get("itemReaderJob")
            .start(copyFileStep())
            .build();
    }

    public static void main(String[] args) {
        SpringApplication.run(SkipBatchConfiguration.class, args);
    }
}
