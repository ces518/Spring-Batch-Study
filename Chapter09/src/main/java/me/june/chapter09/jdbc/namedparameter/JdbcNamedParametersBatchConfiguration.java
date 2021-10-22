package me.june.chapter09.jdbc.namedparameter;

import javax.sql.DataSource;
import me.june.chapter09.domain.Customer;
import me.june.chapter09.jdbc.preparedstatement.CustomerItemPreparedStatementSetter;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;

@EnableBatchProcessing
@SpringBootApplication
public class JdbcNamedParametersBatchConfiguration {

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
    public JdbcBatchItemWriter<Customer> jdbcCustomerWriter(DataSource dataSource)
        throws Exception {
        return new JdbcBatchItemWriterBuilder<Customer>()
            .dataSource(dataSource)
            .sql(""
                + "INSERT INTO customer (first_name, middle_initial, last_name, address, city, state, zip) "
                + "VALUES (:firstName, :middleInitial, :lastName, :address, :city, :state, :zip)")
            .beanMapped()
            .build();
    }

    @Bean
    public Step step() throws Exception {
        return this.stepBuilderFactory.get("jdbcStep")
            .<Customer, Customer>chunk(10)
            .reader(customerFileReader(null))
            .writer(jdbcCustomerWriter(null))
            .build();
    }

    @Bean
    public Job job() throws Exception {
        return this.jobBuilderFactory.get("jdbcNameParametersJob")
            .start(step())
            .build();
    }

    public static void main(String[] args) {
        SpringApplication.run(JdbcNamedParametersBatchConfiguration.class,
            "customerFile=classpath:input/customer.csv");
    }

}
