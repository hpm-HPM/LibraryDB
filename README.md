# LibraryWeb2026 图书馆管理系统

基于 Servlet + JDBC + SQL Server 的图书管理系统，前端使用 HTML/CSS/Vue 3/Axios，Maven 构建，部署到 Tomcat 9。

## 功能

- 注册、登录、验证码
- 用户信息查询（管理员）
- 图书添加、编辑、废弃、副本管理
- 借书、还书、个人借阅查询
- 借阅记录查询、借阅统计
- 图书推荐与审阅

## 技术栈

- 后端：Servlet 4.0、JDBC、SQL Server
- 前端：HTML、CSS、Vue 3、Axios
- 构建与运行：Maven、Tomcat 9、JDK 17+

## 运行步骤

1. 启动 SQL Server，确保 `libraryDB` 数据库已还原。
2. 修改 `src/main/resources/db.properties` 中的数据库连接信息。
3. 在项目根目录执行：

   ```bash
   mvn clean package
   ```

4. 将 `target/LibraryWeb2026.war` 复制到 Tomcat 的 `webapps` 目录。
5. 启动 Tomcat，访问：

   http://localhost:8080/LibraryWeb2026/login.html

## 演示账号

| 用户名 | 密码 | 角色 |
| --- | --- | --- |
| `admin` | `admin123` | 管理员 |
| `operator` | `operator123` | 操作员 |
| `student1` | `student123` | 学生 |
| `teacher` | `teacher123` | 教师 |

## 目录结构

```text
src/main/java/com/library
├── entity      实体类
├── dao         数据访问层
├── service     业务逻辑层
├── servlet     请求处理
└── filter      登录与编码过滤器

src/main/webapp
├── css         样式
├── js          公共脚本
├── vendor      Vue、Axios
└── *.html      页面
```

## 说明

- 未使用 JSP、Spring、MyBatis，按要求使用 Servlet + JDBC。
- 详细接口和实现可查看源码。
