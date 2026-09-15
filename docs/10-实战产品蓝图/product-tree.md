# 后端实现导航树

按真实目录定位代码：`controller` 负责 HTTP（请求与响应）契约，`service` 负责业务规则，`mapper` 访问数据库，`entity` 映射表；安全、配置和异常处理在各自的包中。卡片状态以[实践计划与进度](实践计划/README.md)为准。

```text
boot-server/
├── pom.xml                          Maven（构建工具）聚合工程；Spring Boot 4、Java 25
├── infrastructure/common-core/      不依赖 HTTP 或数据库的公共契约
│   └── src/main/java/com/example/bootserver/common/
│       ├── result/Result.java        统一响应 code、message、data
│       └── error/
│           ├── ErrorCode.java        稳定错误码与 HTTP 分类
│           └── BusinessException.java 业务失败交给 Web 层转换
└── services/user-service/           当前业务模块
    ├── pom.xml
    └── src/
        ├── main/
        │   ├── java/com/example/bootserver/
        │   │   ├── BootServerApplication.java  启动入口
        │   │   ├── controller/                HTTP 入口与接口数据对象（DTO）
        │   │   │   ├── AuthController.java     注册、登录并签发 JWT（登录令牌）
        │   │   │   ├── UserController.java     当前用户与后台用户管理
        │   │   │   ├── ProductController.java  商品分页/详情；管理员创建、上下架
        │   │   │   ├── SkuController.java      管理员按版本改价
        │   │   │   └── dto/
        │   │   │       ├── RegisterRequest.java、LoginRequest.java、LoginResponse.java
        │   │   │       ├── CreateUserRequest.java、UpdateUserRequest.java、UserPageRequest.java
        │   │   │       ├── ProductPageRequest.java、ProductPageResponse.java 等浏览 DTO
        │   │   │       └── CreateProductRequest.java、CreateSkuRequest.java 等管理 DTO
        │   │   │           请求字段白名单；商品浏览响应不暴露删除标记和版本
        │   │   ├── service/                   业务规则与事务边界
        │   │   │   ├── UserService.java        注册、登录、管理员引导、用户增删改查
        │   │   │   ├── ProductQueryService.java 上架过滤、稳定分页、商品与 SKU（商品规格）详情
        │   │   │   └── ProductManagementService.java
        │   │   │       商品和 SKU 原子创建；上下架；按版本条件更新价格
        │   │   ├── mapper/                    MyBatis-Plus（持久化框架）数据访问
        │   │   │   ├── UserMapper.java、RoleMapper.java、UserRoleMapper.java、UserAuthorityMapper.java
        │   │   │   └── ProductMapper.java、SkuMapper.java
        │   │   │       用户权限联表查询；商品和 SKU 单表访问
        │   │   ├── entity/                    数据库表映射
        │   │   │   ├── User.java、Role.java、UserRole.java  账号与角色关系
        │   │   │   └── Product.java、Sku.java   商品状态、逻辑删除、version（改价版本）；
        │   │   │       金额使用 BigDecimal（精确十进制）
        │   │   ├── security/                  JWT 认证与 RBAC（基于角色的权限控制）
        │   │   │   ├── SecurityConfig.java     商品读取需登录；写入需 product:manage
        │   │   │   ├── JwtAuthenticationFilter.java、JwtTokenService.java、UserAuthorityService.java
        │   │   │   │   验证令牌和当前账号状态，每次请求从数据库加载权限
        │   │   │   ├── RestAuthenticationEntryPoint.java、RestAccessDeniedHandler.java
        │   │   │   │   分别返回统一的 401、403 响应
        │   │   │   └── RoleCodes.java、PermissionCodes.java、BearerAuthentication.java
        │   │   │       稳定角色码、权限码与 Bearer（认证头方案）前缀
        │   │   ├── config/                    框架与可部署配置
        │   │   │   ├── MybatisPlusConfig.java  乐观锁与 H2（嵌入式数据库）分页插件
        │   │   │   ├── PasswordConfig.java     BCrypt（密码哈希算法）编码器
        │   │   │   ├── ServletPathProperties.java  MVC（Web 接口）外部路径配置
        │   │   │   ├── OpenApiConfig.java      OpenAPI（接口文档）与 Bearer 方案
        │   │   │   └── LocalAdminProperties.java、LocalAdminInitializer.java
        │   │   │       环境变量控制本地管理员幂等引导
        │   │   ├── handler/GlobalExceptionHandler.java  统一错误响应
        │   │   └── exception/UserNotFoundException.java 用户不存在语义
        │   └── resources/
        │       ├── application.yml           数据源、JWT、MVC 路径等运行配置
        │       └── db/schema.sql             用户/RBAC、商品/SKU 表与幂等种子；
        │           状态、价格、唯一编码和外键约束
        └── test/
            ├── java/com/example/bootserver/
            │   ├── controller/              HTTP 契约与授权
            │   │   ├── AuthControllerWebTests.java、UserControllerWebTests.java
            │   │   ├── ProductQueryIntegrationTests.java
            │   │   └── ProductManagementIntegrationTests.java  并发改价只允许一次成功
            │   ├── mapper/ProductSkuMapperIntegrationTests.java  数据约束与种子幂等
            │   ├── security/JwtAuthenticationIntegrationTests.java  真实 JWT 请求
            │   ├── service/RegisterFlowIntegrationTests.java、LoginFlowIntegrationTests.java
            │   ├── config/LocalAdminBootstrapIntegrationTests.java
            │   ├── handler/GlobalExceptionHandlerWebTests.java
            │   ├── ApiPrefixIntegrationTests.java  可变 MVC 前缀与 OpenAPI
            │   └── BootServerApplicationTests.java  启动与初始化
            └── resources/application.properties  测试用固定假密钥
```
