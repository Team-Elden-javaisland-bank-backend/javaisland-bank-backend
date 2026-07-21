package com.javaisland.bank_backend.auth.service;

import com.javaisland.bank_backend.exception.ApiBankException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class KeycloakAdminService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${keycloak.auth-server-url}")
    private String keycloakAuthUrl;

    @Value("${keycloak.realm}")
    private String keycloakRealm;

    @Value("${keycloak.admin-username}")
    private String adminUsername;

    @Value("${keycloak.admin-password}")
    private String adminPassword;

    private String getAdminToken() {
        String tokenUrl = keycloakAuthUrl + "/realms/master/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", "admin-cli");
        body.add("username", adminUsername);
        body.add("password", adminPassword);
        body.add("grant_type", "password");

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.exchange(tokenUrl, HttpMethod.POST, entity, Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new ApiBankException("Impossibile autenticarsi come admin Keycloak.", "KEYCLOAK_ADMIN_AUTH_FAILED");
        }

        return (String) response.getBody().get("access_token");
    }

    public String createUser(String username, String password, String email, String firstName, String lastName, boolean enabled) {
        String adminToken = getAdminToken();
        String createUrl = keycloakAuthUrl + "/admin/realms/" + keycloakRealm + "/users";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        Map<String, Object> credentials = new LinkedHashMap<>();
        credentials.put("type", "password");
        credentials.put("value", password);
        credentials.put("temporary", false);

        Map<String, Object> userBody = new LinkedHashMap<>();
        userBody.put("username", username);
        userBody.put("email", email);
        userBody.put("firstName", firstName);
        userBody.put("lastName", lastName);
        userBody.put("enabled", enabled);
        userBody.put("emailVerified", true);
        userBody.put("credentials", Collections.singletonList(credentials));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(userBody, headers);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(createUrl, HttpMethod.POST, entity, Void.class);

            String location = response.getHeaders().getLocation().toString();
            String keycloakId = location.substring(location.lastIndexOf('/') + 1);

            log.info("Keycloak user created: {} (id={})", username, keycloakId);

            assignRealmRole(keycloakId, "C", adminToken);

            return keycloakId;

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                throw new ApiBankException(
                        "Utente già registrato su Keycloak con questa email.", "KEYCLOAK_USER_EXISTS");
            }
            log.error("Keycloak create user failed: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ApiBankException("Creazione utente Keycloak fallita.", "KEYCLOAK_USER_CREATION_FAILED");
        }
    }

    public void setUserEnabled(String keycloakId, boolean enabled) {
        String adminToken = getAdminToken();
        String updateUrl = keycloakAuthUrl + "/admin/realms/" + keycloakRealm + "/users/" + keycloakId;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("enabled", enabled);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            restTemplate.exchange(updateUrl, HttpMethod.PUT, entity, Void.class);
            log.info("Keycloak user {} set to enabled={}", keycloakId, enabled);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.warn("Keycloak user {} not found, cannot set enabled={}", keycloakId, enabled);
            } else {
                log.error("Failed to update Keycloak user {}: status={}", keycloakId, e.getStatusCode());
            }
        }
    }

    public void assignRole(String keycloakId, String roleName) {
        String adminToken = getAdminToken();
        assignRealmRole(keycloakId, roleName, adminToken);
    }

    private void assignRealmRole(String keycloakId, String roleName, String adminToken) {
        String roleUrl = keycloakAuthUrl + "/admin/realms/" + keycloakRealm + "/roles/" + roleName;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Void> getEntity = new HttpEntity<>(headers);
        ResponseEntity<Map> roleResponse = restTemplate.exchange(roleUrl, HttpMethod.GET, getEntity, Map.class);

        if (!roleResponse.getStatusCode().is2xxSuccessful() || roleResponse.getBody() == null) {
            log.warn("Cannot assign role {}: role not found in Keycloak", roleName);
            return;
        }

        String roleId = (String) roleResponse.getBody().get("id");
        String roleNameFromResponse = (String) roleResponse.getBody().get("name");

        Map<String, Object> roleMapping = new LinkedHashMap<>();
        roleMapping.put("id", roleId);
        roleMapping.put("name", roleNameFromResponse);

        String mappingUrl = keycloakAuthUrl + "/admin/realms/" + keycloakRealm
                + "/users/" + keycloakId + "/role-mappings/realm";

        HttpEntity<List<Map<String, Object>>> postEntity = new HttpEntity<>(List.of(roleMapping), headers);
        restTemplate.exchange(mappingUrl, HttpMethod.POST, postEntity, Void.class);

        log.info("Assigned realm role '{}' to Keycloak user {}", roleName, keycloakId);
    }

    public void resetPassword(String keycloakId, String newPassword) {
        String adminToken = getAdminToken();
        String resetUrl = keycloakAuthUrl + "/admin/realms/" + keycloakRealm
                + "/users/" + keycloakId + "/reset-password";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "password");
        body.put("value", newPassword);
        body.put("temporary", false);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            restTemplate.exchange(resetUrl, HttpMethod.PUT, entity, Void.class);
            log.info("Keycloak password reset for user {}", keycloakId);
        } catch (HttpClientErrorException e) {
            log.error("Failed to reset Keycloak password for {}: status={}", keycloakId, e.getStatusCode());
            throw new ApiBankException("Reset password Keycloak fallito.", "KEYCLOAK_PASSWORD_RESET_FAILED");
        }
    }

    public void deleteUser(String keycloakId) {
        String adminToken = getAdminToken();
        String deleteUrl = keycloakAuthUrl + "/admin/realms/" + keycloakRealm + "/users/" + keycloakId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            restTemplate.exchange(deleteUrl, HttpMethod.DELETE, entity, Void.class);
            log.info("Keycloak user deleted: {}", keycloakId);
        } catch (HttpClientErrorException e) {
            log.warn("Failed to delete Keycloak user {}: status={}", keycloakId, e.getStatusCode());
        }
    }

    public void logoutUser(String keycloakId) {
        String adminToken = getAdminToken();
        String logoutUrl = keycloakAuthUrl + "/admin/realms/" + keycloakRealm
                + "/users/" + keycloakId + "/logout";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            restTemplate.exchange(logoutUrl, HttpMethod.POST, entity, Void.class);
            log.info("Keycloak sessions invalidated for user {}", keycloakId);
        } catch (HttpClientErrorException e) {
            log.warn("Failed to logout Keycloak user {}: status={}", keycloakId, e.getStatusCode());
        }
    }
}
