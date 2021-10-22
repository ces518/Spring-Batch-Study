package me.june.chapter09.file.xml;

import java.util.HashMap;
import java.util.Map;
import me.june.chapter09.domain.Customer;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.xml.StaxEventItemWriter;
import org.springframework.batch.item.xml.builder.StaxEventItemWriterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.oxm.xstream.XStreamMarshaller;

@EnableBatchProcessing
@SpringBootApplication
public class XmlBatchConfiguration {

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

    @StepScope
    @Bean
    public StaxEventItemWriter<Customer> xmlCustomerWriter(
        @Value("#{jobParameters['outputFile']}") Resource outputFile
    ) {
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
    public Step xmlFormatStep() throws Exception {
        return this.stepBuilderFactory.get("xmlFormatStep")
            .<Customer, Customer>chunk(10)
            .reader(customerFileReader(null))
            .writer(xmlCustomerWriter(null))
            .build();
    }

    @Bean
    public Job xmlFormatJob() throws Exception {
        return this.jobBuilderFactory.get("xmlFormatJob")
            .start(xmlFormatStep())
            .build();
    }

    public static void main(String[] args) {
        SpringApplication.run(XmlBatchConfiguration.class,
            "customerFile=classpath:input/customer.csv", "outputFile=file:xmlCustomer.xml");
    }

}
