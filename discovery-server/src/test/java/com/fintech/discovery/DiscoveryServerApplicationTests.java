package com.fintech.discovery;

import com.netflix.eureka.EurekaServerContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
class DiscoveryServerApplicationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private EurekaServerContext eurekaServerContext;

    @Autowired
    private EurekaClientConfigBean eurekaClientConfig;

    @Value("${app.security.username}")
    private String username;

    @Value("${app.security.password}")
    private String password;

    @Test
    void contextLoads() {
        // Verifies Eureka Server starts successfully in test context
    }

    @Test
    void contextLoadsWithStandaloneProfile() {
        // Dev profile runs as standalone: no self-registration, no registry fetching
        assertThat(eurekaClientConfig.shouldRegisterWithEureka()).isFalse();
        assertThat(eurekaClientConfig.shouldFetchRegistry()).isFalse();
    }

    @Test
    void eurekaServerContextIsAvailable() {
        assertThat(eurekaServerContext).isNotNull();
    }

    @Test
    void healthEndpointReturnsUp() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    void dashboardRequiresAuth() {
        ResponseEntity<String> response = restTemplate.getForEntity("/", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void dashboardAccessibleWithAuth() {
        ResponseEntity<String> response = restTemplate
                .withBasicAuth(username, password)
                .getForEntity("/", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void eurekaApiRequiresAuth() {
        ResponseEntity<String> response = restTemplate.getForEntity("/eureka/apps", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void eurekaApiAccessibleWithAuth() {
        ResponseEntity<String> response = restTemplate
                .withBasicAuth(username, password)
                .getForEntity("/eureka/apps", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
