package com.cinetpay;

import com.cinetpay.controller.HealthController;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class HealthControllerTest {

    @Test
    void healthCheckReturnsUp() {
        HealthController controller = new HealthController();
        ResponseEntity<Map<String, Object>> response = controller.healthCheck();

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("UP", response.getBody().get("status"));
        assertEquals("cinet-pay", response.getBody().get("service"));
    }
}
