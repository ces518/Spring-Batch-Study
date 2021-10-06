package me.june.chapter07.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import me.june.chapter07.domain.Customer;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.json.JacksonJsonObjectReader;
import org.springframework.batch.item.json.JsonItemReader;
import org.springframework.batch.item.json.builder.JsonItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;

@EnableBatchProcessing
@SpringBootApplication
public class JsonBatchConfiguration {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @StepScope
    @Bean
    public JsonItemReader<Customer> customerFileReader(
        @Value("#{jobParameters['customerFile']}")Resource inputFile
    ) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss"));

        JacksonJsonObjectReader<Customer> jsonObjectReader = new JacksonJsonObjectReader<>(Customer.class);
        jsonObjectReader.setMapper(objectMapper);

        return new JsonItemReaderBuilder<Customer>()
            .name("customerFileReader")
            .jsonObjectReader(jsonObjectReader)
            .resource(inputFile)
            .build();
    }

    @Bean
    public Step copyFileStep() {
        return this.stepBuilderFactory.get("copyFileStep")
            .<Customer, Customer>chunk(10)
            .reader(customerFileReader(null))
            .writer(jsonItemWriter())
            .build();
    }

    @Bean
    public ItemWriter jsonItemWriter() {
        return items -> items.forEach(System.out::println);
    }

    @Bean
    public Job jsonJob() {
        return this.jobBuilderFactory.get("jsonJob")
            .start(copyFileStep())
            .build();
    }

    public static void main(String[] args) {
        List<String> realArgs = new ArrayList<>(Arrays.asList(args));
        realArgs.add("customerFile=classpath:input/customer.json");

        SpringApplication.run(JsonBatchConfiguration.class, realArgs.toArray(new String[realArgs.size()]));
    }
}
