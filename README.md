# LibraryWeb2026 图书馆管理系统

基于 **Servlet + JDBC + SQL Server** 的图书馆 Web 项目，前端使用 **HTML/CSS/Vue 3/Axios**，以 Maven WAR 形式部署到 **Tomcat 9**。项目不使用 JSP、Spring、MyBatis，服务端请求全部由 Servlet 处理，数据库访问统一使用作业 1 的 `JDBCTool`（在此基础上做了必要增强）。

前后端代码位于同一个 Maven 工程，页面与 Servlet 一起打包进 WAR，由 Tomcat 直接提供，不涉及独立前端服务或跨域部署。

## 1. 技术栈

| 层次 | 技术 |
| --- | --- |
| 前端 | HTML5、CSS3、Vue 3.5（本地文件）、Axios 1.7（本地文件） |
| 服务端 | Java Servlet 4.0、Filter、JDBC |
| 数据库 | SQL Server 2019，数据库名 `libraryDB` |
| 构建 | Maven 3.9，WAR 包 |
| 运行 | Tomcat 9.0.x，JDK 17+ |

## 2. 功能与角色

| 功能 | 管理员 | 操作员 | 学生 | 教师 |
| --- | :---: | :---: | :---: | :---: |
| 用户信息查询 | ✓ | | | |
| 借阅记录查询 | ✓ | | | |
| 借阅数据统计 | ✓ | | | |
| 审阅推荐图书 | ✓ | | | |
| 添加书籍 | | ✓ | | |
| 查看所有书籍 | | ✓ | | |
| 编辑图书 | | ✓ | | |
| 图书副本编辑 | | ✓ | | |
| 废弃图书 | | ✓ | | |
| 借书、还书 | ✓ | ✓ | ✓ | ✓ |
| 个人借阅查询 | | ✓ | ✓ | ✓ |
| 推荐图书 | | | | ✓ |

主要业务规则：

- `book.stock` 表示**当前可借库存**；借书时库存减 1，还书时库存加 1。
- 新增图书按 `stock` 自动生成对应数量的 `bookcopy` 副本；新增副本库存加 1，删除副本库存减 1。
- 同一读者最多同时借阅 5 本；借阅期限取图书的 `days` 字段，逾期在查询时动态计算。
- 废弃图书要求没有未归还副本，且可借库存等于副本总数；废弃只改状态，保留历史借阅记录。
- 编辑图书只允许修改题名、作者、类型、出版社、可借天数、价格；库存、状态、副本不受影响。
- 推荐图书时先按书名检索馆藏，已有相似图书则直接返回馆藏信息，否则写入 `bookrecommend` 等待管理员审核。

## 3. 目录结构

```text
LibraryWeb2026/
├─ pom.xml
├─ README.md
└─ src/main/
   ├─ java/com/
   │  ├─ util/
   │  │  ├─ JDBCTool.java          # 作业1 JDBCTool 的增强版
   │  │  ├─ JsonUtil.java          # 无第三方依赖的 JSON 输出
   │  │  ├─ Dto.java               # 实体转前端 DTO
   │  │  └─ TableName.java         # 解决 [user] 保留字表名
   │  └─ library/
   │     ├─ entity/                # Base/Book/BookCopy/User 及四个角色子类/Loan/BookRecommend
   │     ├─ dao/                   # UserDao/BookDao/BookCopyDao/LoanDao/StatDao/BookRecommendDao
   │     ├─ service/               # 认证、图书、借阅、推荐、统计业务与事务
   │     ├─ servlet/               # Auth/Captcha/User/Book/BookCopy/Loan/Stat/Recommend Servlet
   │     └─ filter/ApiFilter.java  # UTF-8 与 /api/* 登录校验
   ├─ resources/db.properties      # 数据库连接配置
   └─ webapp/
      ├─ *.html                    # 13 个页面
      ├─ css/style.css
      ├─ js/common.js
      ├─ vendor/                   # 本地 vue.global.prod.js、axios.min.js
      └─ WEB-INF/web.xml
```

## 4. 运行前准备

1. 安装 **JDK 17 或更高版本**，配置 `JAVA_HOME`。
2. 安装 **Maven 3.9+**，或使用 IntelliJ IDEA 内置 Maven。
3. 准备 **Tomcat 9.0.x**（必须是 Tomcat 9，不能使用 Tomcat 10+，因为项目使用 `javax.servlet`）。
4. SQL Server 实例已启动，已还原 `libraryDB` 数据库，并启用 **TCP/IP** 与 **SQL Server 身份验证**。
5. 检查 `src/main/resources/db.properties`：

```properties
jdbc.url=jdbc:sqlserver://localhost:1433;databaseName=libraryDB;encrypt=true;trustServerCertificate=true
jdbc.username=sa
jdbc.password=hpm060612HPM
```

如果本机数据库账号或端口不同，修改此文件即可；也可以在启动 Tomcat 时通过 JVM 参数覆盖：

```text
-Dlibrary.db.url="jdbc:sqlserver://localhost:1433;databaseName=libraryDB;encrypt=true;trustServerCertificate=true"
-Dlibrary.db.username=sa
-Dlibrary.db.password=你的密码
```

## 5. 构建与部署

在项目根目录执行：

```bash
mvn clean package
```

生成：

```text
target/LibraryWeb2026.war
```

把 WAR 复制到 Tomcat 的 `webapps` 目录，启动 Tomcat：

```bat
copy target\LibraryWeb2026.war %CATALINA_HOME%\webapps\
%CATALINA_HOME%\bin\startup.bat
```

浏览器访问：

```text
http://localhost:8080/LibraryWeb2026/
```

登录页：

```text
http://localhost:8080/LibraryWeb2026/login.html
```

> 如果 Tomcat 已经启动，复制 WAR 后等待几秒自动部署；也可以停止 Tomcat、删除 `webapps/LibraryWeb2026` 展开目录后再复制 WAR，确保加载的是最新代码。

## 6. 在 IntelliJ IDEA 中运行

1. 打开 IDEA，选择 **Open**，打开 `D:\LibraryDB\LibraryWeb2026\pom.xml`，按 Maven 项目导入。
2. Project Structure → SDK 选择 JDK 17 或 21。
3. 配置 Tomcat：
   - Run → Edit Configurations → **+** → **Tomcat Server** → **Local**。
   - Application server 选择本机 Tomcat 9 目录。
   - Deployment → **+** → **Artifact** → 选择 `LibraryWeb2026:war exploded`。
   - Application context 填 `/LibraryWeb2026`。
4. 启动配置，访问 `http://localhost:8080/LibraryWeb2026/`。

如果 IDEA 提示找不到 Maven，可使用其内置 Maven：`Settings → Build, Execution, Deployment → Build Tools → Maven`。

## 7. 演示账号

| 用户名 | 密码 | 角色 |
| --- | --- | --- |
| `admin` | `admin123` | 管理员 |
| `operator` | `operator123` | 操作员 |
| `student1` | `student123` | 学生 |
| `teacher` | `teacher123` | 教师 |

登录需要输入页面显示的 4 位验证码。验证码保存在 Session 中，点击图片可刷新。

## 8. 主要接口

统一响应格式：

```json
{ "code": 0, "message": "ok", "data": {} }
```

`code=0` 表示成功；业务失败返回 `code=1` 和提示信息；未登录返回 HTTP 401；无权限返回 HTTP 403。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/auth/login` | 登录（name/password/captcha） |
| POST | `/api/auth/register` | 注册（按角色提交扩展字段） |
| POST | `/api/auth/logout` | 退出 |
| GET | `/api/auth/me` | 当前登录用户 |
| GET | `/api/captcha` | 验证码 PNG |
| GET | `/api/users?keyword=` | 管理员用户查询 |
| GET | `/api/users/detail?id=` | 用户详情 |
| GET | `/api/books?keyword=` | 图书查询 |
| GET | `/api/books/borrowable` | 可借图书 |
| GET | `/api/books/detail?id=` | 图书及副本详情 |
| POST | `/api/books/add` | 添加图书 |
| POST | `/api/books/update` | 编辑图书 |
| POST | `/api/books/discard` | 废弃图书 |
| GET | `/api/bookcopies?bookId=` | 副本列表 |
| POST | `/api/bookcopies/add` | 新增副本 |
| POST | `/api/bookcopies/delete` | 删除副本 |
| GET | `/api/loans/my` | 个人借阅记录 |
| GET | `/api/loans/search` | 管理员借阅查询 |
| POST | `/api/loans/borrow` | 借书 |
| POST | `/api/loans/return` | 还书 |
| GET | `/api/stats/types` | 按图书类型统计 |
| GET | `/api/stats/majors` | 按用户专业统计 |
| POST | `/api/recommend/check` | 推荐前查重/提交推荐 |
| GET | `/api/recommend/mine` | 我的推荐 |
| GET | `/api/recommend/pending` | 待审核推荐 |
| POST | `/api/recommend/review` | 审核推荐 |

请求参数采用 `application/x-www-form-urlencoded` 格式；返回字段与 `com.util.Dto` 一致。

## 9. 实现说明

- **JDBCTool**：保留作业 1 的 `getConnection / close / resultSetToList / save / update / delete / getOneById` 等 API，增强了对继承字段、`LocalDateTime`/`LocalDate`、`boolean`、`BigDecimal`、`[user]` 保留字表名和集合字段的处理；连接配置支持系统属性覆盖，便于部署和测试。
- **Servlet 分发**：`BaseServlet` 重写 `service`，按 HTTP 方法分发到 `handleGet`/`handlePost`，统一处理业务异常、鉴权异常和未知异常；子 Servlet 使用 `@WebServlet` 注解注册。
- **事务与并发**：借书、还书、添加图书、废弃图书、推荐审核等写操作使用 JDBC 事务；借书通过 `WITH (UPDLOCK, ROWLOCK)` 锁定图书和副本，避免并发超借；失败自动回滚。
- **验证码**：`CaptchaServlet` 生成 4 位字符 PNG 并存入 Session，登录时校验，使用后失效。
- **前端**：不使用 Node 构建，静态页面直接由 Tomcat 提供；`js/common.js` 统一封装 Axios、上下文路径、登录跳转、提示、角色样式和侧边栏。

## 10. 注意事项

- 本项目按课程作业要求使用明文密码比对，仅用于教学演示；生产环境应使用 BCrypt 等哈希算法。
- `db.properties` 中包含数据库口令，提交到公开仓库前请改为占位配置或使用环境变量/JVM 参数覆盖。
- 删除有过借阅记录的副本会被拒绝，这是为了保证 `loan` 表外键与历史借阅数据完整；如需删除，应先处理历史数据。
- 若页面能打开但接口报数据库连接错误，请检查 SQL Server 服务、TCP 1433 端口、混合身份验证以及 `sa` 账号状态。
- 浏览器直接访问 `home.html` 等页面时，前端会调用 `/api/auth/me` 校验登录并自动跳转登录页。

## 11. Git 与 Gitee

项目目录已经初始化为 Git 仓库，默认分支为 `master`，并已完成首次提交。由于需要用户自己的 Gitee 账号和仓库地址，代码尚未推送到远程仓库。创建空仓库后执行：

```bash
git remote add origin https://gitee.com/你的用户名/仓库名.git
git push -u origin master
```

提交前建议先修改 `db.properties` 中的数据库口令，避免公开仓库泄露本机账号。