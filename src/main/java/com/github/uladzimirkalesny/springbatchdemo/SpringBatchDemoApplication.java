package com.github.uladzimirkalesny.springbatchdemo;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;

@EnableBatchProcessing
@SpringBootApplication
public class SpringBatchDemoApplication {

    /**
     * If we do not specify an ORDER BY clause, the database does not guarantee the order the data will be provided.
     * This can cause issues if we do need to restart our job with Spring Batch.
     * So you want to be sure always to specify an order by clause within your SQL statements.
     */
    private static final String ORDER_SQL = "SELECT * FROM orders ORDER BY order_id";

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final DataSource dataSource;

    public SpringBatchDemoApplication(JobBuilderFactory jobBuilderFactory,
                                      StepBuilderFactory stepBuilderFactory,
                                      DataSource dataSource) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
        this.dataSource = dataSource;
    }

    @Bean
    public ItemReader<Order> itemReader() {
        return new JdbcCursorItemReaderBuilder<Order>()
                .name("jdbcCursorItemReader")
                .dataSource(dataSource)
                .sql(ORDER_SQL)
                .rowMapper(orderRowMapper())
                .build();
    }

    private RowMapper<Order> orderRowMapper() {
        return (rs, rowNum) -> {
            Order order = new Order();
            order.setOrderId(rs.getLong("order_id"));
            order.setFirstName(rs.getString("first_name"));
            order.setLastName(rs.getString("last_name"));
            order.setEmail(rs.getString("email"));
            order.setCost(rs.getBigDecimal("cost"));
            order.setItemId(rs.getString("item_id"));
            order.setItemName(rs.getString("item_name"));
            order.setShipDate(rs.getDate("ship_date"));

            return order;
        };
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
