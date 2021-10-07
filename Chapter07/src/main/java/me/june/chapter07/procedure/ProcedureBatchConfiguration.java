package me.june.chapter07.procedure;

import java.sql.Types;
import javax.sql.DataSource;
import me.june.chapter07.domain.Customer;
import me.june.chapter07.jdbc.CustomerRowMapper;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.StoredProcedureItemReader;
import org.springframework.batch.item.database.builder.StoredProcedureItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.ArgumentPreparedStatementSetter;
import org.springframework.jdbc.core.SqlInOutParameter;
import org.springframework.jdbc.core.SqlParameter;

@EnableBatchProcessing
@SpringBootApplication
public class ProcedureBatchConfiguration {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @StepScope
    @Bean
    public StoredProcedureItemReader<Customer> customerItemReader(
        DataSource dataSource,
        @Value("#{jobParameters['city']}") String city
    ) {
        return new StoredProcedureItemReaderBuilder<Customer>()
            .name("customerItemReader")
            .dataSource(dataSource)
            .procedureName("customer_list")
            .parameters(new SqlParameter[]{
                new SqlInOutParameter("cityOption", Types.VARCHAR)}
            )
            .preparedStatementSetter(new ArgumentPreparedStatementSetter(new Object[]{city}))
            .rowMapper(new CustomerRowMapper())
            .build();
    }
    @Bean
    public Step copyFileStep() {
        return this.stepBuilderFactory.get("copyFileStep")
            .<Customer, Customer>chunk(10)
            .reader(customerItemReader(null, null))
            .writer(itemWriter())
            .build();
    }

    @Bean
    public ItemWriter itemWriter() {
        return items -> items.forEach(System.out::println);
    }

    @Bean
    public Job json() {
        return this.jobBuilderFactory.get("procedureJob")
            .start(copyFileStep())
            .build();
    }

    public static void main(String[] args) {
        SpringApplication.run(ProcedureBatchConfiguration.class, "city=Chicago");
    }
}
