package me.june.chapter04.chunk.basic;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import me.june.chapter04.chunk.LoggingStepStartStopListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.repeat.CompletionPolicy;
import org.springframework.batch.repeat.policy.CompositeCompletionPolicy;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.batch.repeat.policy.TimeoutTerminationPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

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
