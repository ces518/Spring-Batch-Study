# 4장 잡과 스텝 이해하기

## 잡 소개

- 잡이란, 처음부터 끝까지 **독립적으로 실행할 수 있는 고유하고 순서가 지정된 여러 스텝의 목록** 이다.

`잡의 특징`

- 유일하다
    - 스프링의 빈 구성방식과 동일하게 자바 또는 XML 구성 방식을 사용하며 이를 재사용할 수 있다.
- 순서를 가진 여러 스텝의 목록이다
    - 모든 스텝을 논리적인 순서대로 실행할 수 있도록 잡을 구성한다.
- 처음부터 끝까지 실행 가능하다
    - 잡은 외부 의존성 없이 실행할 수 있는 일련의 스텝이다.
- 독립적이다
    - 각 배치 잡은 외부 의존성의 영향을 받지 않고 실행 가능해야 한다.
    - 이는 외부 의존성이 존재하지 않는다는 것을 의미하는게 아니며, 외부 의존성을 관리할 수 있어야 한다.
    - 만약 파일이 없다면, 오류를 자연스럽게 처리할 수 있어야 하며 파일이 전달될 때 까지 기다리지 않는다.

> 잡의 실행은 스케쥴러와 같은 요소들이 책임지고, 잡은 자신이 처리하기로 정의된 모든 요소를 제어할 수 있다.

### 잡의 생명주기

- 잡의 실행은 생명주기대로 진행된다.
- 이는 **잡 러너 (Job Runner)** 로 부터 시작되며, 잡 이름과 여러 파라미터들을 이용해 잡을 실행시키는 역할을 한다.

`잡 러너의 종류`

- CommandLineJobRunner
    - 스크립트를 이용하거나 명령행에서 직접 잡을 실행할때 사용한다.
    - 스프링을 부트스트랩하고, 전달받은 파라미터를 이용해 잡을 실행하낟.
- JobRegistryBackgroundJobRunner
    - 스프링을 부트스트랩해서 기동한 자바 프로세스 내에서 **쿼츠 (Quartz)** 또는 JMX 후크와 같은 스케쥴러를 이용해 잡을 실행할 때 사용한다.
    - 스프링이 부트스트랩 될 때 **JobRegistry** 를 생성하는데, 이는 실행 가능한 잡을 가지고 있다.
    - JobRegistryBackgroundJobRunner 는 JobRegistry 를 생성할 때 사용한다.
- 이와 별개로 스프링 부트는 **JobLauncherCommandLineRunner (deprecated), JobLauncherApplicationRunner** 를 이용해
  잡을 시작하는 방법을 제공한다.
    - 이는 별도 설정이 없다면, 기본적으로 ApplicationContext 에 정의된 **Job 타입의 모든 빈을 실행** 한다.

> 스프링 배치 실행시에 JobRunner 를 사용하긴 하지만 프레임워크레벨에서 제공하는 표준모듈이 아니다. <br/>
> 각 시나리오마다 서로 다른 구현체가 필요함에 따라 JobRunner 라는 인터페이스를 제공하지 않는다. <br/>
> 실제 실행시 엔트리포인트는 **JobLauncher**

`JobLauncher`

```java
public interface JobLauncher {

    JobExecution run(Job var1, JobParameters var2)
        throws JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException, JobParametersInvalidException;
}

public class SimpleJobLauncher implements JobLauncher, InitializingBean {
  protected static final Log logger = LogFactory.getLog(SimpleJobLauncher.class);
  private JobRepository jobRepository;
  private TaskExecutor taskExecutor;

  public SimpleJobLauncher() {
  }

  public JobExecution run(final Job job, final JobParameters jobParameters) throws JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException, JobParametersInvalidException {
    Assert.notNull(job, "The Job must not be null.");
    Assert.notNull(jobParameters, "The JobParameters must not be null.");
    JobExecution lastExecution = this.jobRepository.getLastJobExecution(job.getName(), jobParameters);
    if (lastExecution != null) {
      if (!job.isRestartable()) {
        throw new JobRestartException("JobInstance already exists and is not restartable");
      }

      Iterator var5 = lastExecution.getStepExecutions().iterator();

      while(var5.hasNext()) {
        StepExecution execution = (StepExecution)var5.next();
        BatchStatus status = execution.getStatus();
        if (status.isRunning() || status == BatchStatus.STOPPING) {
          throw new JobExecutionAlreadyRunningException("A job execution for this job is already running: " + lastExecution);
        }

        if (status == BatchStatus.UNKNOWN) {
          throw new JobRestartException("Cannot restart step [" + execution.getStepName() + "] from UNKNOWN status. The last execution ended with a failure that could not be rolled back, so it may be dangerous to proceed. Manual intervention is probably necessary.");
        }
      }
    }

    job.getJobParametersValidator().validate(jobParameters);
    final JobExecution jobExecution = this.jobRepository.createJobExecution(job.getName(), jobParameters);

    try {
      this.taskExecutor.execute(new Runnable() {
        public void run() {
          try {
            if (SimpleJobLauncher.logger.isInfoEnabled()) {
              SimpleJobLauncher.logger.info("Job: [" + job + "] launched with the following parameters: [" + jobParameters + "]");
            }

            job.execute(jobExecution);
            if (SimpleJobLauncher.logger.isInfoEnabled()) {
              Duration jobExecutionDuration = BatchMetrics.calculateDuration(jobExecution.getStartTime(), jobExecution.getEndTime());
              SimpleJobLauncher.logger.info("Job: [" + job + "] completed with the following parameters: [" + jobParameters + "] and the following status: [" + jobExecution.getStatus() + "]" + (jobExecutionDuration == null ? "" : " in " + BatchMetrics.formatDuration(jobExecutionDuration)));
            }
          } catch (Throwable var2) {
            if (SimpleJobLauncher.logger.isInfoEnabled()) {
              SimpleJobLauncher.logger.info("Job: [" + job + "] failed unexpectedly and fatally with the following parameters: [" + jobParameters + "]", var2);
            }

            this.rethrow(var2);
          }

        }

        private void rethrow(Throwable t) {
          if (t instanceof RuntimeException) {
            throw (RuntimeException)t;
          } else if (t instanceof Error) {
            throw (Error)t;
          } else {
            throw new IllegalStateException(t);
          }
        }
      });
    } catch (TaskRejectedException var8) {
      jobExecution.upgradeStatus(BatchStatus.FAILED);
      if (jobExecution.getExitStatus().equals(ExitStatus.UNKNOWN)) {
        jobExecution.setExitStatus(ExitStatus.FAILED.addExitDescription(var8));
      }

      this.jobRepository.update(jobExecution);
    }

    return jobExecution;
  }

  public void setJobRepository(JobRepository jobRepository) {
    this.jobRepository = jobRepository;
  }

  public void setTaskExecutor(TaskExecutor taskExecutor) {
    this.taskExecutor = taskExecutor;
  }

  public void afterPropertiesSet() throws Exception {
    Assert.state(this.jobRepository != null, "A JobRepository has not been set.");
    if (this.taskExecutor == null) {
      logger.info("No TaskExecutor has been set, defaulting to synchronous executor.");
      this.taskExecutor = new SyncTaskExecutor();
    }

  }
}
```
- 스프링배치가 제공하는 구현체는 SimpleJobLauncher 하나 뿐이다.
  - 잡과, 잡파라미터를 인자로 받아 수행후 JobExecution 을 반환한다.
  - 잡 실행시 **TaskExecutor** 를 이용하며, 별도 설정이 없다면 기본적으로 **SyncTaskExecutor** 를 사용해 동기적으로 실행한다.
- Batch 관련 설정의 핵심 인터페이스는 **BatchConfigurer**

```java
public interface BatchConfigurer {
    JobRepository getJobRepository() throws Exception;

    PlatformTransactionManager getTransactionManager() throws Exception;

    JobLauncher getJobLauncher() throws Exception;

    JobExplorer getJobExplorer() throws Exception;
}
public class BasicBatchConfigurer implements BatchConfigurer, InitializingBean {
  private final BatchProperties properties;
  private final DataSource dataSource;
  private PlatformTransactionManager transactionManager;
  private final TransactionManagerCustomizers transactionManagerCustomizers;
  private JobRepository jobRepository;
  private JobLauncher jobLauncher;
  private JobExplorer jobExplorer;

  protected BasicBatchConfigurer(BatchProperties properties, DataSource dataSource, TransactionManagerCustomizers transactionManagerCustomizers) {
    this.properties = properties;
    this.dataSource = dataSource;
    this.transactionManagerCustomizers = transactionManagerCustomizers;
  }

  public JobRepository getJobRepository() {
    return this.jobRepository;
  }

  public PlatformTransactionManager getTransactionManager() {
    return this.transactionManager;
  }

  public JobLauncher getJobLauncher() {
    return this.jobLauncher;
  }

  public JobExplorer getJobExplorer() throws Exception {
    return this.jobExplorer;
  }

  public void afterPropertiesSet() {
    this.initialize();
  }

  public void initialize() {
    try {
      this.transactionManager = this.buildTransactionManager();
      this.jobRepository = this.createJobRepository();
      this.jobLauncher = this.createJobLauncher();
      this.jobExplorer = this.createJobExplorer();
    } catch (Exception var2) {
      throw new IllegalStateException("Unable to initialize Spring Batch", var2);
    }
  }

  protected JobExplorer createJobExplorer() throws Exception {
    PropertyMapper map = PropertyMapper.get();
    JobExplorerFactoryBean factory = new JobExplorerFactoryBean();
    factory.setDataSource(this.dataSource);
    Jdbc var10001 = this.properties.getJdbc();
    var10001.getClass();
    map.from(var10001::getTablePrefix).whenHasText().to(factory::setTablePrefix);
    factory.afterPropertiesSet();
    return factory.getObject();
  }

  protected JobLauncher createJobLauncher() throws Exception {
    SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
    jobLauncher.setJobRepository(this.getJobRepository());
    jobLauncher.afterPropertiesSet();
    return jobLauncher;
  }

  protected JobRepository createJobRepository() throws Exception {
    JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
    PropertyMapper map = PropertyMapper.get();
    map.from(this.dataSource).to(factory::setDataSource);
    map.from(this::determineIsolationLevel).whenNonNull().to(factory::setIsolationLevelForCreate);
    Jdbc var10001 = this.properties.getJdbc();
    var10001.getClass();
    map.from(var10001::getTablePrefix).whenHasText().to(factory::setTablePrefix);
    map.from(this::getTransactionManager).to(factory::setTransactionManager);
    factory.afterPropertiesSet();
    return factory.getObject();
  }

  protected String determineIsolationLevel() {
    return null;
  }

  protected PlatformTransactionManager createTransactionManager() {
    return new DataSourceTransactionManager(this.dataSource);
  }

  private PlatformTransactionManager buildTransactionManager() {
    PlatformTransactionManager transactionManager = this.createTransactionManager();
    if (this.transactionManagerCustomizers != null) {
      this.transactionManagerCustomizers.customize(transactionManager);
    }

    return transactionManager;
  }
}
```

> SpringBoot 자동설정 : BatchAutoConfiguration > BatchConfigurerConfiguration > BasicBatchConfigurer (JDBC) <br/>
> 일부 설정을 override 하고 싶다면 **DefaultBatchConfigurer** 를 빈으로 등록해서 override 하면 된다.

```java
@Bean
public BatchConfigurer batchConfigurer(DataSource dataSource) {
    return new DefaultBatchConfigurer(dataSource) {
        @Override
        public JobLauncher getJobLauncher() {
            return super.getJobLauncher();
        }
    };
}
```

`TaskExecutor`
- Spring 2.0 부터 추가되었으며, JDK 1.5 에 추가된 Executor 인터페이스를 상속 받고 있다.
  - executor 는 스레드풀에 대한 Java 5 에 도입됨 개념 (실제 구현체가 pool 이라는 보장이 없기 때문에 executor 라는 이름을 사용함)
  - 이는 싱글 스레드가 될수도 있고, 동기화가 될 수도 있다.
- TaskExecutor 인터페이스는 Executor 와 동일하지만, 스레드풀 사용시 JDK 5 에 대한 요구사항을 추상화 하기 위함이다.
- SimpleAsyncTaskExecutor
- SyncTaskExecutor
- ConcurrentTaskExecutor
- SimpleThreadPoolTaskExecutor
- **ThreadPoolTaskExecutor** -> 가장 많이 사용할듯
- TimerTaskExecutor
- WorkManagerTaskExecutor

```java
@FunctionalInterface
public interface TaskExecutor extends Executor {
    void execute(Runnable var1);
}
```

`JobInstance`
- 배치 잡이 실행되면, **JobInstance** 가 생성된다.
- 이는 **잡의 논리적 실행** 을 나타내며 두 가지 항목으로 식별된다.
  - 잡 이름
  - 잡 파라미터
- 잡의 실행과 잡의 실행 시도는 다른 개념이고, JobInstance 는 한 번 성공적으로 완료되었다면, 다시 실행 수 없다.

`JobExecution`
- **잡의 실행 시도** 를 의미한다.
- 만약 잡이 처음 부터 끝까지 단 한번에 완료되었다면, JobInstance 와 JobExecution 은 하나 씩만 존재한다.
- 하지만 잡 실행후 성공적으로 완료되지 못했다면, 해당 JobInstance 를 재시도 할 때 마다 JobExecution 이 새롭게 생성된다.
  - 이 때 동일한 파라미터가 전달됨

`간단 정리`
- Job : 스프링 배치의 하나의 실행 단위
- Step : 잡의 구성요소, 하나의 잡에 여러 스탭이 존재할 수 있다.
- JobLauncher : 실제 잡을 실행하는 실행기
- JobInstance : 잡의 실행, 잡의 이름 / 잡 파라미터로 구성된 단위 (완료된 잡 인스턴스는 재 실행 될 수 없다.)
- JobExecution : 잡의 실행 시도 JobInstance 마다 존재하며, 이는 여럿이 존재할 수 있음

## 잡 구성하기

### 잡의 기본 구성

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-batch</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>
<dependency>
  <groupId>mysql</groupId>
  <artifactId>mysql-connector-java</artifactId>
  <scope>runtime</scope>
</dependency>
<dependency>
  <groupId>org.projectlombok</groupId>
  <artifactId>lombok</artifactId>
  <optional>true</optional>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-test</artifactId>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.springframework.batch</groupId>
  <artifactId>spring-batch-test</artifactId>
  <scope>test</scope>
</dependency>
```

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/spring_batch
    username: root
    password:
  batch:
    jdbc:
      initialize-schema: always
```

```java
@EnableBatchProcessing
@SpringBootApplication
public class Chapter04Application {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job job() {
        return this.jobBuilderFactory.get("basicJob")
            .start(step1())
            .build();
    }

    @Bean
    public Step step1() {
        return this.stepBuilderFactory.get("step1")
            .tasklet(contribution, chunkContext) -> {
                System.out.println("Hello, World!");
                return ReleatStatus.FINISHED;
            })
            .build();
    }
```

### 잡 파라미터
- JobInstance 가 성공적으로 완료되었다면, 동일한 파라미터를 넘겼을때 JobInstanceAlreadyCompleteException 이 발생한다.
- 잡 파라미터를 전달하는 방법은 잡을 호출하는 방법에 따라 달라진다.
  - CommandLineJobRunner / Quartz 와 같은 스케쥴러에서 잡을 실행하때 방식이 서로 다르기 때문이다.

`CommandLineJobRunner 에 파라미터 전달`

```shell
java -jar demo.jar name=Michael
```

> SpringBatch 의 JobParameters 는 스프링 부트의 명령행 기능으로 프로퍼티 구성하는 방식과 다르다. ('-' 접두사를 사용해서 전달하면 안됨) <br/>
> 추가적으로 시스템 프로퍼티와도 다르기 때문에 -D 아규먼트를 사용해서는 안된다.

`JobParameters`
- 사용자가 배치잡에 파라미터를 전달하면, 잡 러너는 **JobParameters** 인스턴스를 생성하는데 이는 잡이 전달받은 모든 파라미터의 컨테이너 역할을 한다.
- 실제로는 JobParameters 를 요소로 가진 Map 의 Wrapper 클래스에 불과하다.

```java
public class JobParameters implements Serializable {
    private final Map<String, JobParameter> parameters;
    // ...
}
```

`JobParameter`
- JobParameters 의 실제 값은 JobParameter 타입인데, 이는 타입 떄문이다.
- 스프링 배치는 파라미터의 타입 변환 기능을 제공하며, 변환 타입에 맞는 JobParameter 의 접근자를 제공한다.

```java
public class JobParameter implements Serializable {

  private final Object parameter;
  private final JobParameter.ParameterType parameterType;
  private final boolean identifying;
  // ...
}
```

```shell
java -jar demo.jar executionDate(date)=2020/12/27
```
- 파라미터의 타입지정은 괄호 () 를 사용해 지정해 줄 수 있는데 해당 타입 명은 모두 **소문자** 여야 한다.
  - LocalDate, LocalDateTime 은 지원하지 않는다

`식별되지 않는 파라미터`
- 식별 파라미터는 JobInstance 를 식별하는데 사용한다.
- 스프링 배치2.2 버전부터 잡 파라미터 전달시 JobInstance 의 식별에 사용할것인지 여부를 결정하는 방식이 추가되었다.
- 매번 잡 실행시마다 동적으로 변경되는 파라미터 등이 있을때 유용하다.
- 특정 잡 파라미터가 식별되지 않길 파란다면 접두사 '-' 를 사용한다.

```shell
java -jar demo.jar executionDate(date)=2020/12/27 -name=Michael
```

`잡 파라미터 접근`
- ItemReader, ItemProcessor, ItemWriter, Tasklet 인터페이스를 살펴보면 모두 JobParameters 를 파라미터로 전달받지 않는다.
- 잡 파라미터에 접근하는 방식은 접근 위치에 따라 여러 방식중 하나를 선택할 수 있다.
  - ChunkContext
    - 실행 시점의 잡 상태 제공
    - 태스크릿 내에서 처리중인 청크 관련 정보 제공 (스탭 및 잡 관련 정보 포함)
    - ChunkContext 는 JobParameters 를 포함한 StepContext 의 참조를 가지고 있다.
  - Late Binding (늦은 바인딩)
    - 스탭이나 잡을 제외한 특정 부분에 파라미터를 전달하는 가장 쉬운 방법
    - 스프링 설정을 사용해 주입하는 방식
    - Late Binding 을 통해 구성될 빈은 스텝 또는 잡 스코프를 지정해 주어야 한다.
    
```java
    @StepScope // StepScope 적용
    @Bean
    public Tasklet helloWorldTasklet(
        @Value("#{jobParameters['name']}") String name, // Late Binding Step Scope 를 적용해야 한다.
        @Value("#{jobParameters['fileName']}") String fileName
    ) {
        return (stepContribution, chunkContext) -> {
            // chunkContext.stepContext 를 통해 잡파라미터에 접근한다.
//            String name = (String) chunkContext.getStepContext() 
//                .getJobParameters()
//                .get("name");
            System.out.println(String.format("Hello, %s !", name));
            System.out.println(String.format("fileName = %s !", fileName));
            return RepeatStatus.FINISHED;
        };
    }
```

> 스프링 배치에 포함된 커스텀 스코프 및 잡스코프를 사용하면 Late Binding 기능을 손 쉽게 사용할 수 있다. <br/>
> 해당 스코프들의 기능은 스텝의 실행범위나 잡의 실행범위에 들어갈 때 까지 빈 생성을 지연시키는 것이다. <br/>
> 이로 인해 잡 파라미터를 빈 생성시점에 주입할 수 있다. 

### 잡 파라미터 유효성 검사
- JobParametersValidator 인터페이스를 구현하고, 해당 구현체를 잡 내에 구성하는 방식을 사용해 잡 파라미터에 대한 검증을 손 쉽게 할 수 있다.
  - JobParametersInvalidException 이 발생하지 않는다면 유효성 검증을 통과했다고 판단한다.

`JobParametersValidator`

```java
public interface JobParametersValidator {
  void validate(@Nullable JobParameters var1) throws JobParametersInvalidException;
}
```

`Valiadator Sample`

```java
public class ParameterValidator implements JobParametersValidator {

  @Override
  public void validate(JobParameters jobParameters) throws JobParametersInvalidException {
    String fileName = jobParameters.getString("fileName");

    if (!StringUtils.hasText(fileName)) {
      throw new JobParametersInvalidException("fileName Parameter is missing");
    } else if (!StringUtils.endsWithIgnoreCase(fileName, "csv")) {
      throw new JobParametersInvalidException(
              "fileName Parameter does not use the csv file extension");
    }
  }
}

@Bean
public Job job() {
  return this.jobBuilderFactory.get("basicJob")
          .start(step1())
          .validator(validator()) // validator 지정
          .build();
}

@Bean
public JobParametersValidator validator() {
    return new ParameterValidator();
}
```

`DefaultJobParametersValidator`

- 유효성 검사기를 직접 구현할 수 도 있지만, 스프링배치는 필수 파라미터가 누락되진 않았는지 검증하는 **DefaultJobParametersValidator** 를 제공한다.
  - requireKeys, optionalKeys 두 개의 인자가 존재하는데, 이는 필수 / 옵셔널한 파라미터 목록을 의미한다.

```java
@Bean
public JobParametersValidator validator() {
    DefaultJobParametersValidator validator = new DefaultJobParametersValidator();
    validator.setRequiredKeys(new String[]{"fileName"});
    validator.setRequiredKeys(new String[]{"name"});
    return validator;
}
```

`CompositeJobParametersValidator`
- 다수의 검증기를 적용하고 싶을 경우에 대비해 스프링 배치는 **CompositeJobParametersValidator** 를 제공한다.
  - 다음은 CompositeJobParametersValidator 의 사용 예이다.

```java
@Bean
public CompositeJobParametersValidator validator() {
    CompositeJobParametersValidator validator = new CompositeJobParametersValidator();
    DefaultJobParametersValidator defaultJobParametersValidator = new DefaultJobParametersValidator(
        new String[]{"fileName"},
        new String[]{"name", "currentDate"}
    );
    defaultJobParametersValidator.afterPropertiesSet();
    validator.setValidators(
        List.of(new ParameterValidator(), defaultJobParametersValidator)
    );
    return validator;
}
```

### 잡 파라미터 증가
- 주어진 식별 파라미터를 이용해 생성된 JobInstance 는 단 한번만 실행할 수 있는데, 이를 피하고 싶다면 간단한 방법이 있다.
- **JobParametersIncrementor** 를 이용하는 방법
- 이는 잡에서 사용할 파라미터를 고유하게 생성할 수 있도록 스프링 배치가 제공하는 인터페이스 이다.
  - 실행시 마다 타임스탬프를 추가한다거나, 실행시 마다 파라미터를 증가해야 하는 경우가 있을 수 있음
- 스프링 배치는 해당 인터페이스의 기본 구현체를 하나 제공하며, 이는 기본적으로 run.id 라는 파라미터 명을 가진 long 타입 파라미터 값을 증가시킨다.

```java
public interface JobParametersIncrementer {
    JobParameters getNext(@Nullable JobParameters var1);
}

public class RunIdIncrementer implements JobParametersIncrementer {
  private static String RUN_ID_KEY = "run.id";
  private String key;

  public RunIdIncrementer() {
    this.key = RUN_ID_KEY;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public JobParameters getNext(@Nullable JobParameters parameters) {
    JobParameters params = parameters == null ? new JobParameters() : parameters;
    JobParameter runIdParameter = (JobParameter)params.getParameters().get(this.key);
    long id = 1L;
    if (runIdParameter != null) {
      try {
        id = Long.parseLong(runIdParameter.getValue().toString()) + 1L;
      } catch (NumberFormatException var7) {
        throw new IllegalArgumentException("Invalid value for parameter " + this.key, var7);
      }
    }

    return (new JobParametersBuilder(params)).addLong(this.key, id).toJobParameters();
  }
}
```

```java
@Bean
public Job job() {
    return this.jobBuilderFactory.get("basicJob")
        .start(step1())
        .validator(validator()) // validator 지정
        .incrementer(new RunIdIncrementor()) // incrementer 지정
        .build();
}
```

> incrementer 변경시 주의... 이전에 실행되었던 잡의 파라미터도 체크함

`Timestamp Incrementor`

```java
public class DailyJobTimestamper implements JobParametersIncrementer {

    @Override
    public JobParameters getNext(JobParameters jobParameters) {
        return new JobParametersBuilder(jobParameters)
            .addDate("currentDate", new Date())
            .toJobParameters();
    }
}

```

## 참고
- https://hyeonyeee.tistory.com/73
- https://jeong-pro.tistory.com/188