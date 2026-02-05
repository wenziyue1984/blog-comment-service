## 运行时镜像：你用 Java 8 就换成 eclipse-temurin:8-jre
#FROM eclipse-temurin:8-jre
#
## 容器内工作目录
#WORKDIR /app
#
## 时区（日志时间统一）
#ENV TZ=Asia/Shanghai
#
## 把打好的 jar 拷进去
## 依赖你上一步固定的 jar 名字
#COPY build/blog-comment-service.jar app.jar
#
## 对外暴露端口（只是声明，真正映射端口看 compose）
#EXPOSE 8083
#
## 启动参数你可以后面再加，比如内存、GC、spring profile
#ENTRYPOINT ["java","-jar","/app/app.jar"]
#
#
###########################
#
## 这行表示：我们以一个“已经装好 Java 运行环境(JRE)”的基础镜像为起点。
## 你现在的 Spring Boot 2.7.x 很常见是 Java 8/11/17 都能跑，但你实际用哪个要跟你项目一致。
## 如果你项目用的是 Java 8，就换成 eclipse-temurin:8-jre
## 如果你项目用的是 Java 11，就换成 eclipse-temurin:11-jre
## 我这里先用 17，后面你如果启动时报 class version 错误再改。
#FROM eclipse-temurin:8-jre
#
## 这行表示：容器里我们所有操作都在 /app 这个目录下进行（类似 cd /app）
#WORKDIR /app
#
## 这行表示：把你电脑上的 jar 拷贝到容器里，并改名叫 app.jar
## 左边是你电脑的路径，右边是容器内的路径
#COPY build/blog-comment-service.jar /app/app.jar
#
## 这两行是“默认环境变量”，意思是容器启动时如果你不传参数，就用这些默认值
## JAVA_OPTS 用来放 JVM 参数（内存、系统属性等）
#ENV JAVA_OPTS=""
## 默认用 dev 环境启动（你也可以改成 prod）
#ENV SPRING_PROFILES_ACTIVE=dev
#
## 这行只是告诉别人：这个容器会用到 8080 端口（不等于自动映射端口）
#EXPOSE 8080
#
## 这段是容器启动时执行的命令
## - sh -c 是为了让 JAVA_OPTS 这种字符串能展开
## - java $JAVA_OPTS -jar /app/app.jar 就是运行 Spring Boot jar
## - --spring.profiles.active=... 指定当前启用的 Spring profile
#ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar --spring.profiles.active=$SPRING_PROFILES_ACTIVE"]