package me.june.chapter08.script;

import me.june.chapter08.domain.Customer;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.support.ScriptItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;

@EnableBatchProcessing
@SpringBootApplication
public class ScriptItemProcessorJob {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @StepScope
    @Bean
    public FlatFileItemReader<Customer> customerItemReader(
        @Value("#{jobParameters['customerFile']}") Resource resource
    ) {
        return new FlatFileItemReaderBuilder<Customer>()
            .name("customerItemReader")
            .delimited()
            .names(
                "firstName", "middleInitial", "lastName",
                "address", "city", "state", "zip"
            )
            .targetType(Customer.class)
            .resource(resource)
            .build();
    }

    @Bean
    public ItemWriter<Customer> itemWriter() {
        return (items) -> items.forEach(System.out::println);
    }

    @StepScope
    @Bean
    public ScriptItemProcessor<Customer, Customer> itemProcessor(
        @Value("#{jobParameters['script']}") Resource script
    ) {
        ScriptItemProcessor<Customer, Customer> processor = new ScriptItemProcessor<>();
        processor.setScript(script);
        return processor;
    }

    @Bean
    public Step copyFileStep() {
        return this.stepBuilderFactory.get("copyFileStep")
            .<Customer, Customer>chunk(5)
            .reader(customerItemReader(null))
            .processor(itemProcessor(null))
            .writer(itemWriter())
            .build();
    }

    @Bean
    public Job job() {
        return this.jobBuilderFactory.get("scriptJob")
            .start(copyFileStep())
            .build();
    }

    public static void main(String[] args) {
        SpringApplication.run(ScriptItemProcessorJob.class,
            "customerFile=classpath:input/customer.csv", "script=classpath:input/upperCase.js");
    }
}
