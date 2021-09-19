package me.june.chapter04.systemcommand;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.step.tasklet.SimpleSystemProcessExitCodeMapper;
import org.springframework.batch.core.step.tasklet.SystemCommandTasklet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

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
