# syntax=docker/dockerfile:1
# ---------- 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build

# HMoneta-Official-Plugin-Api 未发布到公共仓库，先注入镜像内本地仓库
COPY docker/libs/HMoneta-Official-Plugin-Api-0.1.0.jar \
     docker/libs/HMoneta-Official-Plugin-Api-0.1.0.pom \
     /root/.m2/repository/fan/summer/HMoneta-Official-Plugin-Api/0.1.0/

# 先复制 pom 拉取依赖，利用层缓存加速重复构建
COPY pom.xml ./
RUN mvn -q -B dependency:go-offline || true

COPY src ./src
RUN mvn -q -B package -DskipTests

# ---------- 运行阶段 ----------
FROM eclipse-temurin:25-jre AS runtime
ENV TZ=Asia/Shanghai \
    LOG_PATH=/app/logs
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends tzdata \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p /app/plugins /app/certs /app/logs \
    && chown -R 1000:1000 /app

COPY --from=build --chown=1000:1000 /build/target/*.jar /app/app.jar

# 复用基础镜像自带的 ubuntu 用户（uid/gid=1000，与 Linux 普通用户默认 uid 一致），
# 宿主机以同 uid 创建 ./data 挂载目录即可直接写入
USER ubuntu
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
