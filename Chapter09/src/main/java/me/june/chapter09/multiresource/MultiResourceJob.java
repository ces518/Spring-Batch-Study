package me.june.chapter09.multiresource;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.EntityManagerFactory;
import me.june.chapter09.domain.Customer;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.database.JpaCursorItemReader;
import org.springframework.batch.item.database.builder.JpaCursorItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.MultiResourceItemWriter;
import org.springframework.batch.item.file.builder.MultiResourceItemWriterBuilder;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.FormatterLineAggregator;
import org.springframework.batch.item.xml.StaxEventItemWriter;
import org.springframework.batch.item.xml.builder.StaxEventItemWriterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.oxm.xstream.XStreamMarshaller;

@EnableBatchProcessing
@SpringBootApplication
@EntityScan(basePackages = "me.june.chapter09.domain")
public class MultiResourceJob {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Bean
    public JpaCursorItemReader<Customer> customerCursorItemReader(
        EntityManagerFactory entityManagerFactory
    ) {
        return new JpaCursorItemReaderBuilder<Customer>()
            .name("customerCursorItemReader")
            .entityManagerFactory(entityManagerFactory)
            .queryString("select c from Customer c")
            .build();
    }

    @StepScope
    @Bean
    public StaxEventItemWriter<Customer> delegateItemWriter() throws Exception {
        Map<String, Class> aliases = new HashMap<>();
        aliases.put("customer", Customer.class);

        XStreamMarshaller marshaller = new XStreamMarshaller();
        marshaller.setAliases(aliases);
        marshaller.afterPropertiesSet();

        return new StaxEventItemWriterBuilder<Customer>()
            .name("customerItemWriter")
            .marshaller(marshaller)
            .rootTagName("customers")
            .headerCallback(headerCallback())
            .build();
    }

    @StepScope
    @Bean
    public FlatFileItemWriter<Customer> delegateCustomerItemWriter(
        CustomerRecordCountFooterCallback footerCallback
    ) throws Exception {
        BeanWrapperFieldExtractor<Customer> fieldExtractor = new BeanWrapperFieldExtractor<>();
        fieldExtractor.setNames(
            new String[]{"firstName", "lastName", "address", "city", "state", "zip"}
        );
        fieldExtractor.afterPropertiesSet();

        FormatterLineAggregator<Customer> lineAggregator = new FormatterLineAggregator<>();
        lineAggregator.setFormat("%s %s lived at %s %s in %s, %s.");
        lineAggregator.setFieldExtractor(fieldExtractor);

        FlatFileItemWriter<Customer> itemWriter = new FlatFileItemWriter<>();
        itemWriter.setName("delegateCustomerItemWriter");
        itemWriter.setLineAggregator(lineAggregator);
        itemWriter.setAppendAllowed(true);
        itemWriter.setFooterCallback(footerCallback);
        return itemWriter;
    }

    @Bean
    public MultiResourceItemWriter<Customer> multiCustomerFilerWriter() throws Exception {
        return new MultiResourceItemWriterBuilder<Customer>()
            .name("multiCustomerFilerWriter")
            .delegate(delegateCustomerItemWriter(null)) // 실제 아이템을 처리할 writer
            .itemCountLimitPerResource(25) // 25개 단위로 파일을 쓴다.
            .resource(new FileSystemResource("Chapter09/customer"))
//            .resourceSuffixCreator(suffixCreator())
            .build();
    }

    @Bean
    public CustomerOutputFileSuffixCreator suffixCreator() throws Exception {
        return new CustomerOutputFileSuffixCreator();
    }

    @Bean
    public CustomerXmlHeaderCallback headerCallback() throws Exception {
        return new CustomerXmlHeaderCallback();
    }

    @Bean
    public Step multiXmlGeneratorStep() throws Exception {
        return this.stepBuilderFactory.get("multiXmlGeneratorStep")
            .<Customer, Customer>chunk(10)
            .reader(customerCursorItemReader(null))
            .writer(multiCustomerFilerWriter())
            .build();
    }

    @Bean
    public Job xmlGeneratorJob() throws Exception {
        return this.jobBuilderFactory.get("xmlGeneratorJob")
            .start(multiXmlGeneratorStep())
            .build();
    }

    public static void main(String[] args) {
        SpringApplication.run(MultiResourceJob.class, "id=5");
    }
}
