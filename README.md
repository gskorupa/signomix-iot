# Cricket bootstrap

This is a template of a microservice project based on the Cricket Microservices Framework version 2. 
To start building a new microservice clone the repository as a new one and follow the instructions below.

> This is work in progress. Stay tuned. 

## Requirements

* Java 13
* Maven

## Quick start

### Building

```
$ git clone https://github.com/gskorupa/cricket-bootstrap.git myproject
$ cd myproject
$ mvn package
```

### Running

```
$ java -jar target/service.jar -r
```


OpenAPI specification of the running service can be accessed at the service path `/api`:

```
$ curl "http://localhost:8080/api"
```

## How does it work



### Hello service
 
Files:

|File|Description|
|---|---|
|src/main/resources/settings.json| Service configuration file |
|src/main/java/myorg/myservice/Service.java | Service kernel |
|src/main/java/myorg/myservice/in/hello/HelloHttpAdapter.java | |
|src/main/java/myorg/myservice/events/HelloEvent.java| |
|src/main/java/myorg/myservice/events/UserEvent.java| |
|src/main/java/myorg/myservice/events/MyEvent.java| |
|src/main/java/myorg/myservice/out/MyWorker.java| |
|src/main/java/myorg/myservice/out/UserManager.java| |
