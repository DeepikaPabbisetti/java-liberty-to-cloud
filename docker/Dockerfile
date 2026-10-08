# Stage 1: Build the application into artifact
# --------------------------------
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

COPY code/pom.xml .
RUN mvn -B -q dependency:go-offline

COPY code/src ./src
RUN mvn -B -q clean package -DskipTests

# Stage 2: Run the application
# --------------------------------
FROM icr.io/appcafe/websphere-liberty:24.0.0.3-full-java17-openj9-ubi

COPY --chown=1001:0 docker/server.xml /config/server.xml
COPY --chown=1001:0 docker/postgres/ /opt/ibm/wlp/usr/shared/resources/postgres/
COPY --chown=1001:0 --from=builder /build/target/order-management-*.jar /config/apps/order-management.jar

USER root
RUN mkdir -p /logs /shared/logs && chown 1001:0 /logs /shared/logs && chmod g+rwx /logs /shared/logs
USER 1001

# Spring looks the datasource up by this JNDI name (defined in server.xml)
ENV SPRING_DATASOURCE_JNDI_NAME=jdbc/orderManagementDS

EXPOSE 9080 9443
