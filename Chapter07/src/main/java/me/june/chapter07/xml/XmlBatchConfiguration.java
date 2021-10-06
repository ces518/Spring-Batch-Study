package me.june.chapter07.xml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import me.june.chapter07.domain.Customer;
import me.june.chapter07.domain.Transaction;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.batch.item.xml.builder.StaxEventItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

@EnableBatchProcessing
@SpringBootApplication
public class XmlBatchConfiguration {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @StepScope
    @Bean
    public StaxEventItemReader<Customer> customerFileReader(
        @Value("#{jobParameters['customerFile']}")Resource inputFile
    ) {
        return new StaxEventItemReaderBuilder<Customer>()
            .name("customerFileReader")
            .resource(inputFile)
            .addFragmentRootElements("customer")
            .unmarshaller(customerMarshaller())
            .build();
    }

    @Bean
    public Jaxb2Marshaller customerMarshaller() {
        Jaxb2Marshaller jaxb2Marshaller = new Jaxb2Marshaller();
        jaxb2Marshaller.setClassesToBeBound(Customer.class, Transaction.class);
        return jaxb2Marshaller;
    }

    @Bean
    public Step copyFileStep() {
        return this.stepBuilderFactory.get("copyFileStep")
            .<Customer, Customer>chunk(10)
            .reader(customerFileReader(null))
            .writer(xmlItemWriter())
            .build();
    }

    @Bean
    public ItemWriter xmlItemWriter() {
        return items -> items.forEach(System.out::println);
    }

    @Bean
    public Job xmlJob() {
        return this.jobBuilderFactory.get("xmlJob")
            .start(copyFileStep())
            .build();
    }

    public static void main(String[] args) {
        List<String> realArgs = new ArrayList<>(Arrays.asList(args));
        realArgs.add("customerFile=classpath:input/customer.xml");

        SpringApplication.run(XmlBatchConfiguration.class, realArgs.toArray(new String[realArgs.size()]));
    }
}
