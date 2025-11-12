package uk.gov.hmcts.cp;

import com.auth0.jwt.JWT;
import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import io.restassured.response.Response;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.function.UnaryOperator;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

class PublicEndpointTokenValidationSmokeTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(PublicEndpointTokenValidationSmokeTest.class);

    public static final int JWT_PARTS = 3;
    public static final String SUBS_KEY_HEADER = "Ocp-Apim-Subscription-Key";
    private static String tenantId;
    private static String clientId;
    private static String clientSecret;
    private static String apiAppIdUri;
    private static String wafBaseUrl;
    private static String wafSlug;
    private static String subscriptionKey;

    @BeforeAll
    public static void init() {
        tenantId = Environment.require("TENANT_ID");
        clientId = Environment.require("CLIENT_ID");
        clientSecret = Environment.require("CLIENT_SECRET");
        apiAppIdUri = Environment.require("API_APP_ID_URI");   // e.g. api://my-apim
        wafBaseUrl = Environment.require("WAF_BASE_URL");
        wafSlug = Environment.require("WAF_SLUG");
        subscriptionKey = Environment.require("SUBSCRIPTION_KEY");
        LOGGER.info("Running tests against WAF at {}", wafBaseUrl);

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
        final Response response = given()
                .contentType("application/x-www-form-urlencoded")
                .formParam("client_id", clientId)
                .formParam("client_secret", clientSecret)
                .formParam("grant_type", "client_credentials")
                .formParam("scope", apiAppIdUri + "/.default")
                .post("https://login.microsoftonline.com/" + tenantId + "/oauth2/v2.0/token")
                .then()
                .extract()
                .response();

        assertEquals(200, response.statusCode(), "Token endpoint failed");
        final JSONObject json = new JSONObject(response.asString());
        assertTrue(json.has("access_token"), "Missing access_token");
        return json.getString("access_token");
    }

    private static String base64UrlEncode(final byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static byte[] base64UrlDecode(final String value) {
        return Base64.getUrlDecoder().decode(value);
    }

    private static String mutateJwtPayload(final String jwt, final UnaryOperator<JSONObject> payloadMutator) {
        final String[] parts = jwt.split("\\.");
        if (parts.length != JWT_PARTS) {
            throw new IllegalArgumentException("Invalid JWT format");
        }
        final String header = parts[0];
        final String payload = parts[1];
        final String signature = parts[2];

        final JSONObject payloadJson = new JSONObject(new String(base64UrlDecode(payload), StandardCharsets.UTF_8));
        final JSONObject mutated = payloadMutator.apply(payloadJson);
        final String newPayload = base64UrlEncode(mutated.toString().getBytes(StandardCharsets.UTF_8));

        // Note: Signature will no longer be valid after payload mutation (expected for negative tests)
        return header + "." + newPayload + "." + signature;
    }

    @Test
    void canGetAccessTokenFromEntra() {
        final String accessToken = getAccessTokenFromEntra();
        assertNotNull(accessToken, "Access token should not be null");
        assertFalse(accessToken.isEmpty(), "Access token should not be empty");
    }

    @Test
    void canCallWAFWithBearerToken() {
        final String accessToken = getAccessTokenFromEntra();
        assertNotNull(accessToken, "Access token must be retrieved first");

        // Execute GET request
        final Response response =
                given()
                        .header("Authorization", "Bearer " + accessToken)
                        .header(SUBS_KEY_HEADER, subscriptionKey)
                        .header("Content-Type", "application/json")
                        .when()
                        .get(wafBaseUrl + wafSlug)
                        .then()
                        .extract()
                        .response();

        final int status = response.statusCode();

        // Assert that the status code is one of the expected values
        assertEquals(200, status, "Expected status code 200, but got: " + status + ". Response: " + response.asString());

        // Assert specific conditions based on status
        assertNotNull(response.body(), "Response body should not be null for successful request");
        assertNotNull(response.asString(), "Response body should not be null for successful request");
        assertFalse(response.asString().isEmpty(), "Response body should not be empty for successful request");
    }

    @Test
    void expiredTokenShouldReturn401() {
        final String valid = getAccessTokenFromEntra();
        final String expiredToken = mutateJwtPayload(valid, payload -> {
            // Set exp to 7200 seconds (2 hours) in the past
            final long past = (System.currentTimeMillis() / 1000L) - 7200;
            payload.put("exp", past);
            return payload;
        });

        final boolean isExpired = LocalDateTime.now(ZoneId.systemDefault())
                .isAfter(LocalDateTime.ofInstant(
                        JWT.decode(expiredToken).getExpiresAt().toInstant(),
                        ZoneId.systemDefault())
                );
        assertTrue(isExpired);

        final Response response =
                given()
                        .header("Authorization", "Bearer " + expiredToken)
                        .header(SUBS_KEY_HEADER, subscriptionKey)
                        .when()
                        .get(wafBaseUrl + wafSlug)
                        .then()
                        .extract()
                        .response();

        assertEquals(401, response.statusCode(), "Expected 401 for expired token. Response: " + response.asString());
    }

    @Test
    void missingTokenShouldReturn401() {
        final Response response =
                given()
                        .header(SUBS_KEY_HEADER, subscriptionKey)
                        .when()
                        .get(wafBaseUrl + wafSlug)
                        .then()
                        .log()
                        .all()
                        .extract()
                        .response();

        assertEquals(401, response.statusCode(), "Expected 401 for missing token. Response: " + response.asString());
    }

    @Test
    void mismatchedIssuerShouldReturn401() {
        final String valid = getAccessTokenFromEntra();
        final String badIssuerToken = mutateJwtPayload(valid, payload -> {
            payload.put("iss", "https://invalid-issuer.example.com/");
            return payload;
        });

        final Response response =
                given()
                        .header("Authorization", "Bearer " + badIssuerToken)
                        .header(SUBS_KEY_HEADER, subscriptionKey)
                        .when()
                        .get(wafBaseUrl + wafSlug)
                        .then()
                        .extract()
                        .response();

        assertEquals(401, response.statusCode(), "Expected 401 for mismatched issuer. Response: " + response.asString());
    }
}
