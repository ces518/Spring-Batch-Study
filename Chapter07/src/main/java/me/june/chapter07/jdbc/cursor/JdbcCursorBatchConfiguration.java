package me.june.chapter07.jdbc.cursor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.ArgumentPreparedStatementSetter;

/**
 * Cursor 방식은 1건씩 데이터를 **스트리밍** 한다.
 * 대용량 데이터일 경우 1건씩 처리하기 때문에 매번 데이터를 읽어올 때 마다 네트워크 오버헤드가 발생한다.
 */
@EnableBatchProcessing
@SpringBootApplication
public class JdbcCursorBatchConfiguration {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Bean
    public JdbcCursorItemReader<Customer> customerItemReader(DataSource dataSource) {
        return new JdbcCursorItemReaderBuilder<Customer>()
            .name("customerItemReader")
            .dataSource(dataSource)
            .sql("select * from customer where city = ?")
            .rowMapper(new CustomerRowMapper())
            .preparedStatementSetter(citySetter(null))
            .build();
    }

    /**
     * ArgumentPreparedStatementSetter 의 인자가 SqlParameterValue 타입일 경우,  값을 설정하는 메타 데이터가 포함되어 있다.
     * 해당 타입이 아닌경우, 인자로 넘어온 배열의 순서대로 ? 에 바인딩 해준다.
     *
     * @see org.springframework.jdbc.core.SqlParameterValue
     */
    @StepScope
    @Bean
    public ArgumentPreparedStatementSetter citySetter(
        @Value("#{jobParameters['city']}") String city
    ) {
        return new ArgumentPreparedStatementSetter(new Object[]{city});
    }

    @Bean
    public Step copyFileStep() {
        return this.stepBuilderFactory.get("copyFileStep")
            .<Customer, Customer>chunk(10)
            .reader(customerItemReader(null)) // CustomItemReader 를 사용
            .writer(itemWriter())
            .build();
    }

    @Bean
    public ItemWriter itemWriter() {
        return items -> items.forEach(System.out::println);
    }

    @Bean
    public Job job() {
        return this.jobBuilderFactory.get("jdbcJob")
            .start(copyFileStep())
            .build();
    }

    public static void main(String[] args) {
        List<String> realArgs = new ArrayList<>(Arrays.asList(args));
        realArgs.add("id=4");
        realArgs.add("city=Chicago");

        SpringApplication.run(JdbcCursorBatchConfiguration.class, realArgs.toArray(new String[realArgs.size()]));
    }
}
