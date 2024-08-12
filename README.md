# Bubble

## Introduction
This is my first project with gRPC (as opposed to REST) and my first project with a mobile frontend. You may use this
project as a template for your own learning.

Check [Overview](#Overview) for any tools you would like to see in action. Also check [Related Articles](#related-articles)
for further reading.

– Samuel

## Configuration

There are two main configuration files:

1. [src/main/resources/application.properties](src/main/resources/application.properties)

    Here, you can change the name of the application and which ports the application uses.

2. [.env](.env.stub)

    Currently, this file is used only for connecting to the database. Since `.env` is not included
    in version control, you will have to copy `.env.stub` into a new file named `.env` (i.e. `cp .env.stub .env`).
    `.env.stub` gives a proper configuration, given that you have a local Redis database running already 
    (see the next section).

Here is 
[a full list of configuration methods](https://docs.spring.io/spring-boot/docs/1.5.6.RELEASE/reference/html/boot-features-external-config.html)
from Spring's official documentation. 

## Running Locally

You can run a local database as follows (make sure you have Docker installed and that the daemon is running):

```
docker run -d --name redis-stack -p 6379:6379 -p 8001:8001 redis/redis-stack:latest
```

Please read the [official documentation](https://redis.io/docs/latest/operate/oss_and_stack/install/install-stack/docker/)
for Redis.
`localhost:6379` is the URI of the database, and `localhost:8001` (accessible by browser) is the URL of the database explorer.

If you are using a remote Redis database instead, you should still install Docker – the application tests have a 
dependency on Docker. Then, you can run the tests or the application itself with Gradle:

```
gradle test
gradle bootRun
```

The default port for the gRPC server is `9090`. You can test if the gRPC server is running by installing `grpcurl` and
asking for the list of services:

```
grpcurl -plaintext localhost:9090 list
```

## Development

To build this project, you must have Java 21, `protoc`, and `gradle` installed.
See issues at [ISSUES.md](ISSUES.md).

## Overview

### Backend

- **DI and Configuration:** Spring Framework
- **Database:** Redis
- **API Architecture:** gRPC
- **Serialization:** Protocol Buffers

### Testing and Telemetry

- **Testing:** JUnit 5 and Testcontainers
- **Logging:** Logback

### Frontend (Planned)

- **UI:** Swift

## Related Articles

### Spring Framework
- [Spring Framework Overview](https://docs.spring.io/spring-framework/reference/overview.html)
- [Java-based Container Configuration](https://docs.spring.io/spring-framework/reference/core/beans/java.html)
- [Spring Data](https://spring.io/projects/spring-data)
- [Stack Overflow](https://stackoverflow.com/questions/tagged/spring-boot)
