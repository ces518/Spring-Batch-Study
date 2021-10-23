package me.june.chapter09.composite;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManagerFactory;
import me.june.chapter09.domain.Customer;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.support.ClassifierCompositeItemWriter;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.batch.item.support.builder.ClassifierCompositeItemWriterBuilder;
import org.springframework.batch.item.support.builder.CompositeItemWriterBuilder;
import org.springframework.batch.item.xml.StaxEventItemWriter;
import org.springframework.batch.item.xml.builder.StaxEventItemWriterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.classify.Classifier;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.oxm.xstream.XStreamMarshaller;

/**
 * 몇개의 Writer 를 사용하던지, 스프링배치는 아이템의 수를 세고했다. 때문에 Writer 를 2개 조합해서 사용했다고 해서, WriterCount 가 2배가 되진
 * 않는다.
 */
@EnableBatchProcessing
@SpringBootApplication
@EntityScan(basePackages = "me.june.chapter09.domain")
public class CompositeItemWriterJob {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @StepScope
    @Bean
    public FlatFileItemReader<Customer> customerFileReader(
        @Value("#{jobParameters['customerFile']}") Resource inputFile) {

        return new FlatFileItemReaderBuilder<Customer>()
            .name("customerFileReader")
            .resource(inputFile)
            .delimited()
            .names(new String[]{"firstName",
                "middleInitial",
                "lastName",
                "address",
                "city",
                "state",
                "zip",
                "email"})
            .targetType(Customer.class)
            .build();
    }

    @StepScope
    @Bean
    public StaxEventItemWriter<Customer> xmlDelegateItemWriter(
        @Value("#{jobParameters['outputFile']}") Resource outputFile
    ) throws Exception {
        Map<String, Class> aliases = new HashMap<>();
        aliases.put("customer", Customer.class);

        XStreamMarshaller marshaller = new XStreamMarshaller();
        marshaller.setAliases(aliases);
        marshaller.afterPropertiesSet();

        return new StaxEventItemWriterBuilder<Customer>()
            .name("customerItemWriter")
            .resource(outputFile)
            .marshaller(marshaller)
            .rootTagName("customers")
            .build();
    }

    @Bean
    public JpaItemWriter<Customer> jpaDeleteItemWriter(EntityManagerFactory entityManagerFactory) {
        return new JpaItemWriterBuilder<Customer>()
            .entityManagerFactory(entityManagerFactory)
            .build();
    }

    @Bean
    public CompositeItemWriter<Customer> compositeItemWriter() throws Exception {
        return new CompositeItemWriterBuilder<Customer>()
            .delegates(
                List.of(xmlDelegateItemWriter(null), jpaDeleteItemWriter(null))
            )
            .build();
    }

    @Bean
    public ClassifierCompositeItemWriter<Customer> classifierCompositeItemWriter()
        throws Exception {
        Classifier<Customer, ItemWriter<? super Customer>> classifier =
            new CustomerClassifier(xmlDelegateItemWriter(null), jpaDeleteItemWriter(null));
        return new ClassifierCompositeItemWriterBuilder<Customer>()
            .classifier(classifier)
            .build();
    }

    @Bean
    public Step compositeWriterStep() throws Exception {
        return this.stepBuilderFactory.get("compositeWriterStep")
            .<Customer, Customer>chunk(10)
            .reader(customerFileReader(null))
            .writer(compositeItemWriter())
            .build();
    }

    @Bean
    public Job compositeWriterJob() throws Exception {
        return this.jobBuilderFactory.get("compositeWriterJob")
            .start(compositeWriterStep())
            .build();
    }

    public static void main(String[] args) {
        SpringApplication.run(CompositeItemWriterJob.class,
            "customerFile=classpath:input/customerWithEmail.csv",
            "outputFile=file:xmlCustomer.xml",
            "id=1");
    }
}
