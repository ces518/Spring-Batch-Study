package me.june.chapter09.file.delimited;

import me.june.chapter09.domain.Customer;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;

@EnableBatchProcessing
@SpringBootApplication
public class DelimitedTextFileBatchConfiugration {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @StepScope
    @Bean
    public FlatFileItemReader<Customer> customerFileItemReader(
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

    @StepScope
    @Bean
    public FlatFileItemWriter<Customer> customerItemWriter(
        @Value("#{jobParameters['outputFile']}") Resource outputFile
    ) {
        return new FlatFileItemWriterBuilder<Customer>()
            .name("customerItemWriter")
            .resource(outputFile)
            .delimited()
            .delimiter(";")
            .names("firstName", "middleInitial", "lastName", "address", "city", "state", "zip")
            .build();
    }

    @Bean
    public Step step() {
        return this.stepBuilderFactory.get("delimitedStep")
            .<Customer, Customer>chunk(10)
            .reader(customerFileItemReader(null))
            .writer(customerItemWriter(null))
            .build();
    }

    @Bean
    public Job job() {
        return this.jobBuilderFactory.get("delimitedJob")
            .start(step())
            .incrementer(new RunIdIncrementer())
            .build();
    }

    public static void main(String[] args) {
        SpringApplication.run(DelimitedTextFileBatchConfiugration.class,
            "customerFile=classpath:input/customer.csv",
            "outputFile=file:delimitedCustomers.txt");
    }
}
