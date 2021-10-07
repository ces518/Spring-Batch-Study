package me.june.chapter07.jpa;

import java.util.Collections;
import javax.persistence.EntityManagerFactory;
import me.june.chapter07.entity.Customer;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;

/**
 * JPA 는 Cursor 기능을 지원하지 않지만 페이징 기능은 지원한다.
 */
@EnableBatchProcessing
@SpringBootApplication
@EntityScan(basePackages = "me.june.chapter07.entity")
public class JpaCursorBatchConfiguration {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @StepScope
    @Bean
    public JpaPagingItemReader<Customer> customerItemReader(
        EntityManagerFactory entityManagerFactory,
        @Value("#{jobParameters['city']}") String city
    ) {
        CustomerByCityQueryProvider customerByCityQueryProvider = new CustomerByCityQueryProvider();
        customerByCityQueryProvider.setCityName(city);
        return new JpaPagingItemReaderBuilder<Customer>()
            .name("customerItemReader")
            .entityManagerFactory(entityManagerFactory)
//            .queryString("select c from Customer c where c.city = :city")
            .queryProvider(customerByCityQueryProvider)
            .parameterValues(Collections.singletonMap("city", city))
            .build();
    }

    @Bean
    public ItemWriter itemWriter() {
        return items -> items.forEach(System.out::println);
    }

    @Bean
    public Step copyFileStep() {
        return this.stepBuilderFactory.get("copyFileStep")
            .<Customer, Customer>chunk(10)
            .reader(customerItemReader(null, null)) // CustomItemReader 를 사용
            .writer(itemWriter())
            .build();
    }

    @Bean
    public Job job() {
        return this.jobBuilderFactory.get("jpaPagingJob")
            .start(copyFileStep())
            .build();
    }

    public static void main(String[] args) {
        SpringApplication.run(JpaCursorBatchConfiguration.class, "city=Chicago id=1");
    }
}
