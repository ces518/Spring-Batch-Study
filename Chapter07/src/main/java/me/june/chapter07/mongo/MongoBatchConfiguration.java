package me.june.chapter07.mongo;

import java.util.Collections;
import java.util.Map;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.MongoItemReader;
import org.springframework.batch.item.data.builder.MongoItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoOperations;

@EnableBatchProcessing
@SpringBootApplication
public class MongoBatchConfiguration {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    /**
     * name : 잡 재시작이 가능하도록 ExeuctionContext 상태저장시 사용 storeState 가 true 일 경우 필요
     * targetType : 반환되는 문서를 역직렬화 할 클래스
     * jsonQuery : 잡 파라미터로 전달된 값과 동일한 해시태그를 모두 찾는 쿼리
     * collection : 쿼리 대상 컬렉션
     * parameterValues : 쿼리에 필요한 모든 파라미터 값
     * sorts : 정렬 기준 필드와 정렬 방법 MongoItemReader 는 페이지 기반이므로 반드시 정렬되어야한다.
     * template : 쿼리 실행대상 MongoOperations 구현체
     */
    @StepScope
    @Bean
    public MongoItemReader<Map> tweetsItemReader(
        MongoOperations mongoTemplate,
        @Value("#{jobParameters['hashTag']}") String hashTag
    ) {
        return new MongoItemReaderBuilder<Map>()
            .name("tweetsItemReader")
            .targetType(Map.class)
            .jsonQuery("{ \"entities.hashtags.text\" : { $eq: ?0 }}")
            .collection("tweets_collection")
            .parameterValues(Collections.singletonList(hashTag))
            .pageSize(10)
            .sorts(Collections.singletonMap("created_at", Direction.ASC))
            .template(mongoTemplate)
            .build();
    }

    @Bean
    public Step copyFileStep() {
        return this.stepBuilderFactory.get("copyFileStep")
            .<Map, Map>chunk(10)
            .reader(tweetsItemReader(null, null))
            .writer(itemWriter())
            .build();
    }

    @Bean
    public ItemWriter itemWriter() {
        return items -> items.forEach(System.out::println);
    }

    @Bean
    public Job job() {
        return this.jobBuilderFactory.get("mongoJob")
            .start(copyFileStep())
            .build();
    }

    public static void main(String[] args) {
        SpringApplication.run(MongoBatchConfiguration.class, "hashTag=nodejs");
    }
}
