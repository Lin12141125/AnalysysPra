# Week 6 用户管理系统

基于 Spring Boot 3.2、Spring Security、JWT、MyBatis-Plus、MySQL、Redis、Knife4j 的用户管理项目。本周在第 5 周代码基础上补齐了 REST API 安全响应、接口文档、Bean 生命周期实验、事务传播实验、Redis 分布式锁与缓存策略、头像上传、Docker 容器化和单元测试。

## 技术栈

- Java 21
- Spring Boot 3.2.12
- Spring Security + JWT
- MyBatis-Plus 3.5.8
- MySQL 8.4
- Redis 7.4
- Knife4j / OpenAPI 3
- JUnit 5 + Mockito
- Docker / Docker Compose

## 项目结构

```text
src/main/java/com/example/usermanagement
├── cache/                  # Redis 缓存防护
├── common/                 # 统一响应结构
├── config/                 # Security、Redis、MyBatis、OpenAPI 配置
├── controller/             # REST API 控制器
├── demo/                   # Week6 Demo 代码
├── dto/                    # 请求 DTO 与校验
├── entity/                 # 实体类
├── exception/              # 全局异常处理
├── mapper/                 # MyBatis-Plus Mapper
├── security/               # JWT 与认证过滤器
└── service/                # 业务接口与实现
```

## 第 4、5 周反馈修复对应文件

| 改进项 | 对应文件 |
| --- | --- |
| SecurityConfig 增加 `AuthenticationEntryPoint` 和 `AccessDeniedHandler`，未登录返回 JSON 401，权限不足返回 JSON 403 | `src/main/java/com/example/usermanagement/config/SecurityConfig.java`、`src/main/java/com/example/usermanagement/security/JwtAuthenticationEntryPoint.java`、`src/main/java/com/example/usermanagement/security/JwtAccessDeniedHandler.java` |
| 将用户接口权限前移到 Security 过滤链，保证已登录但权限不足走 JSON 403 | `src/main/java/com/example/usermanagement/config/SecurityConfig.java` |
| 删除 Controller 层无效的 `AccessDeniedException` 处理，避免过滤器层异常被误判 | `src/main/java/com/example/usermanagement/exception/GlobalExceptionHandler.java` |
| DB 密码、Redis、JWT secret 改为环境变量注入，配置文件不写明文密码 | `src/main/resources/application.yml`、`docker-compose.yml` |
| PUT 改为 `PUT /api/users/{id}`，从路径取 id | `src/main/java/com/example/usermanagement/controller/UserController.java`、`src/main/java/com/example/usermanagement/dto/UserUpdateDTO.java` |
| update 先查原对象，只合并非 null 字段，避免 null 覆盖数据库字段 | `src/main/java/com/example/usermanagement/service/impl/UserServiceImpl.java` |
| 分页参数 `page` / `size` 增加 `@Min` / `@Max` | `src/main/java/com/example/usermanagement/controller/UserController.java` |
| 修正 `UserUpdateDTO` 校验 message 与 `@Size(min = 1)` 不一致问题 | `src/main/java/com/example/usermanagement/dto/UserUpdateDTO.java` |
| AuthController 登录、注册改为 `@PostMapping`，只允许 POST | `src/main/java/com/example/usermanagement/controller/AuthController.java`、`src/main/java/com/example/usermanagement/config/SecurityConfig.java` |
| `register` 增加 `@Transactional`，默认角色缺失时抛异常触发回滚 | `src/main/java/com/example/usermanagement/service/impl/UserServiceImpl.java` |
| JWT 密钥 `getBytes()` 指定 `StandardCharsets.UTF_8` | `src/main/java/com/example/usermanagement/security/JwtUtil.java` |
| JWT 过滤器白名单从 `contains` 改为 HTTP 方法 + 精确路径匹配 | `src/main/java/com/example/usermanagement/security/JwtAuthenticationFilter.java` |
| `e.printStackTrace()` 替换为 Slf4j `log.error` | `src/main/java/com/example/usermanagement/exception/GlobalExceptionHandler.java` |
| 不支持的 HTTP 方法返回 JSON 405，避免错误方法落到 500 | `src/main/java/com/example/usermanagement/exception/GlobalExceptionHandler.java` |

## Week 6 新任务对应文件

| 任务 | 对应文件 |
| --- | --- |
| 集成 Knife4j / OpenAPI 3 依赖 | `pom.xml` |
| 配置接口文档标题、描述、版本、联系人 | `src/main/java/com/example/usermanagement/config/OpenApiConfig.java`、`src/main/resources/application.yml` |
| Controller 添加 Swagger 注解 | `src/main/java/com/example/usermanagement/controller/AuthController.java`、`src/main/java/com/example/usermanagement/controller/UserController.java`、`src/main/java/com/example/usermanagement/dto/*.java` |
| Bean 生命周期实验 | `src/main/java/com/example/usermanagement/demo/beanLifecycle/BeanLifecycleDemo.java`、`src/main/java/com/example/usermanagement/demo/beanLifecycle/LifeCycleDemoRunner.java` |
| UserServiceImpl 纯 Mockito 单元测试 | `src/test/java/com/example/usermanagement/UserServiceImplTest.java` |
| JwtUtil 单元测试 | `src/test/java/com/example/usermanagement/JwtUtilTest.java` |
| Spring 事务传播实验 REQUIRED / REQUIRES_NEW / NESTED | `src/main/java/com/example/usermanagement/demo/transaction/TransactionDemoService.java`、`src/main/java/com/example/usermanagement/demo/transaction/TransactionDemoRunner.java` |
| Redis 分布式锁 Demo，两个线程抢同一把锁 | `src/main/java/com/example/usermanagement/demo/RedisLockDemo.java` |
| 缓存穿透、击穿、雪崩应对 | `src/main/java/com/example/usermanagement/cache/UserCacheManager.java`、`src/main/resources/application.yml` |
| 头像上传和下载 | `src/main/java/com/example/usermanagement/controller/UserController.java`、`src/main/java/com/example/usermanagement/service/UserService.java`、`src/main/java/com/example/usermanagement/service/impl/UserServiceImpl.java`、`src/main/java/com/example/usermanagement/entity/User.java` |
| Docker 容器化 | `Dockerfile`、`docker-compose.yml`、`docker/mysql/initdb/001-init.sql`、`.dockerignore` |

## 启动

推荐使用 Docker Compose，一次启动 Spring Boot、MySQL、Redis，并自动初始化数据库。

### 1. 准备环境

需要安装：

- JDK 21
- Docker Desktop
- Docker Compose

进入项目 `usermanagement` 根目录。

### 2. 创建 `.env`

在项目根目录创建 `.env` 文件。`docker-compose.yml` 实际读取以下变量：

```env
MYSQL_DATABASE=user_db
DB_USERNAME=root
DB_PASSWORD=change_root_password
REDIS_PASSWORD=change_redis_password
JWT_SECRET=change_this_jwt_secret_to_a_very_long_value_32plus
```


- `DB_PASSWORD` 同时作为 MySQL root 密码和 Spring Boot 数据库密码。
- `JWT_SECRET` 至少 32 个字符，否则 JWT HS256 密钥长度可能不足。
- 项目中的 `.env` 已被 `.gitignore` 忽略，不要提交真实密码。

### 3. 一键启动

```powershell
docker compose up -d --build
```

启动后创建 3 个容器：

- `usermanagement-mysql`：MySQL 8.4
- `usermanagement-redis`：Redis 7.4
- `usermanagement-app`：Spring Boot 应用

查看状态：

```powershell
docker compose ps
```

### 4. 数据库初始化

数据库初始化脚本：

```text
docker/mysql/initdb/001-init.sql
```

首次执行 `docker compose up -d --build` 时，MySQL 容器会自动把该目录挂载到 `/docker-entrypoint-initdb.d/`，并创建：

- 数据库：`user_db`
- 表：`user`、`role`、`user_role`
- 初始角色：`ROLE_ADMIN`、`ROLE_USER`
- 初始用户：`admin`、`user`

初始用户密码均为：

```text
123456
```

如果已经启动过容器并想重新执行初始化 SQL，需要删除 volume 后再启动：

```powershell
docker compose down -v
docker compose up -d --build
```

### 5. 访问地址

- 应用地址：`http://localhost:8080`
- Knife4j 文档：`http://localhost:8080/doc.html`
- OpenAPI JSON：`http://localhost:8080/v3/api-docs`
- MySQL：`localhost:3306`
- Redis：`localhost:6379`

### 6. 登录示例

管理员登录：

```powershell
curl.exe -X POST http://localhost:8080/api/auth/login `
  -H "Content-Type: application/json" `
  -d '{"username":"admin","password":"123456"}'
```

普通用户登录：

```powershell
curl.exe -X POST http://localhost:8080/api/auth/login `
  -H "Content-Type: application/json" `
  -d '{"username":"user","password":"123456"}'
```

### 7. 停止服务

```powershell
docker compose stop
```

如果要删除容器和数据卷：

```powershell
docker compose down -v
```

## 本地非 Docker 启动

如果不用 Docker，需要手动准备 MySQL 和 Redis。

### 1. 启动 MySQL 和 Redis

MySQL 需要创建并初始化 `user_db`。可以执行：

```text
docker/mysql/initdb/001-init.sql
```

Redis 需要保证端口、密码与环境变量一致。

### 2. 设置环境变量

PowerShell 示例：

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/user_db?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=UTF-8"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="change_root_password"
$env:REDIS_HOST="localhost"
$env:REDIS_PORT="6379"
$env:REDIS_PASSWORD="change_redis_password"
$env:JWT_SECRET="change_this_jwt_secret_to_a_very_long_value_32plus"
```

CMD 示例：

```cmd
set DB_URL=jdbc:mysql://localhost:3306/user_db?serverTimezone=Asia/Shanghai^&useUnicode=true^&characterEncoding=UTF-8
set DB_USERNAME=root
set DB_PASSWORD=change_root_password
set REDIS_HOST=localhost
set REDIS_PORT=6379
set REDIS_PASSWORD=change_redis_password
set JWT_SECRET=change_this_jwt_secret_to_a_very_long_value_32plus
```

### 3. 使用 Java 21 启动

PowerShell 示例：

```powershell
$env:JAVA_HOME="D:\DownLoad\Java\jdk21"
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\mvnw.cmd spring-boot:run
```

CMD 示例：

```cmd
set JAVA_HOME=D:\DownLoad\Java\jdk21
set PATH=%JAVA_HOME%\bin;%PATH%
mvnw.cmd spring-boot:run
```

## Demo 运行方式

Demo 代码使用独立 profile，不影响主项目默认启动。

### Bean 生命周期实验

```powershell
$env:JAVA_HOME="D:\DownLoad\Java\jdk21"
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=bean-lifecycle-demo"
```

对应日志会输出构造器、`@PostConstruct`、`InitializingBean.afterPropertiesSet()`、业务方法、`@PreDestroy`、`DisposableBean.destroy()` 的执行顺序。

### 事务传播实验

需要 MySQL 可用，并设置数据库环境变量。运行：

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=transaction-demo"
```

实验会自动创建 `transaction_demo_record` 表，并依次验证：

- REQUIRED 调用 REQUIRES_NEW
- REQUIRES_NEW 调用 REQUIRED
- REQUIRED 调用 NESTED

### Redis 分布式锁实验

需要 Redis 可用，并设置 Redis 环境变量。运行：

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=redis-lock-demo"
```

实验会启动两个线程抢同一把 Redis 锁，只有一个线程能获取成功。

## 测试说明

测试要求使用 Java 21。当前项目在 Java 21 下验证通过。

### 测试文件

| 测试文件 | 说明 |
| --- | --- |
| `src/test/java/com/example/usermanagement/UserServiceImplTest.java` | 纯 Mockito 测试，不启动 Spring 容器；覆盖 `getById` 正常、异常、无角色记录，以及 `pageQuery` 无关键词/有关键词场景，共 5 个测试 |
| `src/test/java/com/example/usermanagement/JwtUtilTest.java` | 直接 `new JwtUtil`，通过反射注入 `secret` 和 `expiration`；覆盖 token 生成、解析、校验和过期 token 校验失败，共 3 个测试 |
| `src/test/java/com/example/usermanagement/UsermanagementApplicationTests.java` | Spring Boot context smoke test，用于确认应用上下文可启动 |

### 运行全部测试

PowerShell：

```powershell
$env:JAVA_HOME="D:\DownLoad\Java\jdk21"
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\mvnw.cmd test
```

CMD：

```cmd
set JAVA_HOME=D:\DownLoad\Java\jdk21
set PATH=%JAVA_HOME%\bin;%PATH%
mvnw.cmd test
```

### 当前测试结果

最近一次 Java 21 测试结果：

```text
Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Surefire 报告位置：

```text
target/surefire-reports/com.example.usermanagement.UserServiceImplTest.txt
target/surefire-reports/com.example.usermanagement.JwtUtilTest.txt
target/surefire-reports/com.example.usermanagement.UsermanagementApplicationTests.txt
```

## 关键接口

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/auth/login` | 公开 | 登录并返回 JWT |
| POST | `/api/auth/register` | 公开 | 注册用户，默认分配 `ROLE_USER` |
| POST | `/api/auth/refresh` | 登录用户 | 刷新 Token |
| GET | `/api/users` | ADMIN / USER | 查询用户列表 |
| GET | `/api/users/{id}` | ADMIN / USER | 查询单个用户 |
| GET | `/api/users/page` | ADMIN / USER | 分页查询用户 |
| POST | `/api/users` | ADMIN | 新增用户 |
| PUT | `/api/users/{id}` | ADMIN | 更新用户，路径 id 定位资源 |
| DELETE | `/api/users/{id}` | ADMIN | 删除用户 |
| POST | `/api/users/{id}/avatar` | ADMIN | 上传 jpg/png 头像，最大 2MB |
| GET | `/api/users/{id}/avatar` | ADMIN / USER | 下载头像文件流 |

## 缓存策略说明

缓存入口在 `UserCacheManager.queryUserById`：

- 缓存穿透：数据库不存在的用户写入空值标记 `__NULL__`，短 TTL。
- 缓存击穿：缓存未命中时使用 Redis `SET key value NX EX` 获取互斥锁，只有拿到锁的线程回源数据库。
- 缓存雪崩：真实用户缓存 TTL 使用基础时间 + 随机偏移，避免大量 key 同时过期。

配置位于 `src/main/resources/application.yml` 的 `cache.user.*`。

## 文件上传说明

头像上传接口：

```text
POST /api/users/{id}/avatar
```

限制：

- 仅 ADMIN 可上传。
- 最大文件大小 2MB。
- 仅支持 `image/jpeg` 和 `image/png`。
- 文件保存到 `./uploads/`，文件名使用 UUID 防冲突。

Docker 启动时，上传目录映射到 Docker volume：

```text
app_uploads:/app/uploads
```
