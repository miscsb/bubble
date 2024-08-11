# Bubble

## Introduction
At first, I started this project to learn for myself, but I figured that it could be a good template for someone else to
learn how to make an app with a Java backend. It's a great language. This is my first project with gRPC (as opposed to REST) 
and my first project with a mobile frontend.

Check [Overview](#Overview) for any tools you would like to see in action.

– Samuel

## Configuration

There are two main configuration files:

1. [src/main/resources/application.properties](src/main/resources/application.properties)

    Here, you can change the name of the application and which ports the application uses.

2. [.env](.env)

    Currently, this file is used only for connecting to the database. Since `.env` is not included
    in version control, you will have to copy `.env.stub` into a new file named `.env` (i.e. `cp .env.stub .env`).
    `.env.stub` gives a proper configuration, given that you have a local Redis database running already 
    (see the next section).

## Running With a Local Database

You can run a local database as follows (make sure you have docker installed and that the daemon is running):

```
docker run -d --name redis-stack -p 6379:6379 -p 8001:8001 redis/redis-stack:latest
```

Please read the [official documentation](https://redis.io/docs/latest/operate/oss_and_stack/install/install-stack/docker/).
for Redis.

`localhost:6379` is the URI of the database, and `localhost:8001` (accessible by browser) is the URL of the database explorer.

Then, you can run the tests with Gradle:

```
gradle test
```

## Development

To build this project, you must have Java 21, `protoc`, and `gradle` installed.

## Overview

### Backend

- **DI and Configuration:** Spring Boot
- **Database:** Redis
- **API Architecture:** gRPC
- **Serialization:** Protocol Buffers

### Testing and Telemetry

- **Testing:** JUnit 5 and Testcontainers
- **Logging:** Logback

### Frontend (Planned)

- **UI:** Swift

See issues at [ISSUES.md](ISSUES.md).
