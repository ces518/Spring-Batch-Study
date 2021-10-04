# 6장 잡 실행하기

## 스프링 부트로 배치 잡 실행하기
- 스프링 부트는 **CommandLineRunner** 와 **ApplicationRunner** 라는 두 가지 매커니즘을 이용해 실행시 로직을 수행한다.
  - 이 둘은 ApplicationContext 가 Refresh 된 이후 수행할 하나의 메소드를 가지고 있다.
- 스프링 부트는 **JobLauncherApplicationRunner** 가 JobLauncher 를 이용해 잡을 실행한다.
  - 기본적으로 ApplicationContext 에 모든 잡을 실행한다.
  - **spring.batch.job.enabled=false** 로 지정하면 부트 애플리케이션이 실행되어도 아무런 잡도 실행하지 않는다.
  - **spring.batch.job.names** 프로퍼티를 이용해 애플리케이션 기동시 실행할 잡을 구성할 수 있다.
    - 쉼표 (,) 로 구분된 잡 목록을 가져와 순서대로 실행한다.

```java
@EnableBatchProcessing
@SpringBootApplication
public class NoRunJob {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job job() {
        return this.jobBuilderFactory.get("job")
            .start(step1())
            .build();
    }

    @Bean
    public Step step1() {
        return this.stepBuilderFactory.get("step1")
            .tasklet((contribution, chunkContext) -> {
                System.out.println("step1 run!");
                return RepeatStatus.FINISHED;
            })
            .build();
    }

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(NoRunJob.class);
        Properties properties = new Properties();
        properties.put("spring.batch.job.enabled", false);
        application.setDefaultProperties(properties);

        application.run(args);
    }
}
```

### REST 방식으로 잡 실행하기
- 특정 기능을 노출하고자 할때 가장 일반적인 방식은 REST API 형태이다.
- REST API 로 스프링 배치 잡을 실행시키고 싶다면, **JobLauncher** 를 이용해 잡을 실행시키도록 직접 개발을 해야한다. (스프링 배치에서 제공해주지 않음)
  - 스프링 배친느 유일한 구현체인 SimpleJobLauncher 를 제공해준다.
  - 기본적으로 동기식으로 동작하며, 필요시 비동기로 동작하는 TaskExecutor 를 이용할 수도 있다.

```java
@EnableBatchProcessing
@SpringBootApplication
public class RestApplication {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Bean("restJob")
    public Job job() {
        return this.jobBuilderFactory.get("restJob")
            .incrementer(new RunIdIncrementer())
            .start(step1())
            .build();
    }

    @Bean
    public Step step1() {
        return this.stepBuilderFactory.get("step1")
            .tasklet((contribution, chunkContext) -> {
                System.out.println("step1 run!");
                return RepeatStatus.FINISHED;
            })
            .build();
    }

    @RestController
    public static class JobLaunchingController {

        @Autowired
        private JobLauncher jobLauncher;

        @Autowired
        private ApplicationContext context;

        @Autowired
        private JobExplorer explorer;

        @PostMapping("/run")
        public ExitStatus runJob(@RequestBody JobLaunchRequest request) throws Exception {
            Job job = this.context.getBean(request.getName(), Job.class);
            JobParameters jobParameters = new JobParametersBuilder(request.getJobParameters(), explorer)
                .getNextJobParameters(job) // RunIdIncrementor 활성화
                .toJobParameters();
            return this.jobLauncher.run(job, jobParameters).getExitStatus();
        }
    }

    @Getter
    public static class JobLaunchRequest {

        private String name;
        private Properties jobParameters;

        public JobParameters getJobParameters() {
            return new JobParametersBuilder(this.jobParameters)
                .toJobParameters();
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(RestApplication.class, args);
    }
}
```
- 위 코드는 JobName 과 JobParameter 를 Request 로 받아, JobName 을 기반으로 ApplicationContext 에 존재하는 Job 을 찾은뒤, JobParameter 와 함께 잡을 실행시킨다.
- 기본적으로 JobLauncher 는 동기식을 실행하기 때문에 사용자에게 ExitStatus 를 반환할 수 있다.
- 하지만 대부분의 배치 잡의 경우 처릴야이 많기 때문에 비동기 식으로 실행하는 것이 더 적합하며, 비동기로 실행할 경우 **JobExecution 의 ID 만 반환** 하게 된다.

### 쿼츠를 사용해 스케줄링하기
- **쿼츠** 는 오픈소스 스케줄러 이며, 잡 실행에 유용한 스프링부트 지원과 같이 오래전부터 스프링과 연동을 지원하고 있다.
- 쿼츠는 스케줄러, 잡, 트리거 라는 세가지 주요 컴포넌트를 가지고 있다.
  - 스케줄러는 SchedulerFactory 를 통해 가져오며, **JobDetails 및 트리거의 저장소** 및 관련 트리거 동작시 **잡을 실행하는 역할** 을 수행한다.
  - 트리거는 **작업 실행 시점을 정의** 한다.
  - 잡이 실행되면 잡의 개별 실행을 정의하는 JobDetails 객체가 생성된다.

`Quartz 로 실행할 잡 정의`

```java
@Configuration
public class QuartzJobConfiguration {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job quartzJob() {
        return this.jobBuilderFactory.get("quartzJob")
            .incrementer(new RunIdIncrementer())
            .start(quartzStep())
            .build();
    }

    @Bean
    public Step quartzStep() {
        return this.stepBuilderFactory.get("step1")
            .tasklet((contribution, chunkContext) -> {
                System.out.println("step1 run!!");
                return RepeatStatus.FINISHED;
            })
            .build();
    }
}
```

`Quartz 의 잡 (JobDetails) 정의`
- 스프링 배치로 정의한 잡을 실행

```java
public class BatchScheduledJob extends QuartzJobBean {

    @Autowired
    private Job job;

    @Autowired
    private JobExplorer jobExplorer;

    @Autowired
    private JobLauncher jobLauncher;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        JobParameters jobParameters = new JobParametersBuilder(jobExplorer)
            .getNextJobParameters(job)
            .toJobParameters();

        try {
            this.jobLauncher.run(this.job, jobParameters);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

`Quartz의 잡을 주기적으로 실행하도록 정의`

```java
@Configuration
@EnableBatchProcessing
@SpringBootApplication
public class QuartzConfiguration {

    @Bean
    public JobDetail quartzJobDetail() {
        return JobBuilder.newJob(BatchScheduledJob.class)
            .storeDurably() // 잡을 실행할 Trigger 가 존재하지 않더라도, 쿼츠 잡에 남겨두도록 지정
            .build();
    }

    @Bean
    public Trigger jobTrigger() {
        SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
            .withIntervalInSeconds(5).withRepeatCount(4); // 5초 주기로 4번 반복

        return TriggerBuilder.newTrigger()
            .forJob(quartzJobDetail())
            .withSchedule(scheduleBuilder)
            .build();
    }

    public static void main(String[] args) {
        SpringApplication.run(QuartzConfiguration.class, args);
    }
}
```
- JobDetail 은 **실행할 쿼츠 잡 수행시 이용되는 메타데이터** 이다.
- Schedule 은 **얼마나 자주 실행할 것인가** 에 대한 정의
- Trigger 는 **Schedule 과 JobDetail 을 연관짓는다.**

## 잡 중지하기
- 잡은 다양한 이유로 중지 될 수 있으며 특정한 이유로 인해 프로그래밍 적으로 중지하거나 외부에서 잡을 중지시킬 수도 있다.
- 스프링 배치를 사용하면 이런 각 중지 시나리오에 맞게 수행되는 방식 및 수행 가능한 다양한 옵션들을 제공한다.

### 자연스러운 완료
- 지금까지 살펴본 잡들은 자연스럽게 완료되었다.
- 각 잡들은 스탭이 **COMPLETED** 상태를 반환할때 까지 스탭을 실행했고, **모든 스탭이 완료되면 잡도 COMPLETED** 종료 코드를 반환한다.
- 잡이 BatchStatus.COMPLETED 상태를 반환했다면, 동일한 잡 파라미터로 잡을 실행할 수 없다.
- 이런 방식은 잡의 실행방식에 영향을 주기 때문에 매우 중요한 부분이다.
- 만약 매일 실행되어야 하는 잡이 있다면, 타임스탬프를 파라미터로 추가하는 JobParametersIncrementor 구현체를 개발하는 것이 좋다.

### 프로그래밍적으로 중지하기
- 배치처리는 일련의 검사 및 부하분산이 필요하다.
- 많은 양의 데이터를 처리할 때는 처리과정에서 발생한 일을 확인할 수 있어야 한다.
- 100만명의 데이터를 처리하던 도중 한 스탭이 10,000 개를 가져온 뒤 갑자기 중지된다면 ?
  - 문제가 발생했으니 더 진행되기 전에 수정을 해야한다.
- 프로그래밍 적으로 잡을 중지하는 방법중 **중지 트랜지션 (Stop Transition)** 과 **StepExecution** 을 활용하는 방법을 살펴본다.

#### 예제 잡
- 프로그래밍적으로 중지하는 잡의 동작을 확인하기 위한 예제 잡 애플리케이션
1. 단순한 거래파일 (transaction.csv) 를 읽어온다.
   - 각 거래는 계좌번호, 타임 스탬프, 금액 으로 구성되며 파일 내용은 파일 내 총 레코드 개수를 보여주는 한줄 짜리 요약 레코드로 끝난다.
2. 거래 정보를 거래 테이블에 저장한 뒤 계좌번호와 현재 계좌 잔액으로 구성된 별도의 계좌 요약 테이블에 적용한다.
3. 각 계좌의 계좌번호와 잔액을 나열하는 요약 파일 (summary.csv) 을 생성한다.

- 이를 설계 관점에서 살펴보면, 각 사용자 계좌에 거래 내역을 적용하기 전 가져온 레코드의 수와 요약 레코드 값과 일치하는지 유효성 검증이 필요하다.
- 이 무결성 검사를 통해 대량의 데이터 처리시 복구/재처리에 소모되는 많은 시간을 줄일 수 있다.

`Transaction.csv`

```csv
51524,2018-01-13 18:53:29,751.06
719106,2018-01-13 18:53:29,591.7
209766,2018-01-13 18:53:29,895.64
424978,2018-01-13 18:53:29,695.89
709443,2018-01-13 18:53:29,-919.19
529933,2018-01-13 18:53:29,-596.91
221416,2018-01-13 18:53:29,-183.59
881616,2018-01-13 18:53:29,-786.84
494514,2018-01-13 18:53:29,580.2
302370,2018-01-13 18:53:29,476.54
455515,2018-01-13 18:53:29,-583.21
486655,2018-01-13 18:53:29,43.62
254179,2018-01-13 18:53:29,629.8
549538,2018-01-13 18:53:29,-149.24
938606,2018-01-13 18:53:29,509.49
12065,2018-01-13 18:53:29,-817.87
762420,2018-01-13 18:53:29,679.61
698261,2018-01-13 18:53:29,-468.37
90518,2018-01-13 18:53:29,367.03
483374,2018-01-13 18:53:29,-154.1
548709,2018-01-13 18:53:29,885.26
193379,2018-01-13 18:53:29,-779.66
517684,2018-01-13 18:53:29,750.59
600668,2018-01-13 18:53:29,94.71
233085,2018-01-13 18:53:29,392.08
501971,2018-01-13 18:53:29,25.02
35706,2018-01-13 18:53:29,383.71
769783,2018-01-13 18:53:29,-620.97
894781,2018-01-13 18:53:29,-902.36
830340,2018-01-13 18:53:29,991.55
789636,2018-01-13 18:53:29,789.55
891341,2018-01-13 18:53:29,478.55
18985,2018-01-13 18:53:29,125.66
281220,2018-01-13 18:53:29,-275.55
170502,2018-01-13 18:53:29,-971.69
222808,2018-01-13 18:53:29,385.79
897606,2018-01-13 18:53:29,-73.33
108554,2018-01-13 18:53:29,-603.09
751899,2018-01-13 18:53:29,258.62
226986,2018-01-13 18:53:29,-11.87
944403,2018-01-13 18:53:29,-872.34
520722,2018-01-13 18:53:29,306.13
742028,2018-01-13 18:53:29,630.76
445057,2018-01-13 18:53:29,-560.62
82405,2018-01-13 18:53:29,735.75
308055,2018-01-13 18:53:29,-1.66
661871,2018-01-13 18:53:29,710.28
986940,2018-01-13 18:53:29,128.94
710826,2018-01-13 18:53:29,-461.79
89988,2018-01-13 18:53:29,770.65
464456,2018-01-13 18:53:29,469.21
940429,2018-01-13 18:53:29,871.97
177560,2018-01-13 18:53:29,522.19
842219,2018-01-13 18:53:29,211.18
771752,2018-01-13 18:53:29,503.9
476542,2018-01-13 18:53:29,610.31
575163,2018-01-13 18:53:29,-323.61
204689,2018-01-13 18:53:29,-678.64
4310,2018-01-13 18:53:29,-294.67
649816,2018-01-13 18:53:29,-745.21
461791,2018-01-13 18:53:29,981.78
645394,2018-01-13 18:53:29,-695.02
855595,2018-01-13 18:53:29,791.04
320221,2018-01-13 18:53:29,786.04
577591,2018-01-13 18:53:29,-521.37
689004,2018-01-13 18:53:29,-742.49
943916,2018-01-13 18:53:29,905.94
63569,2018-01-13 18:53:29,-168.98
966623,2018-01-13 18:53:29,370.28
370601,2018-01-13 18:53:29,-209.5
402098,2018-01-13 18:53:29,72.6
678250,2018-01-13 18:53:29,967.51
163323,2018-01-13 18:53:29,-864.37
396520,2018-01-13 18:53:29,459.34
904181,2018-01-13 18:53:29,-851.86
357204,2018-01-13 18:53:29,-326.95
347089,2018-01-13 18:53:29,240.58
688085,2018-01-13 18:53:29,594.66
948223,2018-01-13 18:53:29,-976.23
308637,2018-01-13 18:53:29,824.17
8401,2018-01-13 18:53:29,397.83
902533,2018-01-13 18:53:29,271.59
878638,2018-01-13 18:53:29,-365.82
64060,2018-01-13 18:53:29,-744.17
827826,2018-01-13 18:53:29,-920.04
811060,2018-01-13 18:53:29,280.5
916624,2018-01-13 18:53:29,-130.28
304317,2018-01-13 18:53:29,627.06
307462,2018-01-13 18:53:29,669.96
203512,2018-01-13 18:53:29,-101.87
471800,2018-01-13 18:53:29,-561.06
790040,2018-01-13 18:53:29,673.29
22520,2018-01-13 18:53:29,-61.18
80277,2018-01-13 18:53:29,-422.83
367483,2018-01-13 18:53:29,-368.06
18675,2018-01-13 18:53:29,493.47
339599,2018-01-13 18:53:29,884.16
991085,2018-01-13 18:53:29,-15.05
297579,2018-01-13 18:53:29,-344.38
99
```

`사용할 스키마`

```sql
create table Transactions
(
    id                 int(32) auto_increment
        primary key,
    timestamp          timestamp default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    amount             decimal(8, 2)                       null,
    account_summary_id int(32)                             null
);
create table Account_Summary
(
  id              int(32)        not null
    primary key,
  account_number  varchar(10)    null,
  current_balance decimal(10, 2) null
);
```

`도메인 관련 클래스`

```java
@Data
public class Transaction {

    private String accountNumber;
    private Date timestamp;
    private double amount;
}

@Data
public class AccountSummary {

  private int id;
  private String accountNumber;
  private Double currentBalance;
}

public class TransactionDao extends JdbcTemplate {

  private static final String GET_BY_ACCOUNT_NUMBER = "select t.id, t.timestamp, t.amount" +
          "from Transaction t join Account_Summary a on" +
          "a.id = t.account_summary_id" +
          "where a.account_number = ?";

  public TransactionDao(DataSource dataSource) {
    super(dataSource);
  }

  public List<Transaction> getTransactionsByAccountNumber(String accountNumber) {
    return queryForList(GET_BY_ACCOUNT_NUMBER, Transaction.class, accountNumber);
  }
}
```

`커스텀 ItemReader`

```java
/**
 * 스프링 배치는 ItemReader, ItemProcessor, ItemWriter, ItemStream 구현체인지 확인하고 적절한 시점에 콜백이 되도록 수행한다.
 * 스프링 배치에 명시적으로 등록되어 있지 않다면, 위 인터페이스들을 구현하고 있는지 확인하지 않는다.
 * -> 이런 경우 선택가능한 옵션은 두가지
 * -> 1. 잡에서 리더를 ItemStream 으로 명시적으로 등록하는 방법
 * -> 2. ItemStream 을 구현하고 적절한 라이프사이클에 따라 메소드를 호출하는 방법
 */
public class TransactionReader implements ItemStreamReader<Transaction> {

    private ItemStreamReader<FieldSet> fieldSetReader;
    private int recordCount = 0;
    private int expectedRecordCount = 0;

    private StepExecution stepExecution;

    public TransactionReader(ItemStreamReader<FieldSet> fieldSetReader) {
        this.fieldSetReader = fieldSetReader;
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        fieldSetReader.open(executionContext);
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        fieldSetReader.update(executionContext);
    }

    @Override
    public void close() throws ItemStreamException {
        fieldSetReader.close();
    }

    @Override
    public Transaction read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        /**
         * 임의로 예외를 발생시킴..
         * this.stepExecution.setTerminateOnly() => 스텝이 완료된 후 잡이 중지 (ExitStatus.STOPPED)
         * 예외를 방생시키는 방식은 스탭이 완료되지 않았다는 점 (ExitStatus.FAILED) 이다.
         * 스탭이 FAILED 로 식별되면, 어떤 청크 처리중 중단되었는지 기록되어있기 때문에, 해당 부분부터 재시작이 가능하다.
         */

        if (this.recordCount == 25) {
            throw new ParseException("This isn't what i hoped to happen");
        }
        return process(fieldSetReader.read());
    }

    private Transaction process(FieldSet fieldSet) {
        Transaction result = null;

        if (fieldSet != null) {
            if (fieldSet.getFieldCount() > 1) { // 레코드에 값이 2개이상 이라면 데이터 레코드, 아닐경우 푸터 레코드이다.
                result = new Transaction();
                result.setAccountNumber(fieldSet.readString(0));
                result.setTimestamp(fieldSet.readDate(1, "yyyy-MM-DD HH:mm:ss"));
                result.setAmount(fieldSet.readDouble(2));
                recordCount++;
            } else {
                expectedRecordCount = fieldSet.readInt(0);
            }
        }
        return result;
    }

    /**
     * 스프링배치에서 CSV 리더를 제공하지만, 커스텀 리더를 만든 이유 ?
     * -> 스탭의 ExitStatus 가 리더의 상태에 묶여 있기 때문이다..
     * -> Footer 레코드에 기록된 수와 실제로 읽은 레코드 수가 다르다면, 잡의 실행을 계속해선 안된다.
     * -> Footer 레코드에 도달했을때, 예상 레코드수와 실제 레코드수가 다르다면 STOPPED 를 반환해서 처리를 중단한다.
     */
    @AfterStep
    public ExitStatus afterStep(StepExecution execution) {
        if (recordCount == expectedRecordCount) {
            return execution.getExitStatus();
        } else {
            return ExitStatus.STOPPED;
        }
    }
}
```
- 스프링 배치가 CSV 리더를 제공하지만 커스텀 리더를 만든 이유 ?
  - 스탭의 ExitStatus 가 CSV 리더의 상태에 묶여있기 때문이다.
  - 실제 레코드 수와 요약 레코드 수가 다르다면 잡의 실행을 중지해야한다.
    - STOPPED 상태를 반환하도록 @AfterStep 을 사용해 구현
- 스프링 배치는 ItemReader, ItemProcessor, ItemWriter 가 ItemStream 의 구현체인지 확인하고, 적절한 시점에 콜백되도록 등록한다.
  - 위 예제는 ItemStream 을 구현하고 있지만, 스프링 배치에 등록되어 있지 않아 ItemStream 구현여부를 확인하지 않는다..
- 이런 경우 두가지 방법을 사용할 수 있다.
  1. 잡에서 해당 리더를 ItemStream 으로 명시적으로 등록
  2. 구현체에서 적절한 라이프사이클에 따라 메소드를 호출하도록 하는 방법

`커스텀 ItemProcessor`

```java
public class TransactionApplierProcessor implements ItemProcessor<AccountSummary, AccountSummary> {

    private TransactionDao transactionDao;

    public TransactionApplierProcessor(TransactionDao transactionDao) {
        this.transactionDao = transactionDao;
    }

    @Override
    public AccountSummary process(AccountSummary accountSummary) throws Exception {
        List<Transaction> transactions = transactionDao.getTransactionsByAccountNumber(
            accountSummary.getAccountNumber());

        for (Transaction transaction : transactions) {
            accountSummary.setCurrentBalance(accountSummary.getCurrentBalance() + transaction.getAmount());
        }
        return accountSummary;
    }
}
```
- TransactionDao 를 이용해 거래 정보를 조회해 거래 정보에 따라 계좌의 현재잔액을 증가 또는 감소시킨다.

#### 각 스탭 살펴보기
- 예제 잡은 세가지 스탭으로 구성된다.
  1. ImportTransactionFileStep (csv 파일을 읽는 STEP)
  2. ApplyTransactionStep (읽어들인 파일을 기반으로 데이터베이스에 적용하는 STEP)
  3. GenerateAccountSummaryStep (AccountSummary 를 생성하는 STEP)

`ImportTransactionFileStep`

```java
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
```
- 스탭의 구성 -> 청크 기반으로 구성한다. 
  1. 이전에 구현했던 TransactionReader (FlatFileItemReader 를 래핑하는 커스텀 리더) 를 사용해 csv 파일을 읽는다.
  2. JdbcBatchItemWriter 를 이용해 값을 데이터 베이스에 저장하는 역할을 수행한다.

`ApplyTransactionStep`

```java
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
```
- 스탭 구성 -> 청크기반으로 구성한다.
  1. JdbcCursorItemReader 를 이용해 AccountSummary 레코드를 읽는다.
  2. TransactionApplierProcessor (커스텀 프로세서) 를 이용해 AccountSummary 의 계좌 상태를 변경한다.
  3. JdbcBatchItemWriter 를 이용해서 변경된 계좌 요약 레코드를 DB 에 기록한다.

`GenerateAccountSummaryStep`

```java
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
```
- 스탭 구성 -> 청크기반으로 구성한다.
  1. ApplyTransactionStep 이 사용한 ItemReader 를 재사용한다.
     - AccountSummaryReader 가 StepScope 이기 때문에 각 스탭에서 리더 사용시 독립적인 인스턴스가 사용된다.
  2. FlatFileItemWriter 를 사용해 각 레코드의 계좌번호와 현재 잔액으로 csv 파일을 생성한다.

`전체 잡 구성`

```java
/**
 * 잡의 트랜지션 구성
 */
@Bean
public Job transactionJob() {
    return this.jobBuilderFactory.get("transactionJob")
      .start(importTransactionFileStep())
      .on("STOPPED").stopAndRestart(importTransactionFileStep())
      .from(importTransactionFileStep()).on("*").to(applyTransactionStep())
      .from(applyTransactionStep()).next(generateAccountSummaryStep())
      .end()
      .build();
}
```
- 전체 잡 구성시 importTransactionFileStep 이 처리중 반환할 수 있는 ExitStatus.STOPPED 관련 처리를 해주어야한다.

> 지금 까지 살펴본 예제잡은 **중지 트랜지션** 을 활용한 방법 다음은 StepExecution 을 활용하는 방법을 살펴본다.

#### StepExecution 을 활용해 중지하기
- 이전에는 StepListener 의 ExitStatus 와 잡의 트랜지션 구성을 통해 수동으로 잡을 중지했다.
- 이 방법은 효과적이긴 하나 잡의 트랜지션을 별도로 구성해야하고 스탭의 ExitStatus 를 재정의 해야한다.

`StepExecution 을 활용한 중지`

```java
public class TransactionReader implements ItemStreamReader<Transaction> {

    private ItemStreamReader<FieldSet> fieldSetReader;
    private int recordCount = 0;
    private int expectedRecordCount = 0;

    private StepExecution stepExecution;

    public TransactionReader(ItemStreamReader<FieldSet> fieldSetReader) {
        this.fieldSetReader = fieldSetReader;
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        fieldSetReader.open(executionContext);
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        fieldSetReader.update(executionContext);
    }

    @Override
    public void close() throws ItemStreamException {
        fieldSetReader.close();
    }

    @Override
    public Transaction read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        /**
         * 임의로 예외를 발생시킴..
         * this.stepExecution.setTerminateOnly() => 스텝이 완료된 후 잡이 중지 (ExitStatus.STOPPED)
         * 예외를 방생시키는 방식은 스탭이 완료되지 않았다는 점 (ExitStatus.FAILED) 이다.
         * 스탭이 FAILED 로 식별되면, 어떤 청크 처리중 중단되었는지 기록되어있기 때문에, 해당 부분부터 재시작이 가능하다.
         */

        if (this.recordCount == 25) {
            throw new ParseException("This isn't what i hoped to happen");
        }
        return process(fieldSetReader.read());
    }

    private Transaction process(FieldSet fieldSet) {
        Transaction result = null;

        if (fieldSet != null) {
            if (fieldSet.getFieldCount() > 1) { // 레코드에 값이 2개이상 이라면 데이터 레코드, 아닐경우 푸터 레코드이다.
                result = new Transaction();
                result.setAccountNumber(fieldSet.readString(0));
                result.setTimestamp(fieldSet.readDate(1, "yyyy-MM-DD HH:mm:ss"));
                result.setAmount(fieldSet.readDouble(2));
                recordCount++;
            } else {
                expectedRecordCount = fieldSet.readInt(0);

                /**
                 * AfterStep 에 있던 Record 검사로직이 process 로 이동
                 */
                // 푸터 레코드와 실제 레코드 값이 다를경우 스탭이 완료된 후 스프링배치가 종료되도록 지시
                // JOB 이 STOPPED 상태를 반환하는 대신 스프링 배치가 JobInterruptedException 을 던진다.
                if (expectedRecordCount != this.recordCount) {
                    this.stepExecution.setTerminateOnly();
                }
            }
        }
        return result;
    }

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        this.stepExecution = stepExecution;
    }
}
```
- 이전에 구현했던 리더와의 차이점은 **@BeforeStep 을 활용해 StepExecution 을 주입받았다** 는 점이다.
- 이를 이용해 StepExecution 에 접근이 가능하므로 StepExecution.setTerminateOnly() 를 호출할 수 있다.
  - 이는 스탭 완료 후 스프링 배치가 종료되도록 지시하는 메소드
- JOB 이 STOPPED 상태를 반환하는 대신 스프링배치가 JobInterruptedException 예외를 던진다.

```java
    @Bean
    public Job transactionJob() {
        return this.jobBuilderFactory.get("transactionJob")
            .start(importTransactionFileStep())
            .next(applyTransactionStep())
            .next(generateAccountSummaryStep())
            .build();
    }
```
- 중지 트랜지션 방식보다 StepExecution 을 사용한 방식이 잡 구성이 좀 더 깔끔해진다.

### 오류 처리
- 어떤 잡도 완벽하지 않으며 오류는 발생할 수 밖에 없다.
- 스프링 배치를 사용해 오류를 처리하는 방법이 중요하다.
- 예외 처리시 선태각능한 여러 옵션과 구현 방법을 살펴본다.

#### 잡 실패
- 스프링 배치의 기본 동작이 가장 안전한 방식이다.
- 잡이 중지된다면, 현재 청크를 롤백하기 때문이다. (청크 기반 처리의 중요한 개념)
- 성공적으로 완료한 작업까지 커밋할 수 있으며, 재시작 시 중단됬던 부분을 찾을 수 있다.
- 스프링 배치는 기본적으로 예외 발생시 스탭 및 잡이 **실패** 한 것으로 간주한다.

```java
@Override
public Transaction read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
    /**
     * 임의로 예외를 발생시킴..
     * this.stepExecution.setTerminateOnly() => 스텝이 완료된 후 잡이 중지 (ExitStatus.STOPPED)
     * 예외를 방생시키는 방식은 스탭이 완료되지 않았다는 점 (ExitStatus.FAILED) 이다.
     * 스탭이 FAILED 로 식별되면, 어떤 청크 처리중 중단되었는지 기록되어있기 때문에, 해당 부분부터 재시작이 가능하다.
     */

    if (this.recordCount == 25) {
        throw new ParseException("This isn't what i hoped to happen");
    }
    return process(fieldSetReader.read());
}
```
- org.springframework.batch.item.ParseException 은 현재 레코드에 문제가 있음을 스프링 배치에게 알린다.
- StepExecution 을 사용해 중지하는 방식과 예외를 발생시켜 잡을 중지하는 방식에는 큰 차이가 있는데, 이는 **잡의 상태** 에서 차이가 발생한다.
- 예외를 발생한 경우는 스탭이 **완료** 처리가 되지 않는다.
- 예외가 던져지면 스탭을 통과하고, 스탭과 잡에 **ExitStatus.FAILED** 상태로 기록된다.
- FAILED 로 기록된 경우 스프링 뱇니느 해당 스탭을 처음 부터 다시 싲가하지 않는다.
- 잡을 재시작 하면 중단되었던 부분부터 재시작 하게 된다.


### 재시작 제어하기

#### 잡의 재시작 방지
- 지금까지 모든 잡은 실패 또는 중지된다면 다시 실행이 가능했다.
  - 스프링 배치의 기본 동작
- 하지만 실패시 다시 실행되어서는 안되는 잡이 존재할 수도 있다.
- 스프링 배치에서는 잡 구성시 preventRestart() 메소드를 통해 이러한 구성을 손쉽게 할 수 있도록 제공한다.

```java
    @Bean
    public Job transactionJob() {
        return this.jobBuilderFactory.get("transactionJob")
            .preventRestart() // 실패또는 어떤 이유든 중지되었다면 재시작 할 수 없다.
            .start(importTransactionFileStep())
            .next(applyTransactionStep())
            .next(generateAccountSummaryStep())
            .build();
    }
```

#### 재시작 횟수를 제한
- 특정 잡이 무한정 재시도 하지 못하도록 횟수를 제한하고 싶을 수도 있다.
- 스프링 배치는 이 기능을 잡이 아닌 **스탭 레벨에서 제공** 한다.

```java
    @Bean
    public Step importTransactionFileStep() {
        return this.stepBuilderFactory.get("importTransactionFileStep")
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
```
- start-limit 속성을 지정해서 재시작 횟수를 제안할 수 있으며, 해당 횟수를 초과한 재시도시 org.springframework.batch.core.StartLimitExceededException 이 발생한다.

#### 완료된 스탭 재실행
- 스프링 배치는 **동일한 파라미터를 가진 잡은 한번만 성공적으로 실행이 가능하다** 는 특징을 가지고 있다.
- 이 문제를 해결할 방법은 없지만, 스탭에는 이 방식이 절대적인 것은 아니다.
- 기본 구성을 재정의 함으로써 완료된 스탭을 두번 이상 실행할 수 있다.
- 스프링 배치는 스탭구성시 allowStartIfComplete(true) 메소드를 통해 해당 기능을 제공한다.

```java
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
```
- 해당 스탭이 완료되었더라도 다시 실행하도록 설정한다.
- 단 주의할 점은 잡의 상태가 ExitStatus.COMPLETED 라면 이 설정과 관계없이 재실행은 불가능하다.