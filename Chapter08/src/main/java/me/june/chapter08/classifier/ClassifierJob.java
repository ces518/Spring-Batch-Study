package me.june.chapter08.classifier;

import me.june.chapter08.domain.Customer;
import me.june.chapter08.itemprocessor.UpperCaseNameService;
import me.june.chapter08.validation.UniqueLastNameValidator;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.adapter.ItemProcessorAdapter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.support.ClassifierCompositeItemProcessor;
import org.springframework.batch.item.support.ScriptItemProcessor;
import org.springframework.batch.item.validator.ValidatingItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.classify.Classifier;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;

@EnableBatchProcessing
@SpringBootApplication
public class ClassifierJob {

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

    @Bean
    public UniqueLastNameValidator validator() {
        UniqueLastNameValidator validator = new UniqueLastNameValidator();
        validator.setName("validator");
        return validator;
    }

    @Bean
    public ValidatingItemProcessor<Customer> customerValidatingItemProcessor() {
        ValidatingItemProcessor<Customer> itemProcessor = new ValidatingItemProcessor<>(
            validator());
        itemProcessor.setFilter(true);
        return itemProcessor;
    }

    @Bean
    public ItemProcessorAdapter<Customer, Customer> upperCaseItemProcessor(
        UpperCaseNameService service) {
        ItemProcessorAdapter<Customer, Customer> adapter = new ItemProcessorAdapter<>();
        adapter.setTargetObject(service);
        adapter.setTargetMethod("upperCase");
        return adapter;
    }

    @StepScope
    @Bean
    public ScriptItemProcessor<Customer, Customer> scriptItemProcessor(
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
            .processor(itemProcessor())
            .writer(itemWriter())
            .build();
    }

    /**
     * ZipCode 가 홀수면 UpperCase / 짝수면 lowerCase 를 적용한다.
     */
    @Bean
    public Classifier classifier() {
        return new ZipCodeClassifier(upperCaseItemProcessor(null), scriptItemProcessor(null));
    }

    @Bean
    public ClassifierCompositeItemProcessor<Customer, Customer> itemProcessor() {
        ClassifierCompositeItemProcessor<Customer, Customer> itemProcessor = new ClassifierCompositeItemProcessor<>();
        itemProcessor.setClassifier(classifier());
        return itemProcessor;
    }

    @Bean
    public Job job() {
        return this.jobBuilderFactory.get("classifierJob")
            .start(copyFileStep())
            .build();
    }

    @Bean
    public UpperCaseNameService upperCaseNameService() {
        return new UpperCaseNameService();
    }

    public static void main(String[] args) {
        SpringApplication.run(ClassifierJob.class, "customerFile=classpath:input/customer.csv",
            "script=classpath:input/lowerCase.js", "id=1");
    }
}
