package com.example.bootserver.order.web;

import com.example.bootserver.common.error.ErrorCode;
import com.example.bootserver.common.result.Result;
import com.example.bootserver.config.ServletPathProperties;
import com.example.bootserver.security.BearerAuthentication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

/** 真实 HTTP 验证下单的身份、价格快照、条件预扣和整单回滚。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:order-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.sql.init.mode=always"
})
class OrderIntegrationTests {
    @LocalServerPort private int port;
    @Autowired private ServletPathProperties paths;
    @Autowired private ObjectMapper json;
    @Autowired private JdbcTemplate db;
    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void successPersistsHeaderItemsAndReservedStockWithSnapshotPrice() throws Exception {
        long first = newSku("2.50", "ON_SALE");
        long second = newSku("3.20", "ON_SALE");
        stock(first, 5);
        stock(second, 6);
        String token = login();

        JsonNode created = data(send(orderRequest(token, """
                {"items":[{"skuId":%d,"quantity":2},{"skuId":%d,"quantity":3}]}
                """.formatted(first, second))));
        long id = created.path("id").asLong();
        assertThat(created.path("status").asString()).isEqualTo("CREATED");
        assertThat(created.path("totalAmount").decimalValue()).isEqualByComparingTo("14.60");
        assertThat(created.path("orderNo").asString()).isNotBlank();
        assertThat(db.queryForObject("SELECT total_amount FROM t_order WHERE id = ?", java.math.BigDecimal.class, id))
                .isEqualByComparingTo("14.60");
        assertThat(db.queryForObject("SELECT COUNT(*) FROM t_order_item WHERE order_id = ?", Integer.class, id))
                .isEqualTo(2);
        assertThat(db.queryForObject("SELECT price FROM t_order_item WHERE order_id = ? AND sku_id = ?",
                java.math.BigDecimal.class, id, first)).isEqualByComparingTo("2.50");
        assertThat(locked(first)).isEqualTo(2);
        assertThat(locked(second)).isEqualTo(3);
        db.update("UPDATE t_sku SET price = 8.00 WHERE id = ?", first);
        assertThat(db.queryForObject("SELECT price FROM t_order_item WHERE order_id = ? AND sku_id = ?",
                java.math.BigDecimal.class, id, first)).isEqualByComparingTo("2.50");
    }

    @Test
    void unavailableLaterSkuRollsBackOrderItemsAndEarlierReservation() throws Exception {
        long first = newSku("1.00", "ON_SALE");
        long second = newSku("1.00", "ON_SALE");
        stock(first, 2);
        stock(second, 0);
        String token = login();
        int before = countOrders();
        int beforeItems = countItems();

        assertError(send(orderRequest(token, """
                {"items":[{"skuId":%d,"quantity":1},{"skuId":%d,"quantity":1}]}
                """.formatted(first, second))), ErrorCode.CONFLICT);
        assertThat(countOrders()).isEqualTo(before);
        assertThat(countItems()).isEqualTo(beforeItems);
        assertThat(locked(first)).isZero();
        assertThat(locked(second)).isZero();
    }

    @Test
    void onlyAuthenticatedUsersCanOrderAndInvalidSkusDoNotWrite() throws Exception {
        long sku = newSku("1.00", "OFF_SALE");
        stock(sku, 5);
        assertError(send(orderRequest(null, """
                {"items":[{"skuId":%d,"quantity":1}]}
                """.formatted(sku))), ErrorCode.UNAUTHORIZED);
        String token = login();
        int before = countOrders();
        assertError(send(orderRequest(token, """
                {"items":[{"skuId":%d,"quantity":1}]}
                """.formatted(sku))), ErrorCode.NOT_FOUND);
        assertError(send(orderRequest(token, """
                {"items":[{"skuId":%d,"quantity":1},{"skuId":%d,"quantity":1}]}
                """.formatted(sku, sku))), ErrorCode.PARAMETER_ERROR);
        assertError(send(orderRequest(token, "{\"items\":[]}")), ErrorCode.PARAMETER_ERROR);
        assertError(send(orderRequest(token, "{\"items\":[null]}")), ErrorCode.PARAMETER_ERROR);
        assertThat(countOrders()).isEqualTo(before);
        assertThat(locked(sku)).isZero();
    }

    @Test
    void competingOrdersCannotReserveMoreThanTotal() throws Exception {
        long sku = newSku("1.00", "ON_SALE");
        stock(sku, 1);
        String token = login();
        int before = countOrders();
        String body = """
                {"items":[{"skuId":%d,"quantity":1}]}
                """.formatted(sku);
        CompletableFuture<HttpResponse<String>> a = client.sendAsync(orderRequest(token, body),
                HttpResponse.BodyHandlers.ofString());
        CompletableFuture<HttpResponse<String>> b = client.sendAsync(orderRequest(token, body),
                HttpResponse.BodyHandlers.ofString());
        assertThat(java.util.List.of(a.join().statusCode(), b.join().statusCode()))
                .containsExactlyInAnyOrder(200, 409);
        assertThat(countOrders()).isEqualTo(before + 1);
        assertThat(locked(sku)).isEqualTo(1);
    }

    @Test
    void openApiContainsOrderCreation() throws Exception {
        JsonNode spec = json.readTree(send(request("/v3/api-docs", null).GET().build()).body());
        assertThat(spec.path("paths").path("/orders").has("post")).isTrue();
    }

    private long newSku(String price, String status) {
        String code = "ORDER-" + UUID.randomUUID();
        Long productId = db.queryForObject("SELECT MIN(id) FROM t_product WHERE status = ?",
                Long.class, status);
        if (productId == null) {
            db.update("INSERT INTO t_product (name, status) VALUES (?, ?)", code, status);
            productId = db.queryForObject("SELECT id FROM t_product WHERE name = ?", Long.class, code);
        }
        db.update("INSERT INTO t_sku (product_id, sku_code, price) VALUES (?, ?, ?)",
                productId, code, new java.math.BigDecimal(price));
        return db.queryForObject("SELECT id FROM t_sku WHERE sku_code = ?", Long.class, code);
    }

    private void stock(long sku, long total) {
        db.update("INSERT INTO t_stock (sku_id, total_count, locked_count) VALUES (?, ?, 0)", sku, total);
    }

    private long locked(long sku) {
        return db.queryForObject("SELECT locked_count FROM t_stock WHERE sku_id = ?", Long.class, sku);
    }

    private int countOrders() {
        return db.queryForObject("SELECT COUNT(*) FROM t_order", Integer.class);
    }

    private int countItems() {
        return db.queryForObject("SELECT COUNT(*) FROM t_order_item", Integer.class);
    }

    private String login() throws Exception {
        String username = "order-" + UUID.randomUUID();
        data(send(request("/auth/register", null).POST(HttpRequest.BodyPublishers.ofString("""
                {"username":"%s","email":"%s@example.com","password":"secret123"}
                """.formatted(username, username))).build()));
        return data(send(request("/auth/login", null).POST(HttpRequest.BodyPublishers.ofString("""
                {"username":"%s","password":"secret123"}
                """.formatted(username))).build())).path("accessToken").asString();
    }

    private HttpRequest orderRequest(String token, String body) {
        return request("/orders", token).POST(HttpRequest.BodyPublishers.ofString(body)).build();
    }

    private HttpRequest.Builder request(String path, String token) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port
                + paths.toExternalPath(path))).header(HttpHeaders.CONTENT_TYPE, "application/json");
        if (token != null) {
            builder.header(HttpHeaders.AUTHORIZATION, BearerAuthentication.SCHEME_PREFIX + token);
        }
        return builder;
    }

    private HttpResponse<String> send(HttpRequest request) throws Exception {
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode data(HttpResponse<String> response) {
        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode body = json.readTree(response.body());
        assertThat(body.path("code").asInt()).isEqualTo(Result.SUCCESS_CODE);
        return body.path("data");
    }

    private void assertError(HttpResponse<String> response, ErrorCode code) {
        assertThat(response.statusCode()).isEqualTo(code.getHttpStatus());
        assertThat(json.readTree(response.body()).path("code").asInt()).isEqualTo(code.getCode());
    }
}
