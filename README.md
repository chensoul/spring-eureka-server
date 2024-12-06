# spring-eureka-server

## How to run

Package the application using the maven command:

```bash
./mvnw package
```

Using Java 17+ or higher, run the application:

```bash
java -jar target/spring-eureka-server-0.0.1-SNAPSHOT.jar 
```

## Enable AOT

Install GraalVM JDK:

```bash
sdk install java 21.0.5-graal 
```

Run maven command to compile the native image:

```bash
./mvnw -Pnative native:compile
```

## Run with Docker

Create an image with [buildpack](https://buildpacks.io/).

```bash
brew install buildpacks/tap/pack

pack build spring-eureka-server:0.0.1 \
  --path ./spring-eureka-server-0.0.1-SNAPSHOT.jar \
  --builder paketobuildpacks/builder:tiny
```

> If you will be running the image on an ARM host (such as an Apple machine with an Apple chipset), you must use a
> different builder:
>
> ```bash
> pack build spring-eureka-server:0.0.1 \
> --path target/spring-eureka-server-0.0.1-SNAPSHOT.jar \
> --builder dashaun/builder:tiny
> ```

Or you can create an image using docker build.

```bash
docker build -t chensoul/spring-eureka-server:0.0.1 .
```

Start the container by running:

```bash
docker run -d -p 8761:8761 chensoul/spring-eureka-server:0.0.1
```

Alternatively, you can push the image to docker hub:

```bash
docker login
docker tag chensoul/spring-eureka-server:0.0.1 chensoul/spring-eureka-server:latest
docker push chensoul/spring-eureka-server:0.0.1
```