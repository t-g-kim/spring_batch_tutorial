package com.example.springBatchTutorial.job.DbDataReadWrite;

import com.example.springBatchTutorial.domain.account.Account;
import com.example.springBatchTutorial.domain.account.AccountRepository;
import com.example.springBatchTutorial.domain.order.Orders;
import com.example.springBatchTutorial.domain.order.OrdersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.batch.item.data.builder.RepositoryItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;

import java.util.Arrays;
import java.util.Collections;

/**
 * desc: 주문 테이블 -> 정산 테이블 데이터 이관
 * run: --job.name=migrationJob
 */
@Configuration
@RequiredArgsConstructor
public class DBMigrationConfig {

    private final OrdersRepository ordersRepository;

    private final AccountRepository accountsRepository;
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job migrationJob(Step migrationStep) {
        return jobBuilderFactory.get("migrationJob")
                .incrementer(new RunIdIncrementer())
                .start(migrationStep)
                .build();
    }

    @JobScope
    @Bean
    public Step migrationStep(ItemReader orderReader, ItemProcessor orderProcessor, ItemWriter orderWriter) {
        return stepBuilderFactory.get("migrationStep")
                .<Orders, Account>chunk(5)
                .reader(orderReader)
//                .writer(new ItemWriter<Orders>() {
//                    @Override
//                    public void write(List<? extends Orders> items) throws Exception {
//                        items.forEach(System.out::println);
//                    }
//                })
                .processor(orderProcessor)
                .writer(orderWriter)
                .build();
    }

    @StepScope
    @Bean
    public RepositoryItemWriter<Account> orderWriter() {
        return new RepositoryItemWriterBuilder<Account>()
                .repository(accountsRepository)
                .methodName("save")
                .build();
    }

    @StepScope
    @Bean
    public ItemProcessor<Orders, Account> orderProcessor() {
        // 주문을 Account로 변환
        return new ItemProcessor<Orders, Account>() {
            @Override
            public Account process(Orders item) throws Exception {
                return new Account(item);
            }
        };
    }

    @StepScope
    @Bean
    public RepositoryItemReader<Orders> orderReader() {
        return new RepositoryItemReaderBuilder<Orders>()
                .name("orderReader")
                .repository(ordersRepository)
                .methodName("findAll")
                .pageSize(5)                // 읽어올 데이터 사이즈
                .arguments(Arrays.asList()) // 파라미터가 존재하면 넣어준다. (파라미터가 없어서 비어있음)
                .sorts(Collections.singletonMap("id", Sort.Direction.ASC))
                .build();
    }

    // ItemWriter를 이용해서 저장을 할 수도 있다.
//    @StepScope
//    @Bean
//    public ItemWriter<Account> orderWriter() {
//        return new ItemWriter<Account>() {
//            @Override
//            public void write(List<? extends Account> items) throws Exception {
//                items.forEach(item -> accountsRepository.save(item));
//            }
//        };
//    }
}
