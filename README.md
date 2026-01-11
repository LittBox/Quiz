# Quiz 项目（最小实现）

包含后端 Spring Boot（MySQL）和前端 Vue 3（Vite）骨架。

快速开始：

1. 准备 MySQL，并创建数据库：

```sql
CREATE DATABASE quizdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 修改后端数据库配置：编辑 `backend/src/main/resources/application.yml`，设置 `spring.datasource.username` 和 `spring.datasource.password`。

3. 启动后端（需要 Maven & JDK 17）：

```bash
cd backend
mvn spring-boot:run
```

4. 启动前端（需要 Node 18+）：

```bash
cd frontend
npm install
npm run dev
```


# 前端项目结构


├─index.html
├─package-lock.json
├─package.json
├─vite.config.js
├─src
|  ├─App.vue
|  ├─main.js
|  ├─components
|  |     ├─Quiz.vue
|  |     └Upload.vue