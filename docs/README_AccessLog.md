# Logging Access Request

---

- Setup logback.xml with armeria request logger
```
<appender name="ACCESS" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>access.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
        <!-- daily rollover -->
        <fileNamePattern>access.%d{yyyy-MM-dd}-%i.log</fileNamePattern>
        <!-- each file should be at most 1GB, keep 30 days worth of history, but at most 30GB -->
        <maxFileSize>1GB</maxFileSize>
        <maxHistory>30</maxHistory>
        <totalSizeCap>30GB</totalSizeCap>
    </rollingPolicy>
    <encoder>
        <pattern>%msg%n</pattern>
    </encoder>
</appender>
```
```
<logger name="com.linecorp.armeria.logging.access" level="INFO" additivity="false">
    <appender-ref ref="ACCESS"/>
</logger>
```

- Load log file using SQL and query data

```sql
select LOAD_FILE('/tmp/access.log');
```

```sql
select * from JSON_TABLE(CONCAT('[', REGEXP_REPLACE(LOAD_FILE('/tmp/access.log'), '\n{', ',\n{'), ']'), '$[*]'
    columns(
        timestamp datetime path '$.timestamp',
        requestId varchar(250) path '$.requestId',
        remoteIP varchar(250) path '$.remoteIP',
        status int(100) path '$.status',
        method varchar(250) path '$.method',
        path varchar(250) path '$.path',
        query varchar(250) path '$.query',
        scheme varchar(250) path '$.scheme',
        requestLength varchar(250) path '$.requestLength',
        headers varchar(500) path '$.headers',
        requestStartTimeMillis varchar(500) path '$.requestStartTimeMillis'
    )
) as t;```