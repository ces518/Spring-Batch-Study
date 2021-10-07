package me.june.chapter07.jdbc.paging;

import java.util.HashMap;
import java.util.Map;
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
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Paging 기법과 Cursor 기법의 차이는, 데이터베이스에서 데이터를 조회해오는 방식에서 의 차이이다.
 * 레코드 처리 자체는 한건 씩 처리된다.
 *
 * 페이징 처리는 PagingQueryProvider 구현체를 제공해야 한다.
 * 각 데이터베이스 마다 구현체를 제공한다.
 * - 각 구현체를 사용하거나 또는 SqlPagingQueryProviderFactoryBean 을 사용하면 데이터베이스를 감지할 수 있다.
 */
@EnableBatchProcessing
@SpringBootApplication
public class JdbcPagingBatchConfiguration {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @StepScope
    @Bean
    public JdbcPagingItemReader<Customer> customerItemReader(
        DataSource dataSource,
        PagingQueryProvider queryProvider,
        @Value("#{jobParameters['city']}") String city
    ) {
        Map<String, Object> parameterValues = new HashMap<>(1);
        parameterValues.put("city", city);
        return new JdbcPagingItemReaderBuilder<Customer>()
            .name("customerItemReader")
            .dataSource(dataSource)
            .queryProvider(queryProvider)
            .parameterValues(parameterValues)
            .pageSize(10)
            .rowMapper(new CustomerRowMapper())
            .build();
    }

    @Bean
    public SqlPagingQueryProviderFactoryBean pagingQueryProvider(DataSource dataSource) {
        SqlPagingQueryProviderFactoryBean factoryBean = new SqlPagingQueryProviderFactoryBean();
        factoryBean.setSelectClause("select *");
        factoryBean.setFromClause("from customer");
        factoryBean.setWhereClause("where city = :city"); // NamedParameter 방식을 사용해 파라미터를 Map 으롤 주입할 수 있다.
        factoryBean.setSortKey("last_name");
        factoryBean.setDataSource(dataSource);
        return factoryBean;
    }

    @Bean
    public ItemWriter itemWriter() {
        return items -> items.forEach(System.out::println);
    }

    @Bean
    public Step copyFileStep() {
        return this.stepBuilderFactory.get("copyFileStep")
            .<Customer, Customer>chunk(10)
            .reader(customerItemReader(null, null, null)) // CustomItemReader 를 사용
            .writer(itemWriter())
            .build();
    }

    @Bean
    public Job job() {
        return this.jobBuilderFactory.get("jdbcPagingJob")
            .start(copyFileStep())
            .build();
    }

    public static void main(String[] args) {
        SpringApplication.run(JdbcPagingBatchConfiguration.class, "city=Chicago");
    }
}
