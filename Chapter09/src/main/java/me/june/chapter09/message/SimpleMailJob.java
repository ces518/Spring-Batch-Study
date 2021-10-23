package me.june.chapter09.message;

import javax.persistence.EntityManagerFactory;
import me.june.chapter09.domain.Customer;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaCursorItemReader;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaCursorItemReaderBuilder;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.mail.SimpleMailMessageItemWriter;
import org.springframework.batch.item.mail.builder.SimpleMailMessageItemWriterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;

@EnableBatchProcessing
@SpringBootApplication
@EntityScan(basePackages = "me.june.chapter09.domain")
public class SimpleMailJob {

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

    @Bean
    public JpaItemWriter<Customer> customerBatchWriter(EntityManagerFactory entityManagerFactory) {
        return new JpaItemWriterBuilder<Customer>()
            .entityManagerFactory(entityManagerFactory)
            .build();
    }

    @Bean
    public JpaCursorItemReader<Customer> customerCursorItemReader(
        EntityManagerFactory entityManagerFactory) {
        return new JpaCursorItemReaderBuilder<Customer>()
            .name("customerCursorItemReader")
            .entityManagerFactory(entityManagerFactory)
            .queryString("select c from Customer c")
            .build();
    }

    @Bean
    public SimpleMailMessageItemWriter emailItemWriter(MailSender mailSender) {
        return new SimpleMailMessageItemWriterBuilder()
            .mailSender(mailSender)
            .build();
    }

    @Bean
    public Step importStep() throws Exception {
        return this.stepBuilderFactory.get("importStep")
            .<Customer, Customer>chunk(10)
            .reader(customerFileReader(null))
            .writer(customerBatchWriter(null))
            .build();
    }

    @Bean
    public Step emailStep() throws Exception {
        return this.stepBuilderFactory.get("emailStep")
            .<Customer, SimpleMailMessage>chunk(10)
            .reader(customerCursorItemReader(null))
            .processor((ItemProcessor<Customer, SimpleMailMessage>) customer -> {
                SimpleMailMessage mail = new SimpleMailMessage();
                mail.setFrom("pupupee9@gamil.com");
                mail.setTo(customer.getEmail());
                mail.setSubject("Welcome!");
                mail.setText(
                    String.format(
                        "Welcome %s %s, \n You were imported into the system using Spring Batch!",
                        customer.getFirstName(), customer.getLastName())
                );
                return mail;
            })
            .writer(emailItemWriter(null))
            .build();
    }

    @Bean
    public Job emailJob() throws Exception {
        return this.jobBuilderFactory.get("emailJob")
            .start(importStep())
            .next(emailStep())
            .build();
    }

    public static void main(String[] args) {
        SpringApplication.run(SimpleMailJob.class,
            "customerFile=classpath:input/customerWithEmail.csv", "id=1");
    }
}
