package com.github.uladzimirkalesny.springbatchdemo;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.batch.item.json.JacksonJsonObjectMarshaller;
import org.springframework.batch.item.json.builder.JsonFileItemWriterBuilder;
import org.springframework.batch.item.support.builder.CompositeItemProcessorBuilder;
import org.springframework.batch.item.validator.BeanValidatingItemProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.UUID;

@EnableBatchProcessing
@SpringBootApplication
public class SpringBatchDemoApplication {

    public static String INSERT_ORDER_SQL =
            "INSERT INTO orders_output(order_id, first_name, last_name, email, item_id, item_name, cost, ship_date)" +
                    "VALUES(:orderId, :firstName, :lastName, :email, :itemId, :itemName, :cost, :shipDate)";

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
    public PagingQueryProvider queryProvider() throws Exception {
        SqlPagingQueryProviderFactoryBean factoryBean = new SqlPagingQueryProviderFactoryBean();
        factoryBean.setSelectClause("SELECT order_id, first_name, last_name, email, cost, item_id, item_name, ship_date");
        factoryBean.setFromClause("FROM orders");
        factoryBean.setSortKey("order_id");
        factoryBean.setDataSource(dataSource);

        return factoryBean.getObject();
    }

    @Bean
    public ItemReader<Order> itemReader() throws Exception {
        return new JdbcPagingItemReaderBuilder<Order>()
                .name("jdbcPagingItemReaderBuilder")
                .dataSource(dataSource)
                .queryProvider(queryProvider())
                .pageSize(2)
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
    public ItemWriter<TrackedOrder> jsonItemWriter() {
        return new JsonFileItemWriterBuilder<TrackedOrder>()
                .name("jsonItemWriter")
                .jsonObjectMarshaller(new JacksonJsonObjectMarshaller<>())
                .resource(new FileSystemResource("/Users/Uladzimir_Kalesny/Downloads/orders.json"))
                .build();
    }

    @Bean
    public ItemProcessor<Order, Order> orderValidatingItemProcessor() {
        BeanValidatingItemProcessor<Order> beanValidatingItemProcessor = new BeanValidatingItemProcessor<>();
        beanValidatingItemProcessor.setFilter(true);
        return beanValidatingItemProcessor;
    }

    @Bean
    public ItemProcessor<Order, TrackedOrder> trackedOrderItemProcessor() {
        return order -> {
            TrackedOrder trackedOrder = new TrackedOrder(order);
            trackedOrder.setTrackingNumber(UUID.randomUUID().toString());
            return trackedOrder;
        };
    }

    @Bean
    public ItemProcessor<TrackedOrder, TrackedOrder> freeShippingItemProcessor() {
        return trackedOrder -> {
            trackedOrder.setFreeShipping(trackedOrder.getCost().compareTo(BigDecimal.valueOf(2)) > 0);
            return trackedOrder.isFreeShipping() ? trackedOrder : null;
        };
    }

    @Bean
    public ItemProcessor<Order, TrackedOrder> compositeItemProcessor() {
        return new CompositeItemProcessorBuilder<Order, TrackedOrder>()
                .delegates(
                        orderValidatingItemProcessor(),
                        trackedOrderItemProcessor(),
                        freeShippingItemProcessor()
                )
                .build();
    }

    @Bean
    public Step chunkBasedStep() throws Exception {
        return this.stepBuilderFactory.get("readStep")
                .<Order, TrackedOrder>chunk(2)
                .reader(itemReader())
                .processor(compositeItemProcessor())
                .writer(jsonItemWriter())
                .build();
    }

    @Bean
    public Job job() throws Exception {
        return this.jobBuilderFactory.get("job")
                .start(chunkBasedStep())
                .build();
    }

    public static void main(String[] args) {
        SpringApplication.run(SpringBatchDemoApplication.class, args);
    }

}
