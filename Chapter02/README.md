# 2장 스프링 배치

## 간단한 예제

```java
@EnableBatchProcessing
@SpringBootApplication
public class Chapter02Application {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Bean
    public Step step() {
        return this.stepBuilderFactory.get("step1")
            .tasklet((stepContribution, chunkContext) -> {
                System.out.println("Hello, World!!");
                /**
                 * 테스클릿이 FINISHED 상태가 되었음을 알림, CONTINUABLE 상태는 지속되어야 함을 알림
                 * CONTINUABLE 상태를 계속해서 반환한다면 스탭이 지속적으로 실행됨에 유의
                 */
                return RepeatStatus.FINISHED;
            }).build();
    }

    @Bean
    public Job job() {
        return this.jobBuilderFactory.get("job")
            .start(step())
            .build();
    }

    public static void main(String[] args) {
        SpringApplication.run(Chapter02Application.class, args);
    }

}
```

## EnableBatchProcessing
- 스프링 배치가 제공
- 배치 인프라스트럭쳐를 부트스트래핑 한다.

`인프라스트럭쳐 구성 컴포넌트`
- JobRepository : 잡의 상태 기록
- JobLauncher : 잡을 구동
- JobExplorer : JobRepository 를 이용해 읽기 전용 작업을 수행
- JobRegistry : 특정한 런쳐 구현체 사용시 잡을 찾는 용도
- PlatformTransactionManager : 트랜잭션
- JobBuilderFactory : 잡을 생성하는 빌더
- StepBuilderFactory : 스탭을 생성하는 빌더

## Tasklet
- 알림메세지 전송과 같은 간단한 배치작업의 경우 **Tasklet** 을 구현한다.
- Tasklet 은 **RepeatStatus** 를 통해 작업의 완료여부를 반환할 수 있다.
  - FINISHED : 작업 완료
  - CONTINUABLE : 지속되어야 하는 상태

> 만약 CONTINUABLE 을 지속적으로 반환한다면 해당 스탭은 무한수행될 수 있음에 유의

## 잡 실행

```java
 ./mvnw clean pakcage
target/  java -jar Chapter02-0.0.1-SNAPSHOT.jar
```

```java
2021-09-17 22:32:33.532  INFO 49487 --- [           main] me.june.chapter02.Chapter02Application   : No active profile set, falling back to default profiles: default
2021-09-17 22:32:34.136  INFO 49487 --- [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Starting...
2021-09-17 22:32:34.323  INFO 49487 --- [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Start completed.
2021-09-17 22:32:34.458  INFO 49487 --- [           main] o.s.b.c.r.s.JobRepositoryFactoryBean     : No database type set, using meta data indicating: H2
2021-09-17 22:32:34.602  INFO 49487 --- [           main] o.s.b.c.l.support.SimpleJobLauncher      : No TaskExecutor has been set, defaulting to synchronous executor.
2021-09-17 22:32:34.685  INFO 49487 --- [           main] me.june.chapter02.Chapter02Application   : Started Chapter02Application in 1.491 seconds (JVM running for 1.871)
2021-09-17 22:32:34.687  INFO 49487 --- [           main] o.s.b.a.b.JobLauncherApplicationRunner   : Running default command line with: []
2021-09-17 22:32:34.739  INFO 49487 --- [           main] o.s.b.c.l.support.SimpleJobLauncher      : Job: [SimpleJob: [name=job]] launched with the following parameters: [{}]
2021-09-17 22:32:34.778  INFO 49487 --- [           main] o.s.batch.core.job.SimpleStepHandler     : Executing step: [step1]
Hello, World!!
2021-09-17 22:32:34.791  INFO 49487 --- [           main] o.s.batch.core.step.AbstractStep         : Step: [step1] executed in 12ms
2021-09-17 22:32:34.795  INFO 49487 --- [           main] o.s.b.c.l.support.SimpleJobLauncher      : Job: [SimpleJob: [name=job]] completed with the following parameters: [{}] and the following status: [COMPLETED] in 29ms
2021-09-17 22:32:34.799  INFO 49487 --- [ionShutdownHook] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Shutdown initiated...
2021-09-17 22:32:34.814  INFO 49487 --- [ionShutdownHook] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Shutdown completed.
```
- JobLauncher 에 의해 실행된 로그를 확인할 수 있다.
- **JobLauncherCommandLineRunner** 라는 컴포넌트가 있고, 이는 스프링 배치 실행시 로드되며 JobLauncher 를 통해 모든 잡을 실행한다.
  - Deprecated 됨
  - JobLauncherApplicationRunner 로 대체됨 (SpringBoot 2.3.0) / 2.6.0에서 제거될 예정

## 참고
- https://kwonnam.pe.kr/wiki/springframework/springboot/batch