package me.june.chapter09.hibernate;

import java.util.Collections;
import javax.persistence.EntityManagerFactory;
import me.june.chapter09.domain.Customer;
import org.hibernate.SessionFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.database.HibernateItemWriter;
import org.springframework.batch.item.database.builder.HibernateItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;

@EnableBatchProcessing
@SpringBootApplication
@EntityScan(basePackages = "me.june.chapter09.domain")
public class HibernateImportJob {

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

//	@StepScope
	@Bean
	public HibernateItemWriter<Customer> hibernateItemWriter(EntityManagerFactory entityManagerFactory) {
		return new HibernateItemWriterBuilder<Customer>()
				.sessionFactory(entityManagerFactory.unwrap(SessionFactory.class))
				.build();
	}

	@Bean
	public Step hibernateFormatStep() throws Exception {
		return this.stepBuilderFactory.get("hibernateFormatStep")
				.<Customer, Customer>chunk(10)
				.reader(customerFileReader(null))
				.writer(hibernateItemWriter(null))
				.build();
	}

	@Bean
	public Job hibernateFormatJob() throws Exception {
		return this.jobBuilderFactory.get("hibernateFormatJob")
				.start(hibernateFormatStep())
				.build();
	}

    public static void main(String[] args) {
        SpringApplication.run(HibernateImportJob.class,
            "customerFile=classpath:input/customer.csv", "id=10");

    }
}
