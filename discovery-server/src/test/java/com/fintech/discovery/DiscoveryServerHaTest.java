package com.fintech.discovery;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HA integration tests for the Discovery Server.
 *
 * Overrides Eureka client properties to simulate a multi-peer HA setup.
 * Validates security enforcement, health endpoint availability,
 * and peer replication URL format.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "eureka.instance.hostname=discovery-server-1",
                "eureka.client.register-with-eureka=true",
                "eureka.client.fetch-registry=true",
                "eureka.client.service-url.defaultZone=http://eureka:password@discovery-server-2:8761/eureka/",
                "eureka.server.enable-self-preservation=true",
                "eureka.server.renewal-percent-threshold=0.85"
        }
)
@ActiveProfiles("dev")
class DiscoveryServerHaTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private EurekaClientConfigBean eurekaClientConfig;

    @Value("${app.security.username}")
    private String username;

    @Value("${app.security.password}")
    private String password;

    // --- Health Endpoint Tests ---

    @Test
    void healthEndpointReturnsUp() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    // --- Security Tests ---

    @Test
    void eurekaAppsReturns401WithoutBasicAuth() {
        ResponseEntity<String> response = restTemplate.getForEntity("/eureka/apps", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void eurekaAppsReturns200WithValidBasicAuth() {
        ResponseEntity<String> response = restTemplate
                .withBasicAuth(username, password)
                .getForEntity("/eureka/apps", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // --- Peer Replication URL Format Tests ---

    @Test
    void peerReplicationUrlContainsCredentials() {
        Map<String, String> serviceUrls = eurekaClientConfig.getServiceUrl();
        String defaultZone = serviceUrls.get("defaultZone");

        assertThat(defaultZone).isNotNull();
        // Peer URL must contain Basic Auth credentials in the URL
        assertThat(defaultZone).containsPattern("http://[^:]+:[^@]+@");
    }

    @Test
    void peerReplicationUrlEndsWithEurekaPath() {
        Map<String, String> serviceUrls = eurekaClientConfig.getServiceUrl();
        String defaultZone = serviceUrls.get("defaultZone");

        assertThat(defaultZone).isNotNull();
        // Each peer URL must end with /eureka/ path
        String[] urls = defaultZone.split(",");
        for (String url : urls) {
            assertThat(url.trim()).endsWith("/eureka/");
        }
    }

    @Test
    void peerReplicationUrlPointsToPeerHost() {
        Map<String, String> serviceUrls = eurekaClientConfig.getServiceUrl();
        String defaultZone = serviceUrls.get("defaultZone");

        assertThat(defaultZone).isNotNull();
        // In HA mode, the peer URL should reference a different discovery-server instance
        assertThat(defaultZone).contains("discovery-server-2");
    }

    @Test
    void haConfigEnablesRegistrationAndFetching() {
        // In HA mode, instances must register with peers and fetch their registries
        assertThat(eurekaClientConfig.shouldRegisterWithEureka()).isTrue();
        assertThat(eurekaClientConfig.shouldFetchRegistry()).isTrue();
    }
}
