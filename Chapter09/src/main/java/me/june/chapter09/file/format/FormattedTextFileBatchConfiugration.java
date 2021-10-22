package me.june.chapter09.file.format;

import me.june.chapter09.domain.Customer;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;

@EnableBatchProcessing
@SpringBootApplication
public class FormattedTextFileBatchConfiugration {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @StepScope
    @Bean
    public FlatFileItemReader<Customer> customerFileItemReader(
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
    public FlatFileItemWriter<Customer> customerItemWriter(
        @Value("#{jobParameters['outputFile']}") Resource outputFile
    ) {
        return new FlatFileItemWriterBuilder<Customer>()
            .name("customerItemWriter")
            .resource(outputFile)
            .formatted()
            .format("%s %s lived at %s %s in %s, %s.")
            .names("firstName", "middleInitial", "lastName", "address", "city", "state", "zip")
            .shouldDeleteIfEmpty(true) // 스탭 완료시 사용된다. 기본값 false / 스탭 종료후 아무런 것도 파일에 작성되지 않았다면 (헤더,푸터 제외) 스탭완료시 해당 파일 삭제
            .shouldDeleteIfExists(false) // 스탭의 쓰기작업 대상 파일이 존재한다면 해당 파일을 삭제한다. 기본값 true / false 로 지정된 경우에 파일이 존재한다면 ItemStreamException 이 발생
            .append(true) // 기본값 false , true 로 지정한다면  shouldDeleteIfExists 값을 false 로 자동지정하고 결과 파일이 없다면 새파일을 생성, 결과 파일에 있다면 기존 파일에 데이터를 추가한다.
            .build();
    }

    @Bean
    public Step formatStep() {
        return this.stepBuilderFactory.get("formatStep")
            .<Customer, Customer>chunk(10)
            .reader(customerFileItemReader(null))
            .writer(customerItemWriter(null))
            .build();
    }

    @Bean
    public Job formatJob() {
        return this.jobBuilderFactory.get("formatJob")
            .start(formatStep())
            .incrementer(new RunIdIncrementer())
            .build();
    }

    public static void main(String[] args) {
        SpringApplication.run(FormattedTextFileBatchConfiugration.class,
            "customerFile=classpath:input/customer.csv",
            "outputFile=file:formattedCustomers.txt");
    }
}
