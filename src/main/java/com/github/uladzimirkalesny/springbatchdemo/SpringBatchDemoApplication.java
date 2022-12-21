package com.github.uladzimirkalesny.springbatchdemo;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.ItemPreparedStatementSetter;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.sql.Date;

@EnableBatchProcessing
@SpringBootApplication
public class SpringBatchDemoApplication {

    public static String INSERT_ORDER_SQL =
            "INSERT INTO orders_output(order_id, first_name, last_name, email, item_id, item_name, cost, ship_date)" +
                    "VALUES(?, ?, ?, ?, ?, ?, ?, ?)";

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

    /**
     * <ul>
     * <li>queryProvider using in order to construct our SQL statement</li>
     * <li>pageSize(2) specify how many items are in a page (amount of items). It's important that our page size also matches our chunk size.</li>
     * </ul>
     */
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
    public ItemWriter<Order> jdbcBatchItemWriterBuilder() {
        return new JdbcBatchItemWriterBuilder<Order>()
                .dataSource(dataSource)
                .sql(INSERT_ORDER_SQL)
                .itemPreparedStatementSetter(fillPreparedStatement())
                .build();
    }

    private ItemPreparedStatementSetter<Order> fillPreparedStatement() {
        return (order, ps) -> {
            ps.setLong(1, order.getOrderId());
            ps.setString(2, order.getFirstName());
            ps.setString(3, order.getLastName());
            ps.setString(4, order.getEmail());
            ps.setString(5, order.getItemId());
            ps.setString(6, order.getItemName());
            ps.setBigDecimal(7, order.getCost());
            ps.setDate(8, new Date(order.getShipDate().getTime()));
        };
    }

    @Bean
    public Step readFlatFileStep() throws Exception {
        return this.stepBuilderFactory.get("readFlatFileStep")
                .<Order, Order>chunk(2)
                .reader(itemReader())
                .writer(jdbcBatchItemWriterBuilder())
                .build();
    }

    @Bean
    public Job job() throws Exception {
        return this.jobBuilderFactory.get("job")
                .start(readFlatFileStep())
                .build();
    }

    public static void main(String[] args) {
        SpringApplication.run(SpringBatchDemoApplication.class, args);
    }

}
