package com.github.uladzimirkalesny.springbatchdemo;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;

@EnableBatchProcessing
@SpringBootApplication
public class SpringBatchDemoApplication {

    private static final String[] CSV_COLUMN_NAMES = {
            "order_id", "first_name", "last_name", "email", "cost", "item_id", "item_name", "ship_date"
    };

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    public SpringBatchDemoApplication(JobBuilderFactory jobBuilderFactory,
                                      StepBuilderFactory stepBuilderFactory) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
    }

    @Bean
    public ItemReader<Order> itemReader() {
        DelimitedLineTokenizer delimitedLineTokenizer = new DelimitedLineTokenizer(); // "," comma as delimiter
        delimitedLineTokenizer.setNames(CSV_COLUMN_NAMES); // headers from csv file

        DefaultLineMapper<Order> defaultLineMapper = new DefaultLineMapper<>();
        defaultLineMapper.setLineTokenizer(delimitedLineTokenizer);
        defaultLineMapper.setFieldSetMapper(new OrderFieldSetMapper());

        FlatFileItemReader<Order> flatFileItemReader = new FlatFileItemReader<>();
        flatFileItemReader.setLinesToSkip(1); // skip headers
        flatFileItemReader.setResource(new ClassPathResource("orders.csv"));
        flatFileItemReader.setLineMapper(defaultLineMapper);

        return flatFileItemReader;
    }

    @Bean
    public Step readFlatFileStep() {
        return this.stepBuilderFactory.get("chunkBasedStep")
                .<Order, Order>chunk(2)
                .reader(itemReader())
                .writer(items -> {
                    System.out.printf("Received list of size %d%n", items.size());
                    items.forEach(System.out::println);
                })
                .build();
    }

    @Bean
    public Job job() {
        return this.jobBuilderFactory.get("job")
                .start(readFlatFileStep())
                .build();
    }

    public static void main(String[] args) {
        SpringApplication.run(SpringBatchDemoApplication.class, args);
    }

}
