package me.june.chapter06.csv;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;
import me.june.chapter06.csv.domain.AccountSummary;
import me.june.chapter06.csv.domain.Transaction;
import me.june.chapter06.csv.domain.TransactionDao;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.mapping.PassThroughFieldSetMapper;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;

@EnableBatchProcessing
@SpringBootApplication
public class TransactionProcessingJob {

    private static final String CREATE_TRANSACTION =
        "insert into Transactions (account_summary_id, timestamp, amount) "
        + "values ("
        + "(select id from Account_Summary where account_number = :accountNumber),"
        + ":timestamp, :amount"
        + ")";

    private static final String GET_ACCOUNT_SUMMARY =
        "select account_number, current_balance from Account_Summary a "
        + "where a.id in ("
        + "select distinct t.account_summary_id from Transactions t"
        + ") order by a.account_number";

    private static final String UPDATE_SUMMARY =
        "update Account_Summary "
        + "set current_balance = :currentBalance "
        + "where account_number = :accountNumber";

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @StepScope
    @Bean
    public TransactionReader transactionReader() {
        return new TransactionReader(fileItemReader(null));
    }

    @StepScope
    @Bean
    public FlatFileItemReader<FieldSet> fileItemReader(
        @Value("#{jobParameters['transactionFile']}") Resource inputFile
    ) {
        return new FlatFileItemReaderBuilder<FieldSet>()
            .name("fileItemReader")
            .resource(inputFile)
            .lineTokenizer(new DelimitedLineTokenizer())
            .fieldSetMapper(new PassThroughFieldSetMapper())
            .build();
    }

    @Bean
    public JdbcBatchItemWriter<Transaction> transactionWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<Transaction>()
            .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
            .sql(CREATE_TRANSACTION)
            .dataSource(dataSource)
            .build();
    }

    @Bean
    public Step importTransactionFileStep() {
        return this.stepBuilderFactory.get("importTransactionFileStep")
            .allowStartIfComplete(true) // 스탭이 잘 완료되었더라도 다시 실행 할 수 있도록 설정한다. (잡의 상태가 ExitStatus.COMPLETED 라면 이와 관계없이 재실행 불가능)
            /**
             * 입력 파일 가져오기를 두번만 시도하도록 구성, 2로 구성되어 있으므로 이 스탭을 사용하는 잡은 2번만 실행이 가능해진다.
             */
            .startLimit(2)
            .<Transaction, Transaction>chunk(100)
            .reader(transactionReader())
            .writer(transactionWriter(null))
            .allowStartIfComplete(true)
            .listener(transactionReader())
            .build();
    }

    @StepScope
    @Bean
    public JdbcCursorItemReader<AccountSummary> accountSummaryReader(DataSource dataSource) {
        return new JdbcCursorItemReaderBuilder<AccountSummary>()
            .name("accountSummaryReader")
            .dataSource(dataSource)
            .sql(GET_ACCOUNT_SUMMARY)
            .rowMapper(new BeanPropertyRowMapper<>())
            .build();
    }

    @Bean
    public TransactionDao transactionDao(DataSource dataSource) {
        return new TransactionDao(dataSource);
    }

    @Bean
    public TransactionApplierProcessor transactionApplierProcessor() {
        return new TransactionApplierProcessor(transactionDao(null));
    }

    @Bean
    public JdbcBatchItemWriter<AccountSummary> accountSummaryWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<AccountSummary>()
            .dataSource(dataSource)
            .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
            .sql(UPDATE_SUMMARY)
            .build();
    }

    @Bean
    public Step applyTransactionStep() {
        return this.stepBuilderFactory.get("applyTransactionsStep")
            .<AccountSummary, AccountSummary>chunk(100)
            .reader(accountSummaryReader(null))
            .processor(transactionApplierProcessor())
            .writer(accountSummaryWriter(null))
            .build();
    }

    @StepScope
    @Bean
    public FlatFileItemWriter<AccountSummary> accountSummaryFileWriter(
        @Value("#{jobParameters['summaryFile']}") Resource summaryFile
    ) {
        DelimitedLineAggregator<AccountSummary> lineAggregator = new DelimitedLineAggregator<>();
        BeanWrapperFieldExtractor<AccountSummary> fieldExtractor = new BeanWrapperFieldExtractor<>();
        fieldExtractor.setNames(new String[]{"accountNumber", "currentBalance"});
        fieldExtractor.afterPropertiesSet();
        lineAggregator.setFieldExtractor(fieldExtractor);

        return new FlatFileItemWriterBuilder<AccountSummary>()
            .name("accountSummaryFileWriter")
            .resource(summaryFile)
            .lineAggregator(lineAggregator)
            .build();
    }

    @Bean
    public Step generateAccountSummaryStep() {
        return this.stepBuilderFactory.get("generateAccountSummaryStep")
            .<AccountSummary, AccountSummary>chunk(100)
            .reader(accountSummaryReader(null))
            .writer(accountSummaryFileWriter(null))
            .build();
    }

    @Bean
    public Job transactionJob() {
        return this.jobBuilderFactory.get("transactionJob")
            .preventRestart() // 실패또는 어떤 이유든 중지되었다면 재시작 할 수 없다.
            .start(importTransactionFileStep())
            .next(applyTransactionStep())
            .next(generateAccountSummaryStep())
            .build();
    }

    public static void main(String[] args) {
        List<String> realArgs = new ArrayList<>(Arrays.asList(args));

        realArgs.add("transactionFile=classpath:input/transactionFile.csv");
        realArgs.add("summaryFile=classpath:input/summaryFile.csv");

        SpringApplication.run(TransactionProcessingJob.class, realArgs.toArray(new String[realArgs.size()]));
    }
}
