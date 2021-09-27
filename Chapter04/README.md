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

### 잡 리스너
- 모든 잡은 생명주기를 가지고 있고, 스프링 배치에서는 거의 모든 측면에서의 생명주기가 정의되어 있다.
  - 생명주기의 여러 시점에 로직을 추가할 수 있는 기능을 제공

`JobExecutionListener`
- 잡의 실행과 관련된 리스너

```java
public interface JobExecutionListener {
    void beforeJob(JobExecution var1);

    void afterJob(JobExecution var1);
}
```

```java
/**
 * 잡 시작/전후 처리
 * JobExecutionListener 를 구현하는 방식 / 애노테이션 사용방식 두가지를 제공
 * 리스너는 스탭, 리더, 라이터 등 다양한 컴포넌트에도 사용이 가능하다.
 */
//public class JobLoggerListener implements JobExecutionListener {
public class JobLoggerListener {

    private static final String START_MESSAGE = "%s is beginning execution";
    private static final String END_MESSAGE = "%s has completed with the status %s";

//    @Override
    @BeforeJob
    public void beforeJob(JobExecution jobExecution) {
        System.out.println(
            String.format(START_MESSAGE, jobExecution.getJobInstance().getJobName()));
    }

//    @Override
    @AfterJob
    public void afterJob(JobExecution jobExecution) {
        System.out.println(String.format(END_MESSAGE, jobExecution.getJobInstance().getJobName(),
            jobExecution.getStatus()));
    }
}
```
- 인터페이스를 구현하는 방식과 애노테이션을 사용하는 방식 2가지를 지원한다.

```java
@Bean
public Job job() {
    return this.jobBuilderFactory.get("basicJob")
        .start(step1())
        .listener(new JobLoggerListener()) // interface 기반 listener 추가
        .listener(JobListenerFactoryBean.getListener(new JobLoggerListener())) // 애노테이션 기반 listener 추가
        .build();
}
```

## ExecutionContext
- 배치 처리 특성상 상태를 가지고 있다.
  - 어떤 스텝이 실행중인지, 스텝이 처리한 레코드 개수는 몇개 인지, 도중에 멈췄다면 어디서 부터 재시작 해야하는지..
- **JobExecution** 은 실제 잡 실행 시도를 나타낸다.
  - 이는 잡이나 스텝이 진행될 때 변경된다.
- 잡의 상태는 JobExecution 의 **ExecutionContext** 에 저장된다.
- 웹 애플리케이션이 HttpSession 사용해 상태를 저장한다면, ExecutionContext 는 이에 대응하는 배치 잡의 세션이다.
  - key/value 쌍을 보관하는 일종의 저장소
- ExecutionContext 는 잡의 상태를 **안전하게 보관하는 방법** 을 제공하며, 세션과의 차이는 잡을 다루는 과정에서 ExecutionContext 가 **여러개 존재 할 수 있다.** 는 점이다.
- JobExecution 와 마찬가지로 StepExecution 도 ExecutionContext 를 가진다.

> ExecutionContext 가 가지고 있는 모든 것은 JobRepository 에 저장되기 때문에 **안전하게 보관하는 방법** 을 제공한다고 표현한다.

### ExecutionContext 조작하기

```java
public class HelloWorld implements Tasklet {

    private static final String HELLO_WORLD = "Hello, %s";

    /**
     * ExecutionContext 에 저장되는 모든 데이터는 JobRepository 에 저장된다.
     * @param step
     * @param context
     * @return
     * @throws Exception
     */
    @Override
    public RepeatStatus execute(StepContribution step, ChunkContext context) throws Exception {
        String name = (String) context.getStepContext()
            .getJobParameters()
            .get("name");

        ExecutionContext jobContext = context.getStepContext()
            // .getJobExecutionContext() // 여기서 꺼낸 Context 로도 접근이 가능하지만 이는 READ-ONLY 임에 유의
            .getStepExecution()
            .getJobExecution()
            .getExecutionContext();

        jobContext.put("user.name", name);
        System.out.println(String.format(HELLO_WORLD, name));
        return RepeatStatus.FINISHED;
    }
}
```
- ExecutionContext 는 Job 또는 StepExecution 의 이루이기 때문에 JobExecution/StepExecution 을 통해 가져와야 한다.
- 여기서 한가지 주의할점은, 반드시 **Job/StepExecution 을 얻어온 후 ExecutionContext 에 접근해야한다.**
  - Job/StepContext 에서 다이렉트로 얻어온 ExecutionContext 는 **READ-ONLY (정확히는 휘발성)** 이기 때문에 오류 발생시 해당 내용이 소멸된다.

`StepExecution 의 Context 를 JobExecution 의 Context 로 승격`
- 정확히는 StepExecution 에 존재하는 Key 를 JobExecution 의 Key 로 승격하는 방법이다.
  - 이렇게 하면 StepExecution 에서도 JobExecution 의 ExecutionContext 를 조작할 수 있다.

```java
/**
 * JobExecution 의 Execution Context 를 조작하는 또 다른 방법
 * > StepExecution 의 Execution Context 의 키를 JobExecution 의 Execution Context 로 승격시키는 방법
 * 스탭간의 공유할 데이터가 있지만, 첫 번째 스탭이 성공후 공유하고 싶다거나 할 때 유용하다.
 *
 * 또 다른 방법은 ItemStream 인터페이스를 사용하는 방법
 */
@Bean
public StepExecutionListener promotionListener() {
    /**
     * name 키를 찾으면 JobExecution 의 ExecutionContext 로 복사한다.
     * name 키가 없어도 아무일도 일어나지 않지만, 예외가 발생하도록 구성도 가능하다.
     */
    ExecutionContextPromotionListener listener = new ExecutionContextPromotionListener();
    listener.setKeys(new String[] {"name"});
    return listener;
}
```

### ExecutionContext 저장하기
- 잡이 처리되는 동안 스프링 배치는 청크를 커밋하면서 잡 또는 스탭의 상태를 저장한다.
  - 정확히 말하면 잡과 스탭의 현재 ExecutionContext 를 데이터베이스에 저장한다.
  - BATCH_JOB_EXECUTION_CONTEXT 테이블

## 스탭 알아보기
- 잡이 전체적인 처리를 **정의** 하는 거라면, 스탭은 **잡의 구성 요소** 를 담당한다.
- 스탭은 **독립적 이고, 순차적으로 배치처리를 수행** 한다.
  - 스탭을 **배치 프로세서** 라고도 표현한다.
- 스탭은 모든 단위의 작업 조각이며, 자체적인 입력처리 및 처리기를 가질 수 있으며 자체적인 출력을 처리한다.
- 트랜잭션은 스탭내에서 이뤄지고, 스탭은 서로 **독립되도록 설계** 되었다.

### 태스크릿과 청크
- 배치작업은 단순한 작업 또는 대량의 데이터를 처리하는 작업일 수 있다.
- 스프링 배치는 두 가지 유형의 처리 모델을 모두 지원한다.

`Tasklet`
- 지금까지 살펴보았던 예제에 존재하는 가장 단순한 모델
- Tasklet 인터페이스 이용해 손쉽게 구현할 수 있다.
  - execute 메소드가 RepeatStatus.FINISHED 를 반환할 때 까지 트랜잭션 범위내에서 반복적으로 실행하는 코드블록을 만들 수 있다.

```java
public interface Tasklet {
    @Nullable
    RepeatStatus execute(StepContribution var1, ChunkContext var2) throws Exception;
}
```

`Chunk`
- 청크 기반 모델은 최소 2 ~ 3 개의 주요 컴포넌트로 구성된다.
  - ItemReader
  - ItemProcessor (Optional)
  - ItemWriter
- 각 청크는 자체 트랜잭션으로 실행되며, 처리에 실패했을 경우 마지막으로 성공한 트랜잭션 이후부터 재시작 할 수 있다.
- 위에서 언급한 3가지 컴포넌트를 사용하면 스프링 배치는 3가지 루프를 수행한다.
- **ItemReader**
  - 청크 단위로 처리할 모든 레코드를 반복저긍로 메모리에 읽어온다.
- **ItemProcessor**
  - 메모리로 읽어들인 아이템들이 Processor 를 거쳐 처리된다.
- **ItemWriter**
  - 마지막으로 한번에 기록할 수 잇는 ItemWriter 를 호출하며 모든 아이템을 전달한다.
  - 이는 물리적인 쓰기를 일괄처리하몀으로써 I/O 최적화를 수행한다.

### 스탭 구성

`태스크릿 스탭`
- 태스크릿 스탭을 만드는 방법은 크게 두가지 방법이 있다.
- Tasklet 인터페이스를 구현 / 또는 별개로 정의한 클래스를 태스크릿 스탭처럼 실행 되도록 하는 방법 (MethodInvokingTaskletAdapter)
- 그외에도 여러 방법이 존재하는데 하나씩 살펴본다.

`CallableTaskletAdapter`
- java.util.concurrent.Callable<RepeatStatus> 인터페이스의 구현체를 구성할 수 있게 해주는 어댑터
  - 별개의 스레드에서 실행되긴 하지만, **병렬로 실행되지는 않는다.**
- 유효한 RepeatStatus 를 반환하기 전까지는 완료된 것으로 간주되지 않는다.

```java
/**
 * CallableTasklet 은 별개의 스레드에서 실행되지만, **병렬로 실행되지는 않는다.**
 */
@EnableBatchProcessing
@SpringBootApplication
public class CallableTaskletConfiguration {

  @Autowired
  private JobBuilderFactory jobBuilderFactory;

  @Autowired
  private StepBuilderFactory stepBuilderFactory;

  @Bean
  public Job callableJob() {
    return this.jobBuilderFactory.get("callableJob")
            .start(callableStep())
            .build();
  }

  @Bean
  public Step callableStep() {
    return this.stepBuilderFactory.get("callableStep")
            .tasklet(tasklet())
            .build();
  }

  @Bean
  public Callable<RepeatStatus> callableObject() {
    return () -> {
      System.out.println("This wa executed in another thread");
      return RepeatStatus.FINISHED;
    };
  }

  @Bean
  public CallableTaskletAdapter tasklet() {
    CallableTaskletAdapter adapter = new CallableTaskletAdapter();
    adapter.setCallable(callableObject());
    return adapter;
  }

  public static void main(String[] args) {
    SpringApplication.run(CallableTaskletConfiguration.class, args);
  }
}
```

`MethodInvokingTaskletAdapter`
- 기존의 달느 클래스를 잡 의 태스크릿처럼 실행할 수 있다.
- Tasklet 인터페이스를 구현하는 대신 이를 이용해 해당 메소드를 호출할 수 있다.
- Late Binding 을 통해 파라메터를 전달할 수도 있음

```java
@EnableBatchProcessing
@SpringBootApplication
public class MethodInvokingTaskletConfiguration {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job methodInvokingJob() {
        return this.jobBuilderFactory.get("methodInvokingJob")
            .start(methodInvokingStep())
            .build();
    }

    @Bean
    public Step methodInvokingStep() {
        return this.stepBuilderFactory.get("methodInvokingStep")
            .tasklet(methodInvokingTasklet(null))
            .build();
    }

    /**
     * TargetObject 의 메소드가 ExitStatus 를 반환하지 않는다면, ExitStatus.COMPLETED 를 반환한다.
     */
    @StepScope
    @Bean
    public MethodInvokingTaskletAdapter methodInvokingTasklet(
        @Value("#{jobParameters['message']}") String message
    ) {
        MethodInvokingTaskletAdapter adapter = new MethodInvokingTaskletAdapter();
        adapter.setTargetObject(service());
        adapter.setTargetMethod("serviceMethod");
        adapter.setArguments(new String[] {message});
        return adapter;
    }

    @Bean
    public CustomerService service() {
        return new CustomerService();
    }

    static class CustomerService {

        public void serviceMethod(String message) {
            System.out.println(String.format("Service method was called... message = %s", message));
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(MethodInvokingTaskletConfiguration.class, args);
    }
}
```

`SystemCommandTasklet`
- 시스템 명령을 실행할 때 사용한다.
- 해당 명령은 **비동기로 실행** 된다.
  - 타임아웃을 지정할 수 있으며,  이는 밀리초 단위이다.
- interruptOnCancel 속성은 잡이 비정상 종료되었을때, 해당 프로세스 관련 스레드를 강제 종료할 것인지 여부이다.

```java
/**
 * 시스템 명령 실행시 사용된다. (비동기로 실행됨)
 */
@EnableBatchProcessing
@SpringBootApplication
public class SystemCommandTaskletConfiguration {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job job() {
        return this.jobBuilderFactory.get("systemCommandJob")
            .start(systemCommandStep())
            .build();
    }

    @Bean
    public Step systemCommandStep() {
        return this.stepBuilderFactory.get("systemCommandStep")
            .tasklet(systemCommandTasklet())
            .build();
    }

    @Bean
    public SystemCommandTasklet  systemCommandTasklet() {
        SystemCommandTasklet tasklet = new SystemCommandTasklet();

        tasklet.setCommand("echo hello world!");
        tasklet.setTimeout(5_000);
        // interruptOnCancel 애트리뷰트 사용 여부는 선택사항
        // 잡이 비정상적 종료시 시스템 프로세스 관련 스레드를 강제로 종료할 것인지 지정
        tasklet.setInterruptOnCancel(true);

        // 시스템 명령 실행시 영향을 주는 파라미터 옵션

        // 명령을 실행할 디렉터리
        tasklet.setWorkingDirectory("/Users/kakaocommerce/work/Spring-Batch-Study/Chapter04");

        // 시스템 코드는 실행 명령에 따라 다른 의미를 가질 수 있다.
        // 시스템 코드를 스프링 배치 상태 값으로 매핑 가능한 구현체를 지정할 수 있다.
        // 기본적으로 두가지 구현체를 제공한다.
        tasklet.setSystemProcessExitCodeMapper(touchCodeMapper());

        // 시스템 명령은 기본적으로 비동기로 실행된다.
        // 명령 실행후 테스크릿이 완료 여부를 주기적으로 확인함. 해당 주기 설정 (기본값 1초)
        tasklet.setTerminationCheckInterval(5_000);

        // 시스템 명령을 실행하는 고유한 실행기 구성
        // 문제발생시 락이 걸릴수 있기 때문에 동기식은 구성하지 않는것이 좋음
        tasklet.setTaskExecutor(new SimpleAsyncTaskExecutor());

        // 명령 실행전 적용할 환경 파라미터들 ..
        tasklet.setEnvironmentParams(new String[] {
//            "JAVA_HOME=/java",
//            "BATCH_HOME=/Users/batch"
        });
        return tasklet;
    }

    @Bean
    public SimpleSystemProcessExitCodeMapper touchCodeMapper() {
        return new SimpleSystemProcessExitCodeMapper();
    }

    public static void main(String[] args) {
        SpringApplication.run(SystemCommandTaskletConfiguration.class, args);
    }
}
```

`청크 스탭`
- 청크는 커밋 간격 (Commit Interval) 에 의해 정의 된다.
- 만약 커밋 간격을 50으로 지정했다면, 50개를 읽고, 50개를 처리 한후 50개를 기록한다.

```java
@EnableBatchProcessing
@SpringBootApplication
public class ChunkBatchConfiguration {

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
            .<String, String>chunk(10) // 10개 단위로 레코드 처리후 작업이 커밋됨
            .reader(itemReader(null))
            .writer(itemWriter(null))
            .build();
  }

  @StepScope
  @Bean
  public FlatFileItemReader<String> itemReader(
          @Value("#{jobParameters['inputFile']}") Resource inputFile
  ) {
    return new FlatFileItemReaderBuilder<String>()
            .name("itemReader")
            .resource(inputFile)
            .lineMapper(new PassThroughLineMapper())
            .build();
  }

  @StepScope
  @Bean
  public FlatFileItemWriter<String> itemWriter(
          @Value("#{jobParameters['outputFile']}") Resource outputFile
  ) {
    return new FlatFileItemWriterBuilder<String>()
            .name("itemWriter")
            .resource(outputFile)
            .lineAggregator(new PassThroughLineAggregator<>())
            .build();
  }

}
```
- 위 예제에서는 10개 단위로 청크사이즈를 지정하였으며, 청크 기반 스탭은, build 메소드 호출 이전에 리더 및 라이터의 구현체를 가져온다.
- 적절한 커밋 간격을 지정하는 것이 중요하다.
- 10개의 레코드를 읽고, 처리할때 까지 레코드 쓰기 작업을 진행하지 않는다는 것을 의미한다.
- 만약 9개를 처리한 후 오류가 발생했다면, 청크 (트랜잭션) 을 롤백처리하고, 잡이 실패했다고 마킹한다.

`청크 크기 구성`
- 청크 기반처리는 스프링 배치의 토대가 된다.
- 이를 최대한 활용하기 위해 다양한 구성 방법에 대한 이해가 중요하다.
- 청크 크기를 구성하는 방식은 **정적인 방식** 과 **CompletionPolicy** 구현체 활용 방법 두가지가 있다. 

`CompletionPolicy`
- 청크가 완료되는 시점을 프로그래밍 방식으로 정의할 수 있는 기능을 제공하며, 많은 구현체를 제공한다.
- 그 중 많이 사용되는 **SimpleCompletionPolicy** 와 **TimeoutTerminationPolicy** 가 있다.
- SimpleCompletionPolicy
  - 아이템 갯수 기반으로 동작한다.
  - 지정한 아이템 갯수의 임계값 ex) 1000 을 넘어서면 청크 완료로 표시한다.
- TimeoutTerminationPolicy
  - 시간을 기반으로 동작한다.
  - 지정한 시간의 임계값 ex) 3 을 넘어서면 청크 완료로 표시한다.

```java
public interface CompletionPolicy {
    boolean isComplete(RepeatContext var1, RepeatStatus var2);

    boolean isComplete(RepeatContext var1);

    RepeatContext start(RepeatContext var1);

    void update(RepeatContext var1);
}
```

```java
@EnableBatchProcessing
@SpringBootApplication
public class ChunkBasedJobConfiguration {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job chunkBasedJob() {
        return this.jobBuilderFactory.get("chunkBasedJob")
            .start(chunkStep())
            .build();
    }

    /**
     * 동적인 Chunk 사이즈 조절을 위한 CompletionPolicy 인터페이스를 제공해준다.
     * 기본 구현체는 SimpleCompletionPolicy -> 처리된 아이템 갯수를 이용해 임계값에 도달하면 완료 처리
     * TimeoutTerminationPolicy -> 처리시간이 임계값에 도달하면 해당 청크가 완료된 것으로 간주한다.
     * CompositeCompletionPolicy -> 청크완료 여부를 여러 정책들을 이용해 구성 가능 (자신이 가진 정책중 하나라도 만족하면 완료처리)
     */
    @Bean
    public Step chunkStep() {
        return this.stepBuilderFactory.get("chunkStep")
            // .<String, String>chunk(1_000) // 일반적으로 청크사이즈를 하드코딩하지만 모든 상황에 적절한 것은 아니다.
//            .<String, String>chunk(completionPolicy())
            .<String, String>chunk(randomCompletionPolicy())
            .reader(itemReader())
            .writer(itemWriter())
            .listener(new LoggingStepStartStopListener())
            .build();
    }

    @Bean
    public ListItemReader<String> itemReader() {
        List<String> items = new ArrayList<>(100_000);

        for (int i = 0; i < 100_000; i++) {
            items.add(UUID.randomUUID().toString());
        }

        return new ListItemReader<>(items);
    }

    @Bean
    public ItemWriter<String> itemWriter() {
        return items -> {
            for (String item : items) {
                System.out.println(">> current item = " + item);
            }
        };
    }

    @Bean
    public CompletionPolicy completionPolicy() {
        CompositeCompletionPolicy policy = new CompositeCompletionPolicy();
        policy.setPolicies(
            new CompletionPolicy[]{
                new TimeoutTerminationPolicy(3),
                new SimpleCompletionPolicy(1_000)
            }
        );
        return policy;
    }

    @Bean
    public CompletionPolicy randomCompletionPolicy() {
        return new RandomChunkSizePolicy();
    }

    public static void main(String[] args) {
        SpringApplication.run(ChunkBasedJobConfiguration.class, args);
    }
}
```
- 위 예제에서 CompositeCompletionPolicy 를 사용하였다.
- 이는 여러 CompletionPolicy 를 조합해서 사용할 수 있도록 제공한다.
- 여러 정책중 하나라도 완료되었다고 판단되면 해당 청크가 완료된것으로 간주한다.

`RandomChunkSizePolicy`
- CompletionPolicy 를 직접 구현해서 사용할 수 도 있다.
- 다음 은 랜덤하게 청크 크기를 지정하도록 작성한 예시이다.

```java
public class RandomChunkSizePolicy implements CompletionPolicy {

    private int chunkSize;
    private int totalProcessed;
    private Random random = new Random();

    @Override
    public boolean isComplete(RepeatContext repeatContext, RepeatStatus repeatStatus) {
        if (RepeatStatus.FINISHED == repeatStatus) {
            return true;
        }
        return isComplete(repeatContext);
    }

    @Override
    public boolean isComplete(RepeatContext repeatContext) {
        return this.totalProcessed >= chunkSize;
    }

    @Override
    public RepeatContext start(RepeatContext repeatContext) {
        this.chunkSize = random.nextInt(20);
        this.totalProcessed = 0;

        System.out.println("The Chunk Size has been set to " + this.chunkSize);
        return repeatContext;
    }

    @Override
    public void update(RepeatContext repeatContext) {
        this.totalProcessed++;
    }
}
```

### 스텝 리스너
- 잡 리스너와 동일하게 스탭도 동일한 유형의 이벤트를 처리할 수 있다.
- StepExecutionListener 와 ChunkListener 인터페이스가 존재한다.
- 각 인터페이스는 스텝/청크 의 시작과 끝에서 특정 로직을 수행할 수 있게 해준다.

```java
public interface StepExecutionListener extends StepListener {
  void beforeStep(StepExecution var1);

  @Nullable
  ExitStatus afterStep(StepExecution var1);
}

public interface ChunkListener extends StepListener {
    String ROLLBACK_EXCEPTION_KEY = "sb_rollback_exception";

    void beforeChunk(ChunkContext var1);

    void afterChunk(ChunkContext var1);

    void afterChunkError(ChunkContext var1);
}
```

> 주의할 점은 스탭 관련 리스너는 **StepExecutionListener** 라는 점, StepListener 라는 인터페이스가 별도로 존재하긴 하지만, 이는 모든 스탭리스너가 상속받는 **마커인터페이스** 이다.

```java
public class LoggingStepStartStopListener implements StepExecutionListener {

    @Override
    public void beforeStep(StepExecution stepExecution) {
        System.out.println(stepExecution.getStepName() + " has begun!");
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        System.out.println(stepExecution.getStepName() + " has ended!");
        return stepExecution.getExitStatus();
    }
}
```
- 잡 리스너와 마찬가지로 위와 같이 인터페이스를 구현하는 방식, 그리고 @BeforeStep, @AfterStep 과 같은 애노테이션 방식을 지원한다.

## 스텝 플로우
- 지금 까지는 각 스텝을 줄을 세워 **순서대로 실행** 했다.
- 하지만 이런 방식으로만 스텝을 사용할 수 있다면 배치 사용방법은 매우 제한적일 것이다.
- 스프링 배치는 잡의 흐름을 커스터마이징 할 수 있는 여러 방법을 제공한다.

### 조건 로직
- 스프링 배치의 스탭은 잡 내에서 StepBuilder 의 next 메소드를 사용해 지정한 순대로 실행한다.
- 스탭을 다른 순서로 실행하는것도 쉬운데, **전이 (transition)** 을 구성하면 된다.

```java
@Bean
public Tasklet passTasklet() {
    return (stepContribution, chunkContext) -> RepeatStatus.FINISHED;
}

@Bean
public Tasklet successTasklet() {
  return (stepContribution, chunkContext) -> {
    System.out.println("Success!");
    return RepeatStatus.FINISHED;
  };
}

@Bean
public Tasklet failTasklet() {
    return (stepContribution, chunkContext) -> {
        System.out.println("Failure!");
        return RepeatStatus.FINISHED;
    };
}

@Bean
public Step firstStep() {
    return this.stepBuilderFactory.get("firstStep")
      .tasklet(passTasklet())
      .build();
}

@Bean
public Step successStep() {
    return this.stepBuilderFactory.get("successStep")
      .tasklet(successTasklet())
      .build();
}

@Bean
public Step failureStep() {
    return this.stepBuilderFactory.get("failureStep")
      .tasklet(failTasklet())
      .build();
}

@Bean
public Job job() {
    // pattern matching 을 지원함
    // ExitStatus 는 **문자열** 이기 때문에 패턴 매칭을 통해 여러 상태로 매칭할 수 있다.
    // ex) C* -> COMPLETE, CORRECT
    return this.jobBuilderFactory.get("conditionalJob")
        .start(firstStep())
        .on("FAILED").to(failureStep())
        .from(firstStep()).on("*").to(successStep()) // 성공할 경우 successStep
        .end()
        .build();
}
```
- 위 잡 구성은, firstStep 의 실행결과가 FAILED 라면 failureStep 을, 성공했다면 successStep 을 실행하도록 전이를 구성한 예이다.
- on() 메소드는 스프링 배치가 스탭의 ExitStatus 를 평가해 어떤 작업을 수행할지 결정하도록 구성하는 메소드이다.

`BatchStatus 와 ExitStatus`
- BatchStatus 는 잡이나 스텝의 **현재 상태를 식별** 하는 Job/StepExecution 의 속성이다.
- ExitStatus 는 잡이나 스텝 종료시 스프링 배치로 반환되는 값이다.
- 스프링 배치는, 이후에 어떤 스텝을 수행할지 결정할 때 **ExitStatus** 를 확인한다.
- ExitStatus 는 문자열 이기 때문에 와일드 카드를 통해 패턴 매칭이 가능하다.
  - *, ? 두개의 와일드 카드를 지원한다.
  - * : 0 개 이상의 문자를 일치 ex) C* = C, COMPLETE, CORRECT
  - ? : 1개 의 문자를 일치 ex) ?AT = CAT, KAT

> ExitStatus 만으로는 현재 스탭에서 특정 레코드를 스킵했을때 특정 스탭을 실행하지 않는다거나 이런 구성을 하기엔 제한적이다.

`JobExecutionDecider`
- ExitStatus 만으로 판단하기 힘든 전이상태를 프로그래밍 적으로 작성할 수 있게 지원하는 인터페이스 이다.

```java
public interface JobExecutionDecider {
    FlowExecutionStatus decide(JobExecution var1, @Nullable StepExecution var2);
}

public class RandomDecider implements JobExecutionDecider {

  private Random random = new Random();

  @Override
  public FlowExecutionStatus decide(JobExecution jobExecution, StepExecution stepExecution) {

    if (random.nextBoolean()) {
      return new FlowExecutionStatus(FlowExecutionStatus.COMPLETED.getName());
    }
    return new FlowExecutionStatus(FlowExecutionStatus.FAILED.getName());
  }
}
```

```java
@EnableBatchProcessing
@SpringBootApplication
public class ConditionalJobConfiguration {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Bean
    public Tasklet passTasklet() {
        return (stepContribution, chunkContext) -> RepeatStatus.FINISHED;
    }

    @Bean
    public Tasklet successTasklet() {
        return (stepContribution, chunkContext) -> {
            System.out.println("Success!");
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    public Tasklet failTasklet() {
        return (stepContribution, chunkContext) -> {
            System.out.println("Failure!");
            return RepeatStatus.FINISHED;
        };
    }

    /**
     * ExitStatus 를 이용해 다음에 어떤 스탭을 실행할 지 결정하게 할 수 있다. 하지만 이만으론 부족할때가 있음 특정 레코드를 건너뛰었다면 특정 스탭을 실행하지
     * 않게 한다거나 ... 이를 위해 JobExecutionDecider 인터페이스를 제공한다.
     */
    @Bean
    public Job job() {
        // pattern matching 을 지원함
        // ExitStatus 는 **문자열** 이기 때문에 패턴 매칭을 통해 여러 상태로 매칭할 수 있다.
        // ex) C* -> COMPLETE, CORRECT
        return this.jobBuilderFactory.get("conditionalJob")
            .start(firstStep())
            .next(decider())
            .from(decider())
            .on("FAILED")
//            .fail() // 실패시 FAIL 상태로 기록 / 동일한 잡으로 재실행 가능
            .stopAndRestart(successStep()) // 실패시 STOPPED 상태로 기록되며 / 지정한 step 부터 재시작 된다.
//            .to(failureStep()) // 실패할 경우 failureStep
            .from(decider()).on("*").to(successStep()) // 성공할 경우 successStep
            .end()
            .build();
    }

    @Bean
    public Step firstStep() {
        return this.stepBuilderFactory.get("firstStep")
            .tasklet(passTasklet())
            .build();
    }

    @Bean
    public Step successStep() {
        return this.stepBuilderFactory.get("successStep")
            .tasklet(successTasklet())
            .build();
    }

    @Bean
    public Step failureStep() {
        return this.stepBuilderFactory.get("failureStep")
            .tasklet(failTasklet())
            .build();
    }

    @Bean
    public JobExecutionDecider decider() {
        return new RandomDecider();
    }

    @Bean
    public BatchConfigurer batchConfigurer(DataSource dataSource) {
        return new DefaultBatchConfigurer(dataSource) {
            @Override
            public JobLauncher getJobLauncher() {
                return super.getJobLauncher();
            }
        };
    }

    public static void main(String[] args) {
        SpringApplication.run(ConditionalJobConfiguration.class, args);
    }
}
```

### 잡 종료하기
- 스프링 배치에서 잡을 프로그래밍적으로 종료하려면 아래 세가지 상태로 종료할 수 있다.
- Completed
  - 스프링 배치가 성공적으로 종료됨
  - 동일한 파라미터로 재실행할 수 없다.
- Failed
  - 잡이 성공적으로 완료되지 않음
  - 스프링 배치를 사용해 동일한 파라미터로 재실행 할 수 있다.
- Stopped
  - 재실행할 수 있다.
  - 잡에 오류가 발생하지 않았지만 중단된 위치에서 잡을 다시 실행할 수 있다.
  - 스탭 사이에 사람의 개입이 필요하거나 검사 및 처리가 필요한 경우 유용하다.

> ExitStatus 는 스탭, 청크, 잡에서 반환 될 수 있으며 JobRepository 에 저장할 BatchStatus 를 판단할 때 이를 활용한다.

- Completed, Failed 상태는 각각 end(), fail() 메소드로 해당 상태를 반환할 수 있는데, stopped 상태는 조금 특이하다.
  - stopAndRestart 메소드를 통해 재시작시 지정한 스탭부터 진행하게 된다.

```java
    @Bean
    public Job job() {
        // pattern matching 을 지원함
        // ExitStatus 는 **문자열** 이기 때문에 패턴 매칭을 통해 여러 상태로 매칭할 수 있다.
        // ex) C* -> COMPLETE, CORRECT
        return this.jobBuilderFactory.get("conditionalJob")
            .start(firstStep())
            .next(decider())
            .from(decider())
            .on("FAILED")
//            .fail() // 실패시 FAIL 상태로 기록 / 동일한 잡으로 재실행 가능
            .stopAndRestart(successStep()) // 실패시 STOPPED 상태로 기록되며 / 지정한 step 부터 재시작 된다.
//            .to(failureStep()) // 실패할 경우 failureStep
            .from(decider()).on("*").to(successStep()) // 성공할 경우 successStep
            .end()
            .build();
    }
```

### 플로우 외부화 하기
- 스프링 배치에서 스탭의 순서를 외부화 하는 방법은 세 가지 이다.
1. 스탭의 시퀀스를 독자적인 플로우로 만드는 방법
2. 플로우 스탭을 사용하는 방법
3. 잡내에서 다른 잡을 호출하는 방법

`플로우 정의`

```java
@EnableBatchProcessing
@SpringBootApplication
public class FlowJob {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Bean
    public Tasklet loadStockFile() {
        return (contribution, chunkContext) -> {
            System.out.println("The stock file has been loaded");
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    public Tasklet loadCustomerFile() {
        return (contribution, chunkContext) -> {
            System.out.println("The customer file has been loaded");
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    public Tasklet updateStart() {
        return (contribution, chunkContext) -> {
            System.out.println("The start has been updated");
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    public Tasklet runBatchTasklet() {
        return (contribution, chunkContext) -> {
            System.out.println("The batch file has been run");
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    public Step loadFileStep() {
        return this.stepBuilderFactory.get("loadFileStep")
            .tasklet(loadStockFile())
            .build();
    }

    @Bean
    public Step loadCustomerStep() {
        return this.stepBuilderFactory.get("loadCustomerStep")
            .tasklet(loadCustomerFile())
            .build();
    }

    @Bean
    public Step updateStartStep() {
        return this.stepBuilderFactory.get("updateStartStep")
            .tasklet(updateStart())
            .build();
    }

    @Bean
    public Step runBatch() {
        return this.stepBuilderFactory.get("runBatch")
            .tasklet(runBatchTasklet())
            .build();
    }

    @Bean
    public Flow preProcessingFlow() {
        return new FlowBuilder<Flow>("preProcessingFlow").start(loadFileStep())
            .next(loadCustomerStep())
            .next(updateStartStep())
            .build();
    }

    @Bean
    public Job conditionalStepLogicJob() {
        return this.jobBuilderFactory.get("conditionalStepLogicJob")
            .start(preProcessingFlow())
            .next(runBatch())
            .end()
            .build();
    }

    public static void main(String[] args) {
        SpringApplication.run(FlowJob.class, args);
    }
}
```
- 플로우는 잡과 비슷한 방식으로 구성한다.
- 이를 실행한 뒤 JobRepository 를 보면 플로우의 스탭이 잡의 일부분으로 저장되어 있는걸 확인할 수 있다.
- 결과적으로는 플로우를 사용하는것과 잡 내에서 스탭을 직접 구성하는 것에는 차이는 없다.

`플로우 스탭`
- 위와 플로우의 구성은 동일하지만, 플로우를 잡 빌더로 전달하지 않고 해당 **플로우를 스탭으로 래핑한뒤 해당 스탭을 잡으로 전달** 한다.

```java
/**
 * FlowStep
 * 플로우를 스탭으로 래핑한 뒤 잡으로 전달한다.
 */
@Bean
public Step initBatch() {
    return this.stepBuilderFactory.get("initBatch")
        .flow(preProcessingFlow())
        .build();
}
@Bean
public Job conditionalStepLogicJob() {
    return this.jobBuilderFactory.get("conditionalStepLogicJob")
//            .start(preProcessingFlow()) // 플로우 방식
    .start(initBatch()) // 스탭 플로우 방식
    .next(runBatch())
//            .end()
    .build();
}
```
- 플로우 스탭을 사용하면, 플로우가 담긴 스탭을 **하나의 스탭처럼 기록** 한다.
- 이는 모니터링 과 리포팅에 이점이 있다.
- 개별 스탭을 집계하지 않아도 플로우의 영향을 전체적으로 볼 수 있다.

`잡 스탭`
- 스탭을 전혀 외부화하지 않는 방식
- 플로우를 작성하는 대신 **잡에서 다른 잡을 호출** 한다.

```java
@EnableBatchProcessing
@SpringBootApplication
public class JobJob {


    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Bean
    public Tasklet loadStockFile() {
        return (contribution, chunkContext) -> {
            System.out.println("The stock file has been loaded");
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    public Tasklet loadCustomerFile() {
        return (contribution, chunkContext) -> {
            System.out.println("The customer file has been loaded");
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    public Tasklet updateStart() {
        return (contribution, chunkContext) -> {
            System.out.println("The start has been updated");
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    public Tasklet runBatchTasklet() {
        return (contribution, chunkContext) -> {
            System.out.println("The batch file has been run");
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    public Step loadFileStep() {
        return this.stepBuilderFactory.get("loadFileStep")
            .tasklet(loadStockFile())
            .build();
    }

    @Bean
    public Step loadCustomerStep() {
        return this.stepBuilderFactory.get("loadCustomerStep")
            .tasklet(loadCustomerFile())
            .build();
    }

    @Bean
    public Step updateStartStep() {
        return this.stepBuilderFactory.get("updateStartStep")
            .tasklet(updateStart())
            .build();
    }

    @Bean
    public Step runBatch() {
        return this.stepBuilderFactory.get("runBatch")
            .tasklet(runBatchTasklet())
            .build();
    }

    @Bean
    public Job conditionalStepLogicJob() {
        return this.jobBuilderFactory.get("conditionalStepLogicJob")
            .start(initBatch())
            .next(runBatch())
            .build();
    }

    @Bean
    public Job preProcessingJob() {
        return this.jobBuilderFactory.get("preProcessingJob")
            .start(loadFileStep())
            .next(loadCustomerStep())
            .next(updateStartStep())
            .build();
    }

    /**
     * JobStep 잡 내에서 다른 잡을 호출하게 한다.
     */
    @Bean
    public Step initBatch() {
        return this.stepBuilderFactory.get("initBatch")
            .job(preProcessingJob())
            .parametersExtractor(new DefaultJobParametersExtractor())
            .build();
    }

    public static void main(String[] args) {
        SpringApplication.run(JobJob.class, args);
    }
}
```
- 기존 잡구성과 차이점은 parametersExtractor 를 사용한다는 점
- 잡을 구동하면 해당 잡의 이름으로 구동되고, 잡은 **잡 이름과 잡 파라미터로 식별** 된다.
  - 잡 스탭도 JobExecutionContext 가 생성된다.
- 사용자는 서브잡인 preProcessingJob 에 잡 파라미터를 전달하지 않고, 상위 잡에서 잡파라미터 는 ExecutionContext 를 추출해 하위잡으로 전달하는 클래스를 정의해야 한다.
- preProcessingJob 은 다른 잡과 마찬가지로 JOB_INSTANCE 로 기록된다.

> 잡스텝은 오히려 실행제어시 제약이 될 수 있다. <br/>
> 오히려 잡 관리기능은 **단일 잡 수준** 에서 이뤄지기 때문에 잡 스탭기능을 이용해서 잡을 트리구조로 만들어 관리하면 문제가 될 수 있다.


## 참고
- https://hyeonyeee.tistory.com/73
- https://jeong-pro.tistory.com/188