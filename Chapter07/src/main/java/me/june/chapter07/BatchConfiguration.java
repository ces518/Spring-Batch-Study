package me.june.chapter07;

import java.util.HashMap;
import java.util.Map;
import me.june.chapter07.domain.Customer;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.mapping.PatternMatchingCompositeLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.LineTokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@EnableBatchProcessing
@Configuration
public class BatchConfiguration {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    /**
     * FlatFileItemReader 는 파일의 레코드를 객체로 변환할 때 LineMapper 를 사용한다.
     * DefaultLineMapper 를 기본적으로 많이 사용
     * 아래 설정중 columns, names = LineTokenizer 설정
     * targetType = FieldSetMapper 설정 BeanWrapperFieldSetMapper 를 사용
     * BeanWrapperFieldSetMapper 는 LinkeTokenizer 에 구성된 컬럼명을 이용해 Customer.setFirstName 과 같은 메소드를 호출한다.
     *
     * @FixedLengthTokenizer 는 필드 앞뒤의 0 또는 공백 문자를 제거 하지 않는다.
     */
    @StepScope
    @Bean
    public FlatFileItemReader customerItemReader(
        @Value("#{jobParameters['customerFile']}") Resource inputFile
    ) {
        return new FlatFileItemReaderBuilder<Customer>()
            /**
             * ItemStream 인터페이스는 애플리케이션 내 각 스텝의 ExecutionContext 에추가되는 특정 키의 접두문자로 사용될 이름이 필요하다.
             * 동일한 스탭에서 FlatFileItemReader 두 개를 함께 사용할 때 각 리더의 상태를 저장하는 작업이 서로 영향을 주지 않게 하는데 필요하다. (Reader 의 SaveState = false 일 경우 지정할 필요가 없다)
             */
            .name("customerItemReader")
            .resource(inputFile)
            // .delimited() // DelimitedLineTokenizer 를 사용
//            .fixedLength() // 고정너비 파일 지정 -> FixedLengthTokenizer 를 생성하는 빌더가 반환된다. 이는 각 줄을 파싱해 FieldSet 으로 만드는 LineTokenizer 의 구현체
//            .columns(new Range[]{ // Range 객체 배열 지정
//                new Range(1, 11), new Range(12, 12), new Range(13, 22),
//                new Range(23, 26), new Range(27, 46), new Range(47, 62),
//                new Range(63, 64), new Range(65, 69)
//            })
//            .names("firstName", "middleInitial", "lastName", "addressNumber", "street", "city", "state", "zipCode") // 레코드 내 각 컬럼명을 지정
            // FieldSetFactory 와 strict 플래그 설정 가능
            // ㄴ DefaultFieldSetFactory 를 제공함
            // ㄴ strict 는 지정한 항목보다 더 많은 정보가 있을 경우 예외를 던진다. 기본값 = true
//            .targetType(Customer.class) // BeanWrapperFieldSetMapping 를 생성
//            .fieldSetMapper(new CustomerFieldSetMapper()) // CustomFieldSetMapper
//            .lineTokenizer(new CustomerFileLineTokenizer()) // CustomLineTokenizer (별도의 파일 포맷, 엑셀 등.. 혹은 특수한 타입변환 요구 조건 처리시 사용)
//            .targetType(Customer.class)
            .lineMapper(lineTokenizer()) // CompositeLineMapper
            .build();
    }



    @Bean
    public DelimitedLineTokenizer transactionLineTokenizer() {
        DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
        lineTokenizer.setNames("prefix", "accountNumber", "transactionDate", "amount");
        return lineTokenizer;
    }

    @Bean
    public DelimitedLineTokenizer customerLineTokenizer() {
        DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
        lineTokenizer.setNames(
            "firstName",
            "middleInitial",
            "lastName",
            "address",
            "city",
            "state",
            "zipCode"
        );
        lineTokenizer.setIncludedFields(1, 2, 3, 4, 5, 6, 7);
        return lineTokenizer;
    }

    // 한 파일의 한 레코드에 여러가지 포맷이 존재할 경우 어떻게 할것인가 ?
    // LineTokenizer 만으로는 한계가 있다.
    // LineTokenizer 는 레코드를 파싱하는것 그 이상을 넘어선 안된다. (레코드 유형을 파악하는데 사용해서는 안된다.)
    // PatternMatchingCompositeLineMapper (여러개의 LikeTokenizer 와 FieldSetMapper 사용 가능)
    @Bean
    public PatternMatchingCompositeLineMapper lineTokenizer() {
        Map<String, LineTokenizer> lineTokenizers = new HashMap<>(2);
        // * 는 문작 ㅏ없거나 여러개 있음을 의미한다. (즉, 레코드가 CUST 로 시작해당 customerLineTokenizer 를 사용한다.)
        lineTokenizers.put("CUST*", customerLineTokenizer()); // 고객정보
        lineTokenizers.put("TRANS*", transactionLineTokenizer()); // 거래정보

        Map<String, FieldSetMapper> fieldSetMappers = new HashMap<>(2);
        BeanWrapperFieldSetMapper<Object> customerFieldSetMapper = new BeanWrapperFieldSetMapper<>();
        customerFieldSetMapper.setTargetType(Customer.class);

        fieldSetMappers.put("CUST*", customerFieldSetMapper);
        fieldSetMappers.put("TRANS*", new TransactionFieldSetMapper());

        PatternMatchingCompositeLineMapper lineMappers = new PatternMatchingCompositeLineMapper();
        lineMappers.setTokenizers(lineTokenizers);
        lineMappers.setFieldSetMappers(fieldSetMappers);

        return lineMappers;
    }

    @Bean
    public CustomerFileReader customerFileReader() {
        return new CustomerFileReader(customerItemReader(null));
    }

    /**
     * SimpleWriter
     */
    @Bean
    public ItemWriter itemWriter() {
        return items -> items.forEach(System.out::println);
    }

    @Bean
    public Step copyFileStep() {
        return this.stepBuilderFactory.get("copyFileStep")
            .<Customer, Customer>chunk(10)
//            .reader(customerItemReader(null))
            .reader(customerFileReader()) // CustomItemReader 를 사용
            .writer(itemWriter())
            .build();
    }

    @Bean
    public Job job() {
        return this.jobBuilderFactory.get("job")
            .start(copyFileStep())
            .build();
    }
}
