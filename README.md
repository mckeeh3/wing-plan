# wing-plan

A reactive microservice built with Akka that manages flight school student lesson scheduling. This application demonstrates how to build scalable, resilient systems using the actor model pattern.

## Overview

This application is a flight school management system that handles the scheduling and coordination of flight lessons between students and instructors. It leverages Akka's actor system to manage concurrent operations and state in a distributed environment.

### Key Features

- **Flight Lesson Scheduling**: Manages the scheduling of flight lessons between students and instructors
- **Reactive Architecture**: Built using Akka's actor model for responsive, resilient, and scalable operations
- **RESTful API**: Provides HTTP endpoints for interacting with the scheduling system
- **Event-Driven Design**: Uses event sourcing for maintaining system state and history

### Technical Architecture

The application is structured using Akka's best practices:

- Actor-based domain model for managing flight school entities
- HTTP interface using Akka HTTP
- Event sourcing for maintaining scheduling state
- Clustering support for scalability

## Getting Started

### Prerequisites

- Java 11 or higher
- Maven
- Docker (for containerization)

### Running Locally

1. Build the project:

  ```shell
  mvn compile
  ```

2. Start the service:

  ```shell
  mvn compile exec:java
  ```

3. Test

4. The service will be available at `http://localhost:8080`
