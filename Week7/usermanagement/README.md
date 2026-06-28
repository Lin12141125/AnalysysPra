# SmartTask 智能任务管理平台

基于 Spring Boot 3.2、Java 21、Spring Security、JWT、MyBatis-Plus、MySQL、Redis、Knife4j 和 Docker Compose 的团队协作任务管理后端。项目在原用户管理系统基础上增量开发，保留用户、认证、系统角色、缓存和文件上传能力，并扩展了项目、项目成员、任务、任务评论和任务附件模块。

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

## 核心能力

### 项目管理

- 创建项目，创建者自动成为项目 `OWNER`。
- 分页查询当前用户参与的项目列表。
- 查看项目详情，包含项目成员列表和任务状态统计。
- 仅项目 `OWNER` 可以更新、删除项目。
- 删除项目依赖数据库外键级联删除任务、评论、附件记录，并同步清理磁盘附件文件。
- 仅项目 `OWNER` 可以邀请、移除成员。
- 邀请成员时只能分配 `MEMBER` 或 `VIEWER`，不能直接邀请新的 `OWNER`。

### 任务管理

- 在项目下创建任务。
- 任务列表支持分页，并支持按状态、优先级、负责人筛选。
- 任务状态按 `TODO -> IN_PROGRESS -> IN_REVIEW -> DONE` 顺序流转，禁止跳过中间状态。
- `PUT /api/tasks/{id}/assign` 将任务分配给项目成员，当前实现为仅项目 `OWNER` 可以分配或重新分配负责人。
- `OWNER` 可以更新、删除任意任务。
- `MEMBER` 只能更新、删除自己负责的任务，不能改负责人。
- `VIEWER` 对任务只读。

### 协作功能

- 任务评论：`OWNER` 和 `MEMBER` 可以添加评论，`VIEWER` 可以查看评论；`OWNER` 可删除任意评论，`MEMBER` 只能删除自己的评论。
- 任务附件：`OWNER` 和 `MEMBER` 可以上传附件，项目成员均可下载附件，`VIEWER` 只读下载。
- 附件大小限制为 10MB。
- 附件保存到 `uploads/task-attachments`，文件名使用 UUID 防冲突。
- 删除任务或项目时，会在数据库级联删除附件记录后清理对应的磁盘附件文件。

## 角色模型

两套独立角色体系：

| 角色体系 | 表/字段 | 示例 | 作用范围 |
| --- | --- | --- | --- |
| 系统角色 | `role`、`user_role` | `ROLE_ADMIN`、`ROLE_USER` | 用户管理、认证后的系统级接口权限 |
| 项目角色 | `project_member.role` | `OWNER`、`MEMBER`、`VIEWER` | 单个项目内的项目、任务、评论、附件权限 |


## 数据库设计

初始化脚本位于：

```text
docker/mysql/initdb/001-schema.sql
docker/mysql/initdb/002-seed-data.sql
```

核心表：

| 表名 | 说明 |
| --- | --- |
| `user` | 用户表 |
| `role` | 系统角色表 |
| `user_role` | 用户与系统角色多对多关联表 |
| `project` | 项目表 |
| `project_member` | 项目成员表，保存项目级角色 |
| `task` | 任务表 |
| `task_comment` | 任务评论表 |
| `task_attachment` | 任务附件表 |

级联关系：

- 删除项目：数据库外键级联删除项目成员、任务、任务评论、任务附件记录。
- 删除任务：数据库外键级联删除任务评论、任务附件记录。
- 删除用户：任务负责人、评论人、附件上传人会按外键策略置空或级联删除关联记录。
- 删除项目或任务时，业务层还会清理对应的磁盘附件文件。

## 项目结构

```text
src/main/java/com/example/usermanagement
├── cache/                  # User / Project / Task Redis 缓存管理
├── common/                 # Result<T> 统一响应结构
├── config/                 # Security、Redis、MyBatis-Plus、OpenAPI 配置
├── controller/             # REST API 控制器
├── dto/                    # 请求 DTO 与参数校验
├── entity/                 # MyBatis-Plus 实体
├── enums/                  # 任务状态枚举
├── exception/              # BusinessException 与全局异常处理
├── mapper/                 # MyBatis-Plus Mapper 与关联查询 SQL
├── security/               # JWT、登录用户详情、认证过滤器
├── service/                # 业务接口
└── service/impl/           # 业务实现
```

## 关键接口

### 认证与用户

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/auth/login` | 登录并返回 JWT |
| POST | `/api/auth/register` | 注册用户，默认分配 `ROLE_USER` |
| POST | `/api/auth/refresh` | 刷新 Token |
| GET | `/api/users` | 查询用户列表 |
| GET | `/api/users/{id}` | 查询用户详情 |
| GET | `/api/users/page` | 分页查询用户 |
| POST | `/api/users` | 新增用户，仅 `ROLE_ADMIN` |
| PUT | `/api/users/{id}` | 更新用户，仅 `ROLE_ADMIN` |
| DELETE | `/api/users/{id}` | 删除用户，仅 `ROLE_ADMIN` |
| POST | `/api/users/{id}/avatar` | 上传头像，仅 `ROLE_ADMIN` |
| GET | `/api/users/{id}/avatar` | 下载头像 |

### 项目与成员

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/projects` | 创建项目，创建者自动成为 `OWNER` |
| GET | `/api/projects` | 分页查询我参与的项目 |
| GET | `/api/projects/{id}` | 查看项目详情，含成员和任务统计 |
| PUT | `/api/projects/{id}` | 更新项目，仅 `OWNER` |
| DELETE | `/api/projects/{id}` | 删除项目，仅 `OWNER` |
| POST | `/api/projects/{projectId}/members` | 邀请成员，仅 `OWNER`，角色只能是 `MEMBER` 或 `VIEWER` |
| GET | `/api/projects/{projectId}/members` | 查看成员列表，`OWNER` / `MEMBER` |
| DELETE | `/api/projects/{projectId}/members/{userId}` | 移除成员，仅 `OWNER`，不能移除项目负责人 |

### 任务、评论、附件

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/projects/{projectId}/tasks` | 创建任务 |
| GET | `/api/projects/{projectId}/tasks` | 分页查询任务，支持 `status`、`priority`、`assigneeId` 筛选 |
| PUT | `/api/tasks/{id}` | 更新任务内容、优先级、截止时间 |
| DELETE | `/api/tasks/{id}` | 删除任务，并清理磁盘附件文件 |
| PATCH | `/api/tasks/{id}/status` | 更新任务状态，校验状态机流转 |
| PUT | `/api/tasks/{id}/assign` | 分配任务负责人，仅 `OWNER` |
| POST | `/api/tasks/{taskId}/comments` | 添加任务评论 |
| GET | `/api/tasks/{taskId}/comments` | 查看任务评论，按时间正序 |
| DELETE | `/api/comments/{id}` | 删除评论 |
| POST | `/api/tasks/{taskId}/attachments` | 上传任务附件，最大 10MB |
| GET | `/api/attachments/{id}` | 下载任务附件 |

## 缓存策略

缓存入口：

- `UserCacheManager`：用户详情缓存。
- `ProjectCacheManager`：项目详情、我参与的项目列表缓存。
- `TaskCacheManager`：任务列表缓存。

缓存防护：

- 缓存穿透：数据库不存在的数据写入空值标记 `__NULL__`，短 TTL。
- 缓存击穿：缓存未命中时使用 Redis 互斥锁回源数据库。
- 缓存雪崩：真实数据缓存 TTL 使用基础时间 + 随机偏移。

项目或任务更新、删除后会清理对应缓存。

## Docker 启动

### 1. 准备 `.env`

在项目根目录创建 `.env`：

```env
MYSQL_DATABASE=user_db
DB_USERNAME=root
DB_PASSWORD=change_root_password
REDIS_PASSWORD=change_redis_password
JWT_SECRET=change_this_jwt_secret_to_a_very_long_value_32plus
```

注意：

- `DB_PASSWORD` 同时作为 MySQL root 密码和应用数据库密码。
- `JWT_SECRET` 至少 32 个字符。
- 不要提交真实 `.env`。

### 2. 启动服务

```powershell
docker compose up -d --build
```

启动后容器：

- `smarttask-mysql`
- `smarttask-redis`
- `smarttask-app`

查看状态：

```powershell
docker compose ps
```

### 3. 重新初始化数据库

如果修改了 `docker/mysql/initdb` 下的 SQL，或希望重新加载种子数据，需要删除旧 volume：

```powershell
docker compose down -v
docker compose up -d --build
```

当前 Compose 声明的 volume 为：

- `smarttask_mysql_data`：MySQL 数据。
- `smarttask_redis_data`：Redis 数据。
- `smarttask_app_uploads`：应用上传文件。

Docker 实际创建的 volume 名通常会带 Compose 项目前缀，例如 `smarttask_smarttask_mysql_data`。

## 本地非 Docker 启动

需要本地 MySQL、Redis 可用，并设置环境变量。

PowerShell：

```powershell
$env:JAVA_HOME="D:\DownLoad\Java\jdk21"
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
$env:DB_URL="jdbc:mysql://localhost:3306/user_db?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=UTF-8"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="change_root_password"
$env:REDIS_HOST="localhost"
$env:REDIS_PORT="6379"
$env:REDIS_PASSWORD="change_redis_password"
$env:JWT_SECRET="change_this_jwt_secret_to_a_very_long_value_32plus"
.\mvnw.cmd spring-boot:run
```

CMD：

```cmd
set JAVA_HOME=D:\DownLoad\Java\jdk21
set PATH=%JAVA_HOME%\bin;%PATH%
set DB_URL=jdbc:mysql://localhost:3306/user_db?serverTimezone=Asia/Shanghai^&useUnicode=true^&characterEncoding=UTF-8
set DB_USERNAME=root
set DB_PASSWORD=change_root_password
set REDIS_HOST=localhost
set REDIS_PORT=6379
set REDIS_PASSWORD=change_redis_password
set JWT_SECRET=change_this_jwt_secret_to_a_very_long_value_32plus
mvnw.cmd spring-boot:run
```

## 测试

项目测试使用 Java 21。

运行完整测试：

```cmd
set JAVA_HOME=D:\DownLoad\Java\jdk21
set PATH=%JAVA_HOME%\bin;%PATH%
mvnw.cmd test
```

主要测试文件：

| 测试文件 | 覆盖范围 |
| --- | --- |
| `UserServiceImplTest.java` | 用户查询、分页、角色填充 |
| `JwtUtilTest.java` | JWT 生成、解析、校验、过期判断 |
| `ProjectServiceImplTest.java` | 项目创建、列表、详情、更新、删除 |
| `ProjectMemberServiceImplTest.java` | 成员邀请、移除、查看和权限 |
| `TaskServiceImplTest.java` | 任务 CRUD、筛选、状态流转 |
| `TaskAssignmentServiceImplTest.java` | 任务分配、优先级筛选、OWNER-only 分配 |
| `TaskCommentServiceImplTest.java` | 任务评论添加、查询、删除权限 |
| `TaskAttachmentServiceImplTest.java` | 附件上传、下载、10MB 限制、种子附件下载 |
| `UsermanagementApplicationTests.java` | Spring Boot 上下文启动 |

最近验证命令输出：

```text
FULL_TEST_SUCCESS
```

## 访问地址

- 应用地址：`http://localhost:8080`
- Knife4j 文档：`http://localhost:8080/doc.html`
- OpenAPI JSON：`http://localhost:8080/v3/api-docs`
- MySQL：`localhost:3306`
- Redis：`localhost:6379`

## 默认种子数据

默认用户密码均为：

```text
123456
```

种子用户：

| 用户名 | 系统角色 | 说明 |
| --- | --- | --- |
| `admin` | `ROLE_ADMIN` | 管理员账号，同时是 `project1` 的项目 `OWNER` |
| `user` | `ROLE_USER` | 普通账号，同时是 `project1` 的 `MEMBER`、`project2` 的 `OWNER` |
| `member` | `ROLE_USER` | 普通项目成员 |
| `viewer` | `ROLE_USER` | 项目只读成员 |

种子数据还包含项目、成员关系、任务、评论和 `Week7.txt` 任务附件。