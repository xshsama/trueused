# syntax=docker/dockerfile:1.7

# 多阶段构建：第一阶段编译 Spring Boot 应用
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

COPY pom.xml .

COPY src/ src/

# 构建应用（跳过测试）
RUN --mount=type=cache,target=/root/.m2 mvn -B package -DskipTests

# 第二阶段：运行时镜像
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# 从构建阶段复制生成的 JAR 文件
COPY --from=builder /app/target/*.jar app.jar

# 暴露端口
EXPOSE 8081

# 启动应用
ENTRYPOINT ["java", "-jar", "app.jar"]
