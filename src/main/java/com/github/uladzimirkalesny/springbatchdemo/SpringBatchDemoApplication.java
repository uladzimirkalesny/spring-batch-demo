package com.github.uladzimirkalesny.springbatchdemo;

import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.time.LocalDateTime;

@EnableBatchProcessing
@SpringBootApplication
public class SpringBatchDemoApplication extends QuartzJobBean {

    private final JobBuilderFactory jobBuilderFactory;
    // provided by Spring Batch
    private final JobExplorer jobExplorer;
    private final JobLauncher jobLauncher;
    private final StepBuilderFactory stepBuilderFactory;

    public SpringBatchDemoApplication(JobBuilderFactory jobBuilderFactory,
                                      JobExplorer jobExplorer,
                                      JobLauncher jobLauncher,
                                      StepBuilderFactory stepBuilderFactory) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.jobExplorer = jobExplorer;
        this.jobLauncher = jobLauncher;
        this.stepBuilderFactory = stepBuilderFactory;
    }

    /**
     * This is where we're going to launch our job
     */
    @Override
    protected void executeInternal(JobExecutionContext context) {
        JobParameters jobParameters = new JobParametersBuilder(jobExplorer)
                .getNextJobParameters(job()) // this is going to cause the job parameters to auto-increment
                .toJobParameters();

        try {
            // using the jobLauncher we can then go ahead and run our job with the parameters that we've specified.
            this.jobLauncher.run(job(), jobParameters);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Quartz dependency
    @Bean
    public JobDetail jobDetail() {
        return JobBuilder.newJob(SpringBatchDemoApplication.class) // class that contains our job
                .storeDurably() // this will cause information regarding our job to be retained
                .build();
    }

    // Quartz dependency
    // This is going to determine when we execute our job
    @Bean
    public Trigger trigger() {
        // build schedule
        SimpleScheduleBuilder simpleScheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInSeconds(30) // interval
                .repeatForever();

        // trigger that is based off of a schedule
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail()) // hat's going to cause this trigger to apply to the job we've defined within our jobDetail method
                .withSchedule(simpleScheduleBuilder) // specify our schedule
                .build();
    }

    @Bean
    public Step step() {
        return this.stepBuilderFactory
                .get("step")
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("The run time is: " + LocalDateTime.now());
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    @Bean
    public Job job() {
        return this.jobBuilderFactory.get("job")
                // We're going to be executing this job on a schedule we need to be able to automatically increment the job parameters.
                // That way we get separate job instances with every run
                .incrementer(new RunIdIncrementer())
                .start(step())
                .build();
    }

    public static void main(String[] args) {
        SpringApplication.run(SpringBatchDemoApplication.class, args);
    }

}
