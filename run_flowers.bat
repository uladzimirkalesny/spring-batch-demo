call mvn clean package -DskipTests
call java -jar "-Dspring.batch.job.names=prepareFlowersJob" ./target/spring-batch-demo-1.0.0.jar "type=roses"