package me.june.chapter09.neo4j;

import me.june.chapter09.domain.Customer;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.data.Neo4jItemWriter;
import org.springframework.batch.item.data.builder.Neo4jItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;

/**
 * 2.4.x 부터 Neo4j 기본 드라이버가 Java Driver 로 변경됨
 * 이로 인해 불편 ㅜ
 */
@EnableBatchProcessing
@SpringBootApplication
public class Neo4jImportJob {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @StepScope
    @Bean
    public FlatFileItemReader<Customer> customerFileReader(
        @Value("#{jobParameters['customerFile']}") Resource inputFile
    ) {
        return new FlatFileItemReaderBuilder<Customer>()
            .name("customerFileReader")
            .resource(inputFile)
            .delimited()
            .names("firstName", "middleInitial", "lastName", "address", "city", "state", "zip")
            .targetType(Customer.class)
            .build();
    }

    @Bean
    public Neo4jItemWriter<Customer> neo4jItemWriter(SessionFactory sessionFactory) {
        return new Neo4jItemWriterBuilder<Customer>()
            .sessionFactory(sessionFactory)
            .build();
    }

    @Bean
    public Step neo4jFormatStep() throws Exception {
        return this.stepBuilderFactory.get("neo4jFormatStep")
            .<Customer, Customer>chunk(10)
            .reader(customerFileReader(null))
            .writer(neo4jItemWriter(null))
            .build();
    }

    @Bean
    public Job neo4jFormatJob() throws Exception {
        return this.jobBuilderFactory.get("neo4jFormatJob")
            .start(neo4jFormatStep())
            .build();
    }

    public static void main(String[] args) {
        SpringApplication.run(Neo4jImportJob.class, "customerFile=classpath:input/customer.csv");
    }
}
