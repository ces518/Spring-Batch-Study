package me.june.chapter04;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;

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
