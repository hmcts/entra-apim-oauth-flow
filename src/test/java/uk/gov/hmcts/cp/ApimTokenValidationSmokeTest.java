package uk.gov.hmcts.cp;

import com.auth0.jwt.JWT;
import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import io.restassured.response.Response;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.UnaryOperator;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

public class ApimTokenValidationSmokeTest {

    private static String tenantId;
    private static String clientId;
    private static String clientSecret;
    private static String apiAppIdUri;
    private static String apimBaseUrl;
    private static String slug;

    private static Optional<String> subscriptionKey;

    @BeforeAll
    static void init() {
        tenantId = Environment.require("TENANT_ID");
        clientId = Environment.require("CLIENT_ID");
        clientSecret = Environment.require("CLIENT_SECRET");
        apiAppIdUri = Environment.require("API_APP_ID_URI");   // e.g. api://my-apim
        apimBaseUrl = Environment.require("APIM_BASE_URL");     // e.g. https://myapim.azure-api.net
        slug = Environment.require("SLUG");
        subscriptionKey = Optional.ofNullable(System.getenv("SUBSCRIPTION_KEY")); // optional
        System.getenv().forEach((key, value) ->
                System.out.println(key + " = " + value)
        );

        // Configure SSL to use relaxed HTTPS validation for testing
        // This accepts self-signed certificates and handles certificate chain issues
        RestAssured.config = RestAssuredConfig.config()
                .sslConfig(SSLConfig.sslConfig().relaxedHTTPSValidation());

        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    /**
     * Retrieves an access token from Entra ID (Azure AD).
     *
     * @return The access token string
     */
    private String getAccessTokenFromEntra() {
        Response response = given()
                .log()
                .all()
                .contentType("application/x-www-form-urlencoded")
                .formParam("client_id", clientId)
                .formParam("client_secret", clientSecret)
                .formParam("grant_type", "client_credentials")
                .formParam("scope", apiAppIdUri + "/.default")
                .post("https://login.microsoftonline.com/" + tenantId + "/oauth2/v2.0/token")
                .then()
                .log()
                .all()
                .extract()
                .response();

        assertEquals(200, response.statusCode(), "Token endpoint failed");
        JSONObject json = new JSONObject(response.asString());
        assertTrue(json.has("access_token"), "Missing access_token");
        String accessToken = json.getString("access_token");
        return accessToken;
    }

    private static String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static byte[] base64UrlDecode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }

    private static String mutateJwtPayload(String jwt, UnaryOperator<JSONObject> payloadMutator) {
        String[] parts = jwt.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWT format");
        }
        String header = parts[0];
        String payload = parts[1];
        String signature = parts[2];

        JSONObject payloadJson = new JSONObject(new String(base64UrlDecode(payload), StandardCharsets.UTF_8));
        JSONObject mutated = payloadMutator.apply(payloadJson);
        String newPayload = base64UrlEncode(mutated.toString().getBytes(StandardCharsets.UTF_8));

        // Note: Signature will no longer be valid after payload mutation (expected for negative tests)
        return header + "." + newPayload + "." + signature;
    }

    @Test
    void canGetAccessTokenFromEntra() {
        String accessToken = getAccessTokenFromEntra();
        assertNotNull(accessToken, "Access token should not be null");
        assertFalse(accessToken.isEmpty(), "Access token should not be empty");
    }

    @Test
    void canCallAPIMWithBearerToken() {
        String accessToken = getAccessTokenFromEntra();
        assertNotNull(accessToken, "Access token must be retrieved first");

        // Execute GET request
        Response response =
                given()
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Ocp-Apim-Subscription-Key", subscriptionKey.orElse(null))
                        .header("Content-Type", "application/json")
                        .log()
                        .all()
                        .when()
                        .get(apimBaseUrl + slug)
                        .then()
                        .log().all()
                        .extract()
                        .response();

        int status = response.statusCode();

        // Assert that the status code is one of the expected values
        assertEquals(200, status, "Expected status code 200, but got: " + status + ". Response: " + response.asString());

        // Assert specific conditions based on status
        assertNotNull(response.body(), "Response body should not be null for successful request");
        assertNotNull(response.asString(), "Response body should not be null for successful request");
        assertFalse(response.asString().isEmpty(), "Response body should not be empty for successful request");
    }

    @Test
    void expiredTokenShouldReturn401() {
        String valid = getAccessTokenFromEntra();
        String expiredToken = mutateJwtPayload(valid, payload -> {
            // Set exp to 10 seconds in the past
            long past = (System.currentTimeMillis() / 1000L) - 7200;
            payload.put("exp", past);
            return payload;
        });

        assertTrue(JWT.decode(expiredToken).getExpiresAt().before(new Date()));

        Response response =
                given()
                        .header("Authorization", "Bearer " + expiredToken)
                        .header("Ocp-Apim-Subscription-Key", subscriptionKey.orElse(null))
                        .log().all()
                        .when()
                        .get(apimBaseUrl + slug)
                        .then()
                        .log().all()
                        .extract()
                        .response();

        assertEquals(401, response.statusCode(), "Expected 401 for expired token. Response: " + response.asString());
    }

    @Test
    void mismatchedIssuerShouldReturn401() {
        String valid = getAccessTokenFromEntra();
        String badIssuerToken = mutateJwtPayload(valid, payload -> {
            payload.put("iss", "https://invalid-issuer.example.com/");
            return payload;
        });

        Response response =
                given()
                        .header("Authorization", "Bearer " + badIssuerToken)
                        .header("Ocp-Apim-Subscription-Key", subscriptionKey.orElse(null))
                        .log().all()
                        .when()
                        .get(apimBaseUrl + slug)
                        .then()
                        .log().all()
                        .extract()
                        .response();

        assertEquals(401, response.statusCode(), "Expected 401 for mismatched issuer. Response: " + response.asString());
    }
}
