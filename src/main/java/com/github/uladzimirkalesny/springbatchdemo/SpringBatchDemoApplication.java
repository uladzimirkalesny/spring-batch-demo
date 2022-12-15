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
    public Step storePackageStep() {
        return stepBuilderFactory
                .get("storePackageStep")
                .tasklet((stepContribution, chunkContext) -> {
                    System.out.println("Storing the package while the customer address is located.");
                    return RepeatStatus.FINISHED;
                })
                .build();
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
        boolean GOT_LOST = false;
        return stepBuilderFactory
                .get("driveToAddressStep")
                .tasklet((contribution, chunkContext) -> {
                    if (GOT_LOST) {
                        throw new RuntimeException("Got lost driving to the address");
                    }
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

    /**
     * Using the transition elements in Spring Batch:
     * <ul>
     *     <li>on</li>
     *     <li>to</li>
     *     <li>from</li>
     * </ul>
     * We were able to build a conditional job, that changed its flow dynamically depending, upon the exit status of subsequent steps.
     * This is an important feature of Spring Batch because it allows us to construct complex jobs, that can satisfy more complicated batch requirements.
     */
    @Bean
    public Job deliverPackageJob() {
        return jobBuilderFactory
                .get("deliverPackageJob")
                .start(packageItemStep())
                // conditional flow
                .next(driveToAddressStep())
                    .on("FAILED") // equals statement
                    .to(storePackageStep()) // then statement
                .from(driveToAddressStep())
                    .on("*")
                    .to(givePackageToCustomerStep())
                .end()
                .build();
    }

    public static void main(String[] args) {
        SpringApplication.run(SpringBatchDemoApplication.class, args);
    }

}
