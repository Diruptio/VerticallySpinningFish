# Vertically Spinning Fish

A container management application built with Java.

## Project Structure

This project is organized as a multi-module Gradle build:

### Modules

- **`common/`** - Shared data models and API interfaces
- **`api/`** - Client library for external consumers 
- **`velocity-plugin/`** - Velocity server plugin
- **Root module** - Main application and server

### Build Configuration

- **`buildSrc/`** - Gradle convention plugins for shared build logic
- **`gradle/libs.versions.toml`** - Centralized dependency version management
- **`gradle/docker.gradle.kts`** - Docker-related build tasks

## Building

```bash
./gradlew build
```

## Running

```bash
./gradlew run
```

This will start the application in a Docker container.

## Project Layout

```
├── api/                    # Client API library
├── common/                 # Shared models and interfaces
├── velocity-plugin/        # Velocity plugin
├── src/                    # Main application sources
├── buildSrc/              # Build convention plugins
├── gradle/                # Gradle configuration
└── build.gradle.kts       # Root build configuration
```

## Development

The project uses Java 21 and follows modern Gradle conventions with:

- Version catalogs for dependency management
- Convention plugins for shared build logic
- Clear module separation and dependencies
- Docker integration for deployment