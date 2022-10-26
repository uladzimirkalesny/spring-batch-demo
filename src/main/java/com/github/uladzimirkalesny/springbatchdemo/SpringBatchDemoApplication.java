package com.github.uladzimirkalesny.springbatchdemo;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * The @EnableBatchProcessing annotation adds autoconfiguration for Spring Batch to an application and automatically creates beans for a
 * JobRepository, JobLauncher, JobRegistry, PlatformTransactionManager, JobBuilderFactory and StepBuilderFactory.
 * <p>
 * HTTP Request processing is not a supported feature of Spring Batch and is typically addressed in web applications.
 * <p>
 * Within Spring Boot there is a JobLauncher that just kicks off and starts a job.
 */
@EnableBatchProcessing
@SpringBootApplication
public class SpringBatchDemoApplication {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    public SpringBatchDemoApplication(JobBuilderFactory jobBuilderFactory,
                                      StepBuilderFactory stepBuilderFactory) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
    }

    @Bean
    public Step packageItemStep() {
        return stepBuilderFactory
                .get("packageItemStep") // specify the name of the step
                // a tasklet is a particular type of step that has one method in interface 'execute'
                // and that method will get called over and over again until the tasklet signals that it has been completed.
                .tasklet((stepContribution, chunkContext) -> {
                    var item = chunkContext.getStepContext().getJobParameters().get("item").toString();
                    var date = chunkContext.getStepContext().getJobParameters().get("run.date").toString();
                    System.out.println(String.format("The %s has been packaged on %s.", item, date));
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    @Bean
    public Step driveToAddressStep() {
        return stepBuilderFactory
                .get("driveToAddressStep")
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("Successfully arrived to address.");
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    @Bean
    public Step givePackageToCustomerStep() {
        return stepBuilderFactory
                .get("givePackageToCustomerStep")
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("Given the package to the customer.");
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    @Bean
    public Job deliverPackageJob() {
        return jobBuilderFactory
                .get("deliverPackageJob") // specify the name of our job
                // transition one step to another step. These steps are executed in sequence.
                .start(packageItemStep())
                .next(driveToAddressStep())
                .next(givePackageToCustomerStep())
                .build();
    }

    public static void main(String[] args) {
        SpringApplication.run(SpringBatchDemoApplication.class, args);
    }

}
