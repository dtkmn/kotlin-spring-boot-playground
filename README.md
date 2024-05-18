# Kotlin Playground

Welcome to the Kotlin Playground repository! This project serves as a standard library used within all microservices developed using Kotlin. The repository contains various components and utilities that facilitate the development and maintenance of Kotlin-based microservices.

## Table of Contents

- [Overview](#overview)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [Usage](#usage)
- [Components](#components)
- [Contributing](#contributing)
- [License](#license)

## Overview

The Kotlin Playground repository includes a collection of utilities and components such as authentication managers, batch processing configurations, exception handling, logging, and more. These components are designed to be modular and reusable across different microservices.

## Project Structure


```plaintext
kotlin-playground-main/
├── src/
│   └── main/
│       └── kotlin/
│           └── playground/
│               ├── common/
│               │   ├── authserver/
│               │   ├── batch/
│               │   ├── exception/
│               │   ├── restclient/
│               ├── configuration/
│               ├── controller/
│               ├── entity/
│               ├── processor/
│               ├── publisher/
│               └── repository/
└── resources/
├── application.yml
├── items.csv
├── logback-spring.xml
└── avro/
```

## Getting Started

### Prerequisites

- Java 11 or higher
- Kotlin 1.4 or higher
- Maven or Gradle for dependency management

### Installation

Clone the repository to your local machine:

```bash
git clone https://github.com/dtkmn/kotlin-playground.git
cd kotlin-playground
```

## Configuration

The main configuration file is located at \`src/main/resources/application.yml\`. Update the configuration properties as needed for your environment.

## Usage

### Running the Application

To run the application, use the following command:

```bash
./gradlew bootRun
```

### Building the Application

To build the application, use the following command:

```bash
./gradlew build
```

### Testing the Application

To run the tests, use the following command:

```bash
./gradlew test
```

## Components

### Authentication

- \`MoxAuthenticationManagerResolver\`: Resolves the appropriate authentication manager based on the provided token.

### Batch Processing

- \`BatchConfiguration\`: Configures batch processing related beans and properties.
- \`BatchController\`: Handles batch job-related requests.

### Exception Handling

- Custom exceptions like \`DragonException\` and \`IdempotentInputConflict\`.

### Logging

- Configurable logging setup with \`logback-spring.xml\`.

### Controllers

- \`HtmlController\`: Provides basic endpoints for testing.

### Processors

- \`CustomerProfileSnapshotProcessor\`: Listens to Kafka topics and processes customer profile snapshots.

### Repositories

- \`CePiiRepository\`: Repository interface for entity operations.

## Contributing

We welcome contributions to the Kotlin Playground repository. Please follow the standard GitHub flow:

1. Fork the repository.
2. Create a feature branch.
3. Commit your changes.
4. Open a pull request.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
"""
