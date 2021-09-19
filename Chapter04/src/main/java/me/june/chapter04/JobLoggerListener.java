package me.june.chapter04;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.annotation.AfterJob;
import org.springframework.batch.core.annotation.BeforeJob;

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
