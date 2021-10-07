package me.june.chapter07.hibernate.paging;

import java.util.Collections;
import javax.persistence.EntityManagerFactory;
import me.june.chapter07.domain.Customer;
import org.hibernate.SessionFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.HibernatePagingItemReader;
import org.springframework.batch.item.database.builder.HibernatePagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;

@EnableBatchProcessing
@SpringBootApplication
@EntityScan(basePackages = "me.june.chapter07.entity")
public class HibernatePagingBatchConfiguration {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @StepScope
    @Bean
    public HibernatePagingItemReader<Customer> customerItemReader(
        EntityManagerFactory entityManagerFactory,
        @Value("#{jobParameters['city']}") String city
    ) {
        return new HibernatePagingItemReaderBuilder<Customer>()
            .name("customerItemReader")
            .sessionFactory(entityManagerFactory.unwrap(SessionFactory.class))
            .queryString("from Customer where city = :city")
            .parameterValues(Collections.singletonMap("city", city))
            .pageSize(10)
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
        return this.jobBuilderFactory.get("hibernatePagingJob")
            .start(copyFileStep())
            .build();
    }

    public static void main(String[] args) {
        SpringApplication.run(HibernatePagingBatchConfiguration.class, "city=Chicago");
    }
}
