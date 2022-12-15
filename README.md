# Spring Batch

### 1 Getting Started

##### 1.1 Batch Processing Concepts

Batch jobs follow a typical pattern. They consist of reading from a fixed data set, like our time entries. And then processing each item in the data set until it reaches the end. The processing that occurs for each item varies for each batch job and the business operation it's meant to complete. It is common to execute batch jobs on a schedule. Allowing the necessary data to accumulate over a period of time between executions. Once we have the necessary data for the period, the job is triggered to perform the processing.

<p> 
What is Batch Processing:
    <li>Behind the scenes processes</li>
    <li>Run without user interaction</li>
    <li>Executes over a fixed data set</li>
    <li>Scheduled jobs correspond with business activities</li>
</p>
<p>
Use Cases:
    <li>Reporting:</li>
        <ul>Processing large datasets to calculate and distribute information</ul>
        <ul>Often time-based (monthly bank statements, quarterly financials)</ul>
        <ul>Requires a collection of data over the period</ul>
    <li>Information Exchange</li>
        <ul>Sending/receiving of data between systems</ul>
        <ul>Integration via file exchanges, database connections, and messaging</ul>
        <ul>Applies <b>extract</b>, <b>transform</b>, and <b>load</b> (ETL) pattern</ul>
</p>

##### 1.2 Spring Batch Overview

<p>
Spring Batch is a lightweight framework for building batch applications that perform batch processing

![img.png](img/img1.png)
![img.png](img/img2.png)

Key features
<li>State Management — the framework stores metadata regarding jobs and their executions out of the box. During execution of a job, Spring Batch writes metadata to a job repository at various points in time, we don't even need to write any code to make this happen. This is very helpful when determining what jobs have executed, why a job has failed, and is used to support additional batch functionality like restarts.</li>
<li>Restartability — the framework can restart failed jobs at the appropriate step. In the event a job does fail Spring Batch provides the capability to restart the job from where we left off based upon information about the job in the job repository. So if we execute this job and processing fails when reading the second chuck of data in step two, the entire job will be marked as failed. So we see step two marked as failed, and then our job ultimately has failed. We can then use the framework to relaunch the job. And the job will begin to execute, skipping those steps that have executed successfully and starting at our failed step, in this case step two. Step two will not process the first chunk of data, it's going to restart at the second chuck, and here we see that it completes successfully. At that point, both our step and job will be marked as successfully executed within the job repository.</li>

![img.png](img/img3.png)

<li>Readers and Writers — the framework provides out-of-the-box components to integrate with popular data sources. Most batch jobs need to read data from some data source, do some processing, then write the process data to another data source. Spring Batch provides the item reader and item writer interfaces to abstract this concept and provides out of the box implementations for consuming or writing data to popular data sources like flat files, relational databases, XML files, JSON files, and Kafka. So the framework handles a lot of the heavy lifting when it comes to reading and writing data in a batch job. For developers using Spring Batch it's just a matter of configuring the appropriate reader, or writer for your data store.</li>

![img.png](img/img4.png)

<li>Transaction Support — the framework provides transactional writers that can roll back in the event of an error. Spring Batch also provides support for transactions, primarily for the item writers found within the framework. When an item writer writes a chuck of data it occurs with in a transaction. So if the first record in the chunk is written and the second record fails, the entire chuck of data is rolled back. The architecture and features found within Spring Batch address the most common challenges developers face when building a batch job. There really isn't a good case for attempting to write your own batch application on the JVM with a custom architecture.</li>

![img.png](img/img5.png)

<p>Building application:</p>

![img.png](img/img6.png)

</p>

Spring Batch uses a job repository to capture metadata regarding the execution of a batch job. The job repository typically stores this metadata in a relational database that Spring Batch uses to make decisions about job executions. Whilst Spring Batch will create the table schema required to store the metadata, we'll need to supply the database where the schema can be created.

### 2 Building Batch Jobs

##### 2.1 Spring Batch Architecture

<p> 
A job represents the entire batch process that we want to execute. It defines one or more steps that execute in an order we commonly call a flow. 
A step is a phase in a batch job that defines how the actual processing will occur for that portion of the job. The processing logic within a step may read data from a data source, process it and then write it to another data source.
A job can contain multiple steps and the flow from one step to another can be dynamic. Meaning it can be conditional or occur in parallel.</br>
The entire job is launched using a JobLauncher, which may pass JobParameters to the job. 
As the job runs, metadata regarding the job is written to the job repository.  When a job launcher creates a job, it typically will pass the name of the job and some parameters. The combination of the Job Name and its parameters defines a new JobInstance which is created.
When we execute a JobInstance, we create a new JobExecution.</p>

Example: relaunch job with the same job name and JobParameters:
On the screen below it represents the same JobInstance 
But looks attention to JobExecution. For this example, it not the same JobExecution.
Because it is the second execution of a particular job instance.
![img.png](img/img8.png)

Example: Different Job Instances
Here we'll see the same job ran with different parameters, which creates a different JobInstance and a new execution of that JobInstance. So here we see in attempt three, we have the same job name but you'll notice that the JobParameter being passed is different. This means that we're going to have a different JobInstance and when that JobInstance is executed we're going to get a different JobExecution.
![img.png](img/img9.png)

As the steps execute within a job, there's a very similar concept that is applied. So each execution of a step is going to create a new StepExecution. The StepExecution is associated with a JobExecution and its possible to have multiple StepExecutions when a failure occurs in our processing and we need to restart a job.
![img.png](img/img10.png)
As they execute, metadata regarding JobInstance, JobExecutions and StepExecutions are all stored in the JobRepository. Here you see some of the tables the JobRepository uses to store this information in a relational database. Of particular interest is the status and exit code columns found within the execution tables. These columns hold information regarding the success or failure of a job.
![img.png](img/img11.png)

<p>The two types of steps found within Spring Batch. </p>
<li><b>Tasklet-Based Step:</b> It contains a single method on its interface named execute, that runs over and over until it gives signal to stop. Tasklets are typically used for things like setup logic, stored procedures or other custom logic that cannot be achieved without the box components.</li>
<li><b>Chunk-Based Step:</b> It is used in scenarios where we need to process data from a data source. The chunk-based step leverages the ItemReader interface to read chunks of data from a data store. Then writes the chunks in a transaction using the ItemWriter. Optionally, we can include an ItemProcessor implementation to perform transformations on the data.</li>

##### 2.2 Configuring a Job Repository
<p>
Spring batch uses the job repository to store metadata about the execution of a batch job.
</p>
<p>
Run PostgreSQL container using docker

```shell
docker run --name postgresql -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=job_repository -p 5432:5432 -d postgres
```

Provide Postgres dependencies into pom.xml

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

Provide Application Properties for Datasource:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/job_repository
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver
  jpa:
    show-sql: true
    open-in-view: false
  batch:
    jdbc:
      initialize-schema: always
      isolation-level-for-create: default
```

Check the output from the following tables:
```text
SELECT * FROM batch_job_instance;
SELECT * FROM batch_job_execution;
SELECT * FROM batch_step_execution;
```
</p>

##### 2.3 Job Parameters
<p>
Job instances are created using the name of the job and parameters passed by a job launcher. 
If a job instance has been successfully executed, <u>it's not possible to rerun that same job instance</u>. 
Instead, a new job instance must be created by passing a new job parameter to the job. 

```shell
maven clean package
```
```shell
java -jar .\target\spring-batch-demo-1.0.0.jar
```
So for example, when we've been running our job, we haven't been supplying any job parameters. 
Therefore, if we re-execute the same job, it will not be started by the framework, it will assume the job has been successfully completed, and it won't allow us to restart. 
So here you can see that it's signifying the step has already been completed, or it's not restartable. 
So if we want to relaunch this job, we need to do it with a new job parameter so that we get a new job instance. 
Here, we're also able to pass our job parameter. 
```shell
java -jar .\target\spring-batch-demo-1.0.0.jar "job_parameter_1=value" "job_parameter_2=value"
```
So if we look at our step, we're able to access job parameters by working with the chunkContext. 
So, what we're going to do is create a new string named item and then we're going to use the chunkContext and we're going to get the stepContext and then from there, we can access a map containing our job parameters. 
```java
@Bean
public Step packageItemStep() {
    return stepBuilderFactory
        .get("packageItemStep")
        .tasklet((stepContribution, chunkContext) -> {
            var item = chunkContext.getStepContext().getJobParameters().get("item").toString();
            var date = chunkContext.getStepContext().getJobParameters().get("run.date").toString();
            System.out.println(String.format("The %s has been packaged on %s.", item, date));
            return RepeatStatus.FINISHED;
        })
        .build();
}
```
So just like a typical map, we need a key.
In this case, it's the name of the job parameter. And then because the value of the map is of type object, we're going to take that object to a string in order to assign it to our items string.
And then we're going to declare a new job parameter that we're going to access.
And this is a very common job parameter, it's going to be the date that the job was ran.
```shell
java -jar .\target\spring-batch-demo-1.0.0.jar "item=shoes" "run.date(date)=2022/10/27"
```
So when we launched the job next time, we're going to specify a parameter run.date. And that's going to allow us to pass in the date the job was ran and then when we output this information within our tasklet to the console.
So we'll just need to then supply those two arguments, first, the name of the item, and then second, the date.
Now when the job executes, you'll see that we got a new job instance so we were able to, you know, rerun the job and then we were able to access the value of those job parameters. 
So job parameters are very important. When we are scheduling jobs, it's often necessary to pass in job parameters so that we're able to get that new job instance and we're able to execute the run for that particular instance of the job.
</p>

##### 2.4 Building Jobs with multiple steps
<p>
Batch jobs normally contain more than a single step.
The execution of most jobs flows sequentially from one step to another until the job completes.
When configuring a job with multiple steps, the job builder interface allows us to define <b>transitions</b> from one step to another to create a multi-step job.
</p>
See these transitions:

```java
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
            .get("deliverPackageJob")
            //transition one step to another step. These steps are executed in sequence.
            .start(packageItemStep())
            .next(driveToAddressStep())
            .next(givePackageToCustomerStep())
            .build();
}
```
Bat script:
```cmd
set CURRENT_DATE=%date:~10,6%%date:~6,4%%date:~4,2%
echo %CURRENT_DATE%
call mvn clean package -DskipTests
call java -jar ./target/spring-batch-demo-1.0.0.jar "item=shoes" "run.date(date)=%CURRENT_DATE%"
```

##### 2.5 Restarting Jobs
Unfortunately, not every batch job runs successfully the first time. Spring Batch allows failed jobs to be restarted. The framework only allows a job to be restarted if the overall status of a job execution is marked as `FAILED` or `STOPPED`.</br>
By default, it does not allow completed jobs to be restarted.</br>
When a job is restarted, Spring Batch will create a new `JobExecution` for the particular `JobInstance` that failed, and it will restart at the failed step, executing from that point forward.</br>
An Example: We're going to introduce some boolean logic within our drive to address step. Let's imagine that we can get lost while we are driving our package to the address for delivery. So, we'll just introduce a boolean, and we'll name that boolean `GOT_LOST = true`. 
```java
@Bean
public Step driveToAddressStep() {
    boolean GOT_LOST = true;
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
```
And then, within the execute method of our tasklet, I'm going to add a conditional that determines if we got lost, and if so, we're going to then throw a `RuntimeException`. So we just have a way to toggle the success or failure of our drive to address step. 
```shell
.\run.bat
```
You can see that we have a stack trace, within the job itself, and that's due to the run time exception that was thrown. So we did not successfully complete our job. 
```commandline
2022-12-15 12:15:55.162  INFO 15948 --- [           main] o.s.b.a.b.JobLauncherApplicationRunner   : Running default command line with: [item=potato, run.date(date)=2022/15/12]
2022-12-15 12:15:55.409  INFO 15948 --- [           main] o.s.b.c.l.support.SimpleJobLauncher      : Job: [SimpleJob: [name=deliverPackageJob]] launched with the following parameters: [{item=potato, r
un.date=1678568400000}]
2022-12-15 12:15:55.474  INFO 15948 --- [           main] o.s.batch.core.job.SimpleStepHandler     : Executing step: [packageItemStep]
The potato has been packaged on Sun Mar 12 00:00:00 MSK 2023.
2022-12-15 12:15:55.529  INFO 15948 --- [           main] o.s.batch.core.step.AbstractStep         : Step: [packageItemStep] executed in 54ms
2022-12-15 12:15:55.567  INFO 15948 --- [           main] o.s.batch.core.job.SimpleStepHandler     : Executing step: [driveToAddressStep]
2022-12-15 12:15:55.594 ERROR 15948 --- [           main] o.s.batch.core.step.AbstractStep         : Encountered an error executing step driveToAddressStep in job deliverPackageJob

java.lang.RuntimeException: Got lost driving to the address
        at com.github.uladzimirkalesny.springbatchdemo.SpringBatchDemoApplication.lambda$driveToAddressStep$1(SpringBatchDemoApplication.java:56) ~[classes!/:1.0.0]
        at org.springframework.batch.core.step.tasklet.TaskletStep$ChunkTransactionCallback.doInTransaction(TaskletStep.java:407) ~[spring-batch-core-4.3.7.jar!/:4.3.7]
        at org.springframework.batch.core.step.tasklet.TaskletStep$ChunkTransactionCallback.doInTransaction(TaskletStep.java:331) ~[spring-batch-core-4.3.7.jar!/:4.3.7]
        at org.springframework.transaction.support.TransactionTemplate.execute(TransactionTemplate.java:140) ~[spring-tx-5.3.23.jar!/:5.3.23]
        at org.springframework.batch.core.step.tasklet.TaskletStep$2.doInChunkContext(TaskletStep.java:273) ~[spring-batch-core-4.3.7.jar!/:4.3.7]
        at org.springframework.batch.core.scope.context.StepContextRepeatCallback.doInIteration(StepContextRepeatCallback.java:82) ~[spring-batch-core-4.3.7.jar!/:4.3.7]
        at org.springframework.batch.repeat.support.RepeatTemplate.getNextResult(RepeatTemplate.java:375) ~[spring-batch-infrastructure-4.3.7.jar!/:4.3.7]
        at org.springframework.batch.repeat.support.RepeatTemplate.executeInternal(RepeatTemplate.java:215) ~[spring-batch-infrastructure-4.3.7.jar!/:4.3.7]
        at org.springframework.batch.repeat.support.RepeatTemplate.iterate(RepeatTemplate.java:145) ~[spring-batch-infrastructure-4.3.7.jar!/:4.3.7]
        at org.springframework.batch.core.step.tasklet.TaskletStep.doExecute(TaskletStep.java:258) ~[spring-batch-core-4.3.7.jar!/:4.3.7]
        at org.springframework.batch.core.step.AbstractStep.execute(AbstractStep.java:208) ~[spring-batch-core-4.3.7.jar!/:4.3.7]
        at org.springframework.batch.core.job.SimpleStepHandler.handleStep(SimpleStepHandler.java:152) ~[spring-batch-core-4.3.7.jar!/:4.3.7]
        at org.springframework.batch.core.job.AbstractJob.handleStep(AbstractJob.java:413) ~[spring-batch-core-4.3.7.jar!/:4.3.7]
        at org.springframework.batch.core.job.SimpleJob.doExecute(SimpleJob.java:136) ~[spring-batch-core-4.3.7.jar!/:4.3.7]
        at org.springframework.batch.core.job.AbstractJob.execute(AbstractJob.java:320) ~[spring-batch-core-4.3.7.jar!/:4.3.7]
        at org.springframework.batch.core.launch.support.SimpleJobLauncher$1.run(SimpleJobLauncher.java:149) ~[spring-batch-core-4.3.7.jar!/:4.3.7]
        at org.springframework.core.task.SyncTaskExecutor.execute(SyncTaskExecutor.java:50) ~[spring-core-5.3.23.jar!/:5.3.23]
        at org.springframework.batch.core.launch.support.SimpleJobLauncher.run(SimpleJobLauncher.java:140) ~[spring-batch-core-4.3.7.jar!/:4.3.7]
        at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method) ~[na:na]
        at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77) ~[na:na]
        at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43) ~[na:na]
        at java.base/java.lang.reflect.Method.invoke(Method.java:568) ~[na:na]
        at org.springframework.aop.support.AopUtils.invokeJoinpointUsingReflection(AopUtils.java:344) ~[spring-aop-5.3.23.jar!/:5.3.23]
        at org.springframework.aop.framework.ReflectiveMethodInvocation.invokeJoinpoint(ReflectiveMethodInvocation.java:198) ~[spring-aop-5.3.23.jar!/:5.3.23]
        at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:163) ~[spring-aop-5.3.23.jar!/:5.3.23]
        at org.springframework.batch.core.configuration.annotation.SimpleBatchConfiguration$PassthruAdvice.invoke(SimpleBatchConfiguration.java:128) ~[spring-batch-core-4.3.7.jar!/:4.3.7]
        at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:186) ~[spring-aop-5.3.23.jar!/:5.3.23]
        at org.springframework.aop.framework.JdkDynamicAopProxy.invoke(JdkDynamicAopProxy.java:215) ~[spring-aop-5.3.23.jar!/:5.3.23]
        at jdk.proxy2/jdk.proxy2.$Proxy69.run(Unknown Source) ~[na:na]
        at org.springframework.boot.autoconfigure.batch.JobLauncherApplicationRunner.execute(JobLauncherApplicationRunner.java:199) ~[spring-boot-autoconfigure-2.7.5.jar!/:2.7.5]
        at org.springframework.boot.autoconfigure.batch.JobLauncherApplicationRunner.executeLocalJobs(JobLauncherApplicationRunner.java:173) ~[spring-boot-autoconfigure-2.7.5.jar!/:2.7.5]
        at org.springframework.boot.autoconfigure.batch.JobLauncherApplicationRunner.launchJobFromProperties(JobLauncherApplicationRunner.java:160) ~[spring-boot-autoconfigure-2.7.5.jar!/:2.7.5]      
        at org.springframework.boot.autoconfigure.batch.JobLauncherApplicationRunner.run(JobLauncherApplicationRunner.java:155) ~[spring-boot-autoconfigure-2.7.5.jar!/:2.7.5]
        at org.springframework.boot.autoconfigure.batch.JobLauncherApplicationRunner.run(JobLauncherApplicationRunner.java:150) ~[spring-boot-autoconfigure-2.7.5.jar!/:2.7.5]
        at org.springframework.boot.SpringApplication.callRunner(SpringApplication.java:762) ~[spring-boot-2.7.5.jar!/:2.7.5]
        at org.springframework.boot.SpringApplication.callRunners(SpringApplication.java:752) ~[spring-boot-2.7.5.jar!/:2.7.5]
        at org.springframework.boot.SpringApplication.run(SpringApplication.java:315) ~[spring-boot-2.7.5.jar!/:2.7.5]
        at org.springframework.boot.SpringApplication.run(SpringApplication.java:1306) ~[spring-boot-2.7.5.jar!/:2.7.5]
        at org.springframework.boot.SpringApplication.run(SpringApplication.java:1295) ~[spring-boot-2.7.5.jar!/:2.7.5]
        at com.github.uladzimirkalesny.springbatchdemo.SpringBatchDemoApplication.main(SpringBatchDemoApplication.java:87) ~[classes!/:1.0.0]
        at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method) ~[na:na]
        at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77) ~[na:na]
        at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43) ~[na:na]
        at java.base/java.lang.reflect.Method.invoke(Method.java:568) ~[na:na]
        at org.springframework.boot.loader.MainMethodRunner.run(MainMethodRunner.java:49) ~[spring-batch-demo-1.0.0.jar:1.0.0]
        at org.springframework.boot.loader.Launcher.launch(Launcher.java:108) ~[spring-batch-demo-1.0.0.jar:1.0.0]
        at org.springframework.boot.loader.Launcher.launch(Launcher.java:58) ~[spring-batch-demo-1.0.0.jar:1.0.0]
        at org.springframework.boot.loader.JarLauncher.main(JarLauncher.java:65) ~[spring-batch-demo-1.0.0.jar:1.0.0]

2022-12-15 12:15:55.618  INFO 15948 --- [           main] o.s.batch.core.step.AbstractStep         : Step: [driveToAddressStep] executed in 50ms
2022-12-15 12:15:55.650  INFO 15948 --- [           main] o.s.b.c.l.support.SimpleJobLauncher      : Job: [SimpleJob: [name=deliverPackageJob]] completed with the following parameters: [{item=potato, 
run.date=1678568400000}] and the following status: [FAILED] in 203ms
```
And if we navigate over to our job repository, we can query the job repository to see some information about what exactly happened. You can see that we have one job instance, we attempted one execution of that job instance, and it did fail.
```sql
SELECT * FROM batch_job_instance;
```
```
job_instance_id     version     job_name            job_key
4                   0           deliverPackageJob   a21ea2caf15ef34a0329b12585445dc9
```
```sql
SELECT * FROM batch_job_execution;
```
```
job_execution_id    version     job_instance_id     create_time                 start_time                  end_time                    status  exit_code   exit_message                                                    last_updated                job_configuration_location
4                   2           4                   2022-12-15 12:15:55.336000  2022-12-15 12:15:55.436000  2022-12-15 12:15:55.639000  FAILED  FAILED      java.lang.RuntimeException: Got lost driving to the address...  2022-12-15 12:15:55.639000  null
```
```sql
SELECT * FROM batch_step_execution;
```
```
step_execution_id   version     step_name           job_execution_id    start_time                  end_time                    status      commit_count  rollback_count  ...   exit_code   exit_message                                                    last_updated
8                   3           packageItemStep     4                   2022-12-15 12:15:55.475000  2022-12-15 12:15:55.529000  COMPLETED   1             0                     COMPLETED   ""                                                              2022-12-15 12:15:55.530000
9                   2           driveToAddressStep  4                   2022-12-15 12:15:55.568000  2022-12-15 12:15:55.618000  FAILED      0             1                     FAILED      java.lang.RuntimeException: Got lost driving to the address...  2022-12-15 12:15:55.619000
```
And then when we take a look at the step execution, you can see that we successfully packaged the item, but we failed in driving to the address.</br> 
And this time, we're just going to toggle the `GOT_LOST = false`, that way we succeed. So this is going to be the second run of this job instance, and then we can run the job again. So this time, it's going to be successful, as you see we're not going to execute the package items step again, because it successfully completed.
```sql
2022-12-15 13:46:20.988  INFO 33012 --- [           main] o.s.b.c.l.support.SimpleJobLauncher      : Job: [SimpleJob: [name=deliverPackageJob]] launched with the following parameters: [{item=potato, r
un.date=1678568400000}]
2022-12-15 13:46:21.050  INFO 33012 --- [           main] o.s.batch.core.job.SimpleStepHandler     : Step already complete or not restartable, so no action to execute: StepExecution: id=8, version=3, 
name=packageItemStep, status=COMPLETED, exitStatus=COMPLETED, readCount=0, filterCount=0, writeCount=0 readSkipCount=0, writeSkipCount=0, processSkipCount=0, commitCount=1, rollbackCount=0, exitDescri
ption=
2022-12-15 13:46:21.078  INFO 33012 --- [           main] o.s.batch.core.job.SimpleStepHandler     : Executing step: [driveToAddressStep]
Successfully arrived to address.
2022-12-15 13:46:21.126  INFO 33012 --- [           main] o.s.batch.core.step.AbstractStep         : Step: [driveToAddressStep] executed in 47ms
2022-12-15 13:46:21.163  INFO 33012 --- [           main] o.s.batch.core.job.SimpleStepHandler     : Executing step: [givePackageToCustomerStep]
Given the package to the customer.
2022-12-15 13:46:21.202  INFO 33012 --- [           main] o.s.batch.core.step.AbstractStep         : Step: [givePackageToCustomerStep] executed in 39ms
2022-12-15 13:46:21.241  INFO 33012 --- [           main] o.s.b.c.l.support.SimpleJobLauncher      : Job: [SimpleJob: [name=deliverPackageJob]] completed with the following parameters: [{item=potato, 
run.date=1678568400000}] and the following status: [COMPLETED] in 208ms

```
From the output to the console you can see that it's not going to execute the package item step, but we do execute the drive to address step, because it previously had failed, and when we relaunch the job, we would like to complete it successfully.</br>
From job repository, we can go ahead and query the database, and you'll now see that we still only have one job instance, because we used the same parameters.
```sql
SELECT * FROM batch_job_instance;
```
```
job_instance_id     version     job_name            job_key
4                   0           deliverPackageJob   a21ea2caf15ef34a0329b12585445dc9
```
We have a second job execution, and that job execution completed successfully.
```sql
SELECT * FROM batch_job_execution;
```
```
job_execution_id    version     job_instance_id     create_time                 start_time                  end_time                    status      exit_code   exit_message                                                    last_updated                job_configuration_location
4                   2           4                   2022-12-15 12:15:55.336000  2022-12-15 12:15:55.436000  2022-12-15 12:15:55.639000  FAILED      FAILED      java.lang.RuntimeException: Got lost driving to the address...  2022-12-15 12:15:55.639000  null
5                   2           4                   2022-12-15 13:46:20.955000  2022-12-15 13:46:21.020000  2022-12-15 13:46:21.228000  COMPLETED   COMPLETED   ""                                                              2022-12-15 13:46:21.228000  null
```
Then take a look at our step execution, see for the second job execution, we executed the drive to address step and the give package to customer step. Both of those steps completed successfully.
```sql
SELECT * FROM batch_step_execution;
```
```
step_execution_id   version     step_name                   job_execution_id    start_time                  end_time                    status      commit_count  rollback_count  ...   exit_code   exit_message                                                    last_updated
8                   3           packageItemStep             4                   2022-12-15 12:15:55.475000  2022-12-15 12:15:55.529000  COMPLETED   1             0                     COMPLETED   ""                                                              2022-12-15 12:15:55.530000
9                   2           driveToAddressStep          4                   2022-12-15 12:15:55.568000  2022-12-15 12:15:55.618000  FAILED      0             1                     FAILED      java.lang.RuntimeException: Got lost driving to the address...  2022-12-15 12:15:55.619000
10                  3           driveToAddressStep          5                   2022-12-15 13:46:21.079000  2022-12-15 13:46:21.126000  COMPLETED   1             0                     COMPLETED   ""                                                              2022-12-15 13:46:21.127000
11                  3           givePackageToCustomerStep   5                   2022-12-15 13:46:21.163000  2022-12-15 13:46:21.202000  COMPLETED   1             0                     COMPLETED   ""                                                              2022-12-15 13:46:21.204000
```
It is important to understand that jobs and steps have specific statuses that are assigned based upon the results of a job execution.</br>
Steps have an exit status that affects the overall batch status. These statuses ultimately decide if a job can be restarted, and where the job is restarted from.
##### 2.5 Jobs Flow
The majority of the work required to create a spring batch job, is defining how jobs move from one step to another step.</br> 
This concept is known as <b>flow</b>, and it is important to understand in order to address batch processing requirements.</br>
In simple jobs, execution flows sequentially from one step to another, as we have seen so far in our delivery job.</br>
![img12.png](img%2Fimg12.png)
We simply execute one step then the next step, repeating this pattern until the job completes. <b>Sequential execution</b> is achieved using the <b>next</b> transition within our job configuration. This transition allows us to specify the next step to execute upon the successful completion of a step.</br>
More complex jobs may require a <b>conditional</b> flow where the flow of the job may execute a different step, depending upon the result of a previous step.
![img13.png](img%2Fimg13.png)
Here we see a segment of a job that is configured to execute Step3 ,if Step1 is successful.</br>
If Step1 is not successful, we'll execute Step2.</br>
To support this, Spring Batch provides the transitions <b><i>on</i></b>, <b><i>to</i></b>, and <b><i>from</i></b>.</br>
These transitions can be used to conditionally define the flow from one step to another.</br>
The most <b>important</b> of these is the <b>on</b> transition, it allows us to specify a pattern that if matched by the exit status of a step, will cause the job execution to proceed to the next step specified within the step configuration.</br>
On example above after Step1, we sue the on method to check the pattern of the exit status and see if it matches `failed`. If it does, we move to Step2, otherwise, we'll go from Step1, take the exit status, and compare it to the special pattern, asterisks. This pattern basically is a catch-all, so anything aside from failed will cause our job to move to Step3. So as we watch this job execute, we see Step1 is successful, and because Step1 is successful, that will cause us to match the asterisks pattern specified in the on transition. And that means that Step3 will be executed next.</br>
If we look at the inverse, let's say Step1 were to fail, that's going to cause our job flow to transition to Step2, because we're going to return `failed` as an exit status, and then that will cause JobExecution to proceed to Step2.</br> 
So it's all about that pattern specified in the on transition and then the next step specified in the to following on. 
The statuses are very important. When a batch job executes, Spring Batch captures `two statuses for the JobExecution and the StepExecution`.</br>
The statuses are slightly similar and the differences between them is a little confusing.</br>
![img14.png](img%2Fimg14.png)
The first status is the `Batch Status`. This status represents the overall status for the job or step execution. It is limited to an enumerated set of values.</br>
`Batch Statuses` are fixed, they're specified in an enum, so the ones you see on the left are a fixed set of values and we cannot specify a custom BatchStatus. List of BatchStatuses: `ABANDONED, COMPLETED, FAILED, STARTED, STOPPED.`</br>
The second status is the `Exit Status` and when we're talking about conditional job flow, this is the important one. It represents a literal status that is returned from a JobExecution or a StepExecution. The Exit Status is the one that is used by our on transition for the pattern match. So when performing a conditional flow, the Exit Status will be consulted for that pattern match, which will ultimately determine the flow the job will take. 
The Exit Status, on the other hand, uses literal values and we can define our own custom ExitStatuses. This will be important when we look at conditional flows moving forward. List of ExitStatuses: `COMPLETED, EXECUTING, FAILED, STOPPED`, <b>Custom Status</b>

##### Controlling flow with custom statuses[JobExecutionDecider]
```shell
git checkout 2.8-controlling-flow-with-custom-status
```
When more granular control over the conditional flow of the batch job is required developers can implement a `JobExecutionDecider`.</br>
`JobExecutionDecider` provides better of the job flow by ExitStatus of the Step to be modified into a Custom Exit Status.</br>
It's very useful when standard Exit Statuses not suffice.</br>
Custom Status is required to determine where the job flow should proceed.
JobExecutionDecider helps us to determine when this step needs to be executed. (for example, customer not at home)
```java
public class DeliveryDecider implements JobExecutionDecider {
    @Override
    public FlowExecutionStatus decide(JobExecution jobExecution, StepExecution stepExecution) {
        var result = LocalDateTime.now().getHour() < 12 ? "PRESENT" : "NOT_PRESENT";
        System.out.println("Decider result is: " + result);
        return new FlowExecutionStatus(result);
    }
}
```
```java
@Bean 
public JobExecutionDecider decider() {
    return new DeliveryDecider();
}
```
```java
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
            .on("*").to(decider())
                .on("PRESENT").to(givePackageToCustomerStep())
            .from(decider())
                .on("NOT_PRESENT").to(leaveAtDoorStep())
        .end()
        .build();
}
```