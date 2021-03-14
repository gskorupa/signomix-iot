# Signomix IoT

The Signomix IoT microservice is part of the Signomix IoT Platform. 
The microservice provides an API related to the management of IoT devices and their data.

> This is work in progress. Stay tuned. 

## Requirements

* Java 13
* Maven

## Quick start

### Building package

```
$ git clone https://github.com/gskorupa/signomix-iot.git myproject
$ cd myproject
$ mvn package
```

### Docker image

```
docker build -t REPOSITORY_NAME/signomix-iot:TAG .
docker push REPOSITORY_NAME/signomix-iot:TAG
```
