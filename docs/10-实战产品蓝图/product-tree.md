# 后端实现导航树

按真实目录定位代码：既有商品与用户功能按 `controller/service/mapper/entity` 分包；购物车、库存和订单按业务上下文分包，在内部区分 Web、应用、领域和基础设施。卡片状态以[实践计划与进度](实践计划/README.md)为准。

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
        │   │   ├── cart/                      购物车上下文
        │   │   │   ├── web/CartController.java、AddCartItemRequest.java、UpdateCartItemRequest.java、CartItemResponse.java
        │   │   │   │   仅认证接口；数量请求校验与现价响应
        │   │   │   ├── application/CartService.java、CartWriteStepService.java
        │   │   │   │   加购重试编排；每个原子写步骤独立事务
        │   │   │   ├── domain/CartQuantity.java、CartItemRepository.java、CartEntry.java
        │   │   │   │   数量不变式、仓储端口与只读条目
        │   │   │   └── infrastructure/CartItemEntity.java、CartItemMapper.java、CartItemRow.java、MyBatisCartItemRepository.java
        │   │   │       条件累加、唯一键插入、本人数据过滤与关联现价查询
        │   │   ├── stock/                     库存上下文
        │   │   │   ├── web/StockController.java、UpdateStockRequest.java、StockResponse.java、StrictStockTotalDeserializer.java
        │   │   │   │   管理员设置总量、严格整数绑定与查询 total/locked/available
        │   │   │   ├── application/StockService.java、StockWriteStepService.java
        │   │   │   │   SKU 存在性协作、条件更新、首次插入竞争重试
        │   │   │   ├── domain/Stock.java、StockBelowLockedException.java、StockRepository.java
        │   │   │   │   库存不变式与仓储端口
        │   │   │   └── infrastructure/StockEntity.java、StockMapper.java、MyBatisStockRepository.java
        │   │   │       库存表映射与原子设置、预扣
        │   │   ├── order/                     订单上下文
        │   │   │   ├── web/OrderController.java、CreateOrderRequest.java、CreateOrderItemRequest.java、CreateOrderResponse.java
        │   │   │   │   仅认证的下单入口、请求校验和创建回执
        │   │   │   ├── application/OrderService.java、OrderWriteStepService.java、OrderNumberGenerator.java、CreatedOrder.java
        │   │   │   │   幂等协调、独立写事务、快照计价和雪花式订单号
        │   │   │   ├── domain/
        │   │   │   │   ├── Order.java、OrderLine.java、OrderStatus.java、OrderRepository.java
        │   │   │   │   │   订单金额与明细不变式、状态及仓储端口
        │   │   │   │   └── IdempotencyKey.java、OrderSelection.java、OrderRequestFingerprint.java、ExistingOrder.java
        │   │   │   │       用户作用域幂等键、请求语义、指纹与原单投影
        │   │   │   └── infrastructure/OrderEntity.java、OrderItemEntity.java、OrderMapper.java、OrderItemMapper.java、MyBatisOrderRepository.java
        │   │   │       订单头、明细与用户加幂等键唯一约束的持久化映射
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
        │   │   │   ├── ProductQueryService.java、OrderableSkuQuote.java 上架过滤、商品详情与下单报价；库存侧 SKU 存在性查询
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
        │   │   │   ├── SecurityConfig.java     商品读取、购物车与下单需登录；商品写入与库存管理需 product:manage
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
        │       ├── application.yml           数据源、JWT、MVC 路径、订单 worker ID 等运行配置
        │       └── db/schema.sql             用户/RBAC、商品/SKU、购物车、库存、订单表与幂等种子；
        │           数量范围、库存锁定约束、状态、价格、唯一编码和外键约束
        └── test/
            ├── java/com/example/bootserver/
            │   ├── cart/domain/CartQuantityTest.java  数量边界
            │   ├── cart/web/CartIntegrationTests.java  真实 HTTP、并发累加与本人隔离
            │   ├── stock/domain/StockTest.java  可用量与库存不变式
            │   ├── stock/StockArchitectureTest.java  库存分层与依赖方向约束
            │   ├── stock/web/StockIntegrationTests.java  真实 HTTP、权限、条件更新与并发首次设置
            │   ├── order/domain/OrderTest.java  订单金额和明细不变式
            │   ├── order/domain/OrderRequestFingerprintTest.java  规范化请求指纹与键边界
            │   ├── order/application/OrderNumberGeneratorTest.java  订单号唯一性与 worker 边界
            │   ├── order/OrderArchitectureTest.java  订单分层与依赖方向约束
            │   ├── order/web/OrderIntegrationTests.java  下单 HTTP、原子回滚、并发预扣与幂等重放
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
