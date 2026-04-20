.PHONY: all build up down clean logs

# Default target
all: env build up

# Build Maven projects (producer & consumer)
build-mvn:
	cd producer && mvn clean package -DskipTests
	cd consumer && mvn clean package -DskipTests

# Build Docker images
build-docker:
	docker compose build

build: build-docker

# Start services
up:
	docker compose up -d

# Stop services
down:
	docker compose down

# Clean everything
clean:
	docker compose down -v
	cd producer && mvn clean
	cd consumer && mvn clean

# Logs
logs:
	docker compose logs -f

# .env file
env:
	cp .env.example .env