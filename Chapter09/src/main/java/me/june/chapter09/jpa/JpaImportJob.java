package me.june.chapter09.jpa;

import javax.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import me.june.chapter09.domain.Customer;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@EnableBatchProcessing
@SpringBootApplication
@RequiredArgsConstructor
@EntityScan(basePackages = "me.june.chapter09.domain")
public class JpaImportJob {

	private final JobBuilderFactory jobBuilderFactory;

	private final StepBuilderFactory stepBuilderFactory;


	@Bean
	@StepScope
	public FlatFileItemReader<Customer> customerFileReader(
			@Value("#{jobParameters['customerFile']}") Resource inputFile) {

		return new FlatFileItemReaderBuilder<Customer>()
				.name("customerFileReader")
				.resource(inputFile)
				.delimited()
				.names(new String[] {"firstName",
						"middleInitial",
						"lastName",
						"address",
						"city",
						"state",
						"zip"})
				.targetType(Customer.class)
				.build();
	}

	@Bean
	public JpaItemWriter<Customer> jpaItemWriter(EntityManagerFactory entityManagerFactory) {
		JpaItemWriter<Customer> jpaItemWriter = new JpaItemWriter<>();

		jpaItemWriter.setEntityManagerFactory(entityManagerFactory);

		return jpaItemWriter;
	}

	@Bean
	public Step jpaFormatStep() throws Exception {
		return this.stepBuilderFactory.get("jpaFormatStep")
				.<Customer, Customer>chunk(10)
				.reader(customerFileReader(null))
				.writer(jpaItemWriter(null))
				.build();
	}

	@Bean
	public Job jpaFormatJob() throws Exception {
		return this.jobBuilderFactory.get("jpaFormatJob")
				.start(jpaFormatStep())
				.build();
	}

    public static void main(String[] args) {
        SpringApplication.run(JpaImportJob.class, "customerFile=classpath:input/customer.csv", "id=8");
    }
}