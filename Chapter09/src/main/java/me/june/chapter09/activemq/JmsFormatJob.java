package me.june.chapter09.activemq;

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
import org.springframework.batch.item.jms.JmsItemReader;
import org.springframework.batch.item.jms.JmsItemWriter;
import org.springframework.batch.item.jms.builder.JmsItemReaderBuilder;
import org.springframework.batch.item.jms.builder.JmsItemWriterBuilder;
import org.springframework.batch.item.xml.StaxEventItemWriter;
import org.springframework.batch.item.xml.builder.StaxEventItemWriterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.oxm.xstream.XStreamMarshaller;

/**
 * JMS JOB 실행순서 : 2 단계의 스탭으로 구성
 * 1. customer.csv 를 읽음 -> ActiveMQ 에 쏜다.
 * 2. ActiveMQ 로 부터 메세지를 읽음 -> XML 파일에 쓴다.
 */
@EnableBatchProcessing
@SpringBootApplication
public class JmsFormatJob {

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

    @StepScope
    @Bean
    public StaxEventItemWriter<Customer> xmlOutputWriter(
        @Value("#{jobParameters['outputFile']}") Resource outputFile
    ) {
        Map<String, Class> aliases = new HashMap<>();
        aliases.put("customer", Customer.class);

        XStreamMarshaller marshaller = new XStreamMarshaller();
        marshaller.setAliases(aliases);

        return new StaxEventItemWriterBuilder<Customer>()
            .name("xmlOutputWriter")
            .resource(outputFile)
            .marshaller(marshaller)
            .rootTagName("customers")
            .build();
    }

    @Bean
    public JmsItemReader<Customer> jmsItemReader(JmsTemplate jmsTemplate) {
        return new JmsItemReaderBuilder<Customer>()
            .jmsTemplate(jmsTemplate)
            .itemType(Customer.class)
            .build();
    }

    @Bean
    public JmsItemWriter<Customer> jmsItemWriter(JmsTemplate jmsTemplate) {
        return new JmsItemWriterBuilder<Customer>()
            .jmsTemplate(jmsTemplate)
            .build();
    }

    @Bean
    public Step formatInputStep() throws Exception {
        return this.stepBuilderFactory.get("formatInputStep")
            .<Customer, Customer>chunk(10)
            .reader(customerFileReader(null))
            .writer(jmsItemWriter(null))
            .build();
    }

    @Bean
    public Step formatOutputStep() throws Exception {
        return this.stepBuilderFactory.get("formatOutputStep")
            .<Customer, Customer>chunk(10)
            .reader(jmsItemReader(null))
            .writer(xmlOutputWriter(null))
            .build();
    }

    @Bean
    public Job job() throws Exception {
        return this.jobBuilderFactory.get("jmsFormatJob")
            .start(formatInputStep())
            .next(formatOutputStep())
            .build();
    }

    public static void main(String[] args) {
        SpringApplication.run(JmsFormatJob.class, "customerFile=classpath:input/customer.csv", "outputFile=file:result.xml");
    }
}
