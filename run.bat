set CURRENT_DATE=%date:~10,6%%date:~6,4%%date:~4,2%
echo %CURRENT_DATE%
call mvn clean package -DskipTests
call java -jar ./target/spring-batch-demo-1.0.0.jar "item=shoes" "run.date(date)=%CURRENT_DATE%"
