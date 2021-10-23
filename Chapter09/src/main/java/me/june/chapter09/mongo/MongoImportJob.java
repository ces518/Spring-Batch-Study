package me.june.chapter09.mongo;

import me.june.chapter09.domain.Customer;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.step.item.Chunk;
import org.springframework.batch.item.data.MongoItemWriter;
import org.springframework.batch.item.data.builder.MongoItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.core.MongoOperations;

@EnableBatchProcessing
@SpringBootApplication
public class MongoImportJob {

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
    public MongoItemWriter<Customer> mongoItemWriter(MongoOperations mongoTemplate) {
        return new MongoItemWriterBuilder<Customer>()
            .collection("customers")
            .template(mongoTemplate)
            .build();
    }

    @Bean
    public Step mongoFormatStep() throws Exception {
        return this.stepBuilderFactory.get("mongoFormatStep")
            .<Customer, Customer>chunk(10)
            .reader(customerFileReader(null))
            .writer(mongoItemWriter(null))
            .build();
    }

    @Bean
    public Job job() throws Exception {
        return this.jobBuilderFactory.get("mongoFormatJob")
            .start(mongoFormatStep())
            .build();
    }

    public static void main(String[] args) {
        SpringApplication.run(MongoImportJob.class, "customerFile=classpath:input/customer.csv");
    }
}
