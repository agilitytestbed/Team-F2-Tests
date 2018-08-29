package nl.utwente.ing;

import io.restassured.path.json.JsonPath;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.nio.file.Paths;
import java.util.Calendar;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchema;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UserMessageTests {

    private static final URI USER_MESSAGE_SCHEMA = Paths.get("src/test/java/nl/utwente/ing/schemas" +
            "/user-message.json").toAbsolutePath().toUri();

    private String sessionId;

    /**
     * Makes sure each test is run with a new session ID.
     */
    @Before
    public void setTestData() {
        sessionId = Util.getSessionID();
    }

    /*
     *  Tests related to GET requests on the /messages API endpoint.
     *  API Documentation:
     *  https://app.swaggerhub.com/apis/INGHonours5/UserMessages/1.0.0#/messages/get_messages
     */

    /**
     * Performs a GET request on the messages API endpoint.
     * <p>
     * This test uses a valid session to get all the created messages and checks if it adheres to the messages
     * format.
     */
    @Test
    public void validSessionUserMessagesGetTest() {
        given()
                .header("X-session-ID", sessionId)
                .get("/api/v1/messages")
                .then()
                .statusCode(200)
                .body(matchesJsonSchema(USER_MESSAGE_SCHEMA));
    }

    /**
     * Performs a GET request on the messages API endpoint.
     * <p>
     * This test uses an invalid session to check whether the API result adheres to the messages format.
     */
    @Test
    public void invalidSessionUserMessagesGetTest() {
        given()
                .get("/api/v1/messages")
                .then()
                .statusCode(401);
    }

    /**
     * Performs a GET and PUT request on the messages API endpoint.
     * <p>
     * This test uses a valid session to test if the API handles the transaction inputs correctly in combination with
     * the user messages.
     */
    @Test
    public void validSessionUserMessagesHighGetTest() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -4);
        Util.insertTransaction(sessionId, "200.00", Util.DATE_FORMAT.format(calendar.getTime()), "deposit", null,
                null);

        calendar = Calendar.getInstance();
        Util.insertTransaction(sessionId, "200.00", Util.DATE_FORMAT.format(calendar.getTime()), "deposit", null,
                null);

        JsonPath response = given()
                .header("X-session-ID", sessionId)
                .get("/api/v1/messages")
                .then()
                .statusCode(200)
                .body(matchesJsonSchema(USER_MESSAGE_SCHEMA))
                .extract()
                .jsonPath();

        assertEquals("info", response.getString("[0].type"));
        Integer messageId = response.getInt("[0].id");

        given()
                .header("X-session-ID", sessionId)
                .put(String.format("/api/v1/messages/%d", messageId))
                .then()
                .statusCode(201);
    }

    /**
     * Performs a GET and PUT request on the messages API endpoint.
     * <p>
     * This test uses a valid session to test if the API handles the transaction inputs correctly in combination with
     * the user messages.
     */
    @Test
    public void validSessionUserMessagesBelowZeroGetTest() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -4);
        Util.insertTransaction(sessionId, "200.00", Util.DATE_FORMAT.format(calendar.getTime()), "deposit", null,
                null);

        calendar = Calendar.getInstance();
        Util.insertTransaction(sessionId, "300.00", Util.DATE_FORMAT.format(calendar.getTime()), "withdrawal", null,
                null);

        JsonPath response = given()
                .header("X-session-ID", sessionId)
                .get("/api/v1/messages")
                .then()
                .statusCode(200)
                .body(matchesJsonSchema(USER_MESSAGE_SCHEMA))
                .extract()
                .jsonPath();

        assertEquals("warning", response.getString("[0].type"));
        Integer messageId = response.getInt("[0].id");

        given()
                .header("X-session-ID", sessionId)
                .put(String.format("/api/v1/messages/%d", messageId))
                .then()
                .statusCode(201);
    }

    /**
     * Performs a GET and PUT request on the messages API endpoint.
     * <p>
     * This test uses a valid session to test if the API handles the Payment request inputs correctly in combination
     * with the user messages.
     */
    @Test
    public void validSessionUserMessagesFilledPaymentRequestGetTest() {
        new PaymentRequestTests().validSessionValidPaymentRequestsCreateTest();

        Calendar calendar = Calendar.getInstance();
        Util.insertTransaction(sessionId, "213.04", Util.DATE_FORMAT.format(calendar.getTime()), "deposit", null,
                null);

        calendar.add(Calendar.HOUR, 1);
        Util.insertTransaction(sessionId, "213.04", Util.DATE_FORMAT.format(calendar.getTime()), "deposit", null,
                null);

        JsonPath response = given()
                .header("X-session-ID", sessionId)
                .get("/api/v1/messages")
                .then()
                .statusCode(200)
                .body(matchesJsonSchema(USER_MESSAGE_SCHEMA))
                .extract()
                .jsonPath();

        assertEquals("info", response.getString("[0].type"));
        Integer messageId = response.getInt("[0].id");

        given()
                .header("X-session-ID", sessionId)
                .put(String.format("/api/v1/messages/%d", messageId))
                .then()
                .statusCode(201);
    }

    /**
     * Performs a GET and PUT request on the messages API endpoint.
     * <p>
     * This test uses a valid session to test if the API handles the Payment request inputs correctly in combination
     * with the user messages.
     */
    @Test
    public void validSessionUserMessagesUnfilledPaymentRequestGetTest() {
        new PaymentRequestTests().validSessionValidPaymentRequestsCreateTest();

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, 1);
        Util.insertTransaction(sessionId, "100.00", Util.DATE_FORMAT.format(calendar.getTime()), "deposit", null,
                null);

        JsonPath response = given()
                .header("X-session-ID", sessionId)
                .get("/api/v1/messages")
                .then()
                .statusCode(200)
                .body(matchesJsonSchema(USER_MESSAGE_SCHEMA))
                .extract()
                .jsonPath();

        assertEquals("warning", response.getString("[0].type"));
        Integer messageId = response.getInt("[0].id");

        given()
                .header("X-session-ID", sessionId)
                .put(String.format("/api/v1/messages/%d", messageId))
                .then()
                .statusCode(201);
    }

    /**
     * Performs a GET and PUT request on the messages API endpoint.
     * <p>
     * This test uses a valid session to test if the API handles the saving goal inputs correctly in combination
     * with the user messages.
     */
    @Test
    public void validSessionUserMessagesSavingGoalGetTest() {
        new SavingGoalsTests().validSessionValidSavingGoalsCreateTest();

        Calendar calendar = Calendar.getInstance();
        Util.insertTransaction(sessionId, "1000.00", Util.DATE_FORMAT.format(calendar.getTime()), "deposit", null,
                null);

        calendar.add(Calendar.MONTH, 3);
        Util.insertTransaction(sessionId, "100.00", Util.DATE_FORMAT.format(calendar.getTime()), "deposit", null,
                null);


        JsonPath response = given()
                .header("X-session-ID", sessionId)
                .get("/api/v1/messages")
                .then()
                .statusCode(200)
                .body(matchesJsonSchema(USER_MESSAGE_SCHEMA))
                .extract()
                .jsonPath();

        assertEquals("info", response.getString("[0].type"));
        Integer messageId = response.getInt("[0].id");

        given()
                .header("X-session-ID", sessionId)
                .put(String.format("/api/v1/messages/%d", messageId))
                .then()
                .statusCode(201);
    }

    /**
     * Performs a PUT request on the messages API endpoint.
     * <p>
     * This test uses an invalid session ID to see if the API handles the response correctly.
     */
    @Test
    public void invalidSessionUserMessagesPutTest() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -4);
        Util.insertTransaction(sessionId, "200.00", Util.DATE_FORMAT.format(calendar.getTime()), "deposit", null,
                null);

        calendar = Calendar.getInstance();
        Util.insertTransaction(sessionId, "200.00", Util.DATE_FORMAT.format(calendar.getTime()), "deposit", null,
                null);

        Integer messageId = given()
                .header("X-session-ID", sessionId)
                .get("/api/v1/messages")
                .then()
                .statusCode(200)
                .body(matchesJsonSchema(USER_MESSAGE_SCHEMA))
                .extract()
                .jsonPath()
                .getInt("[0].id");

        given()
                .put(String.format("/api/v1/messages/%d", messageId))
                .then()
                .statusCode(401);
    }

    /**
     * Performs a PUT request on the messages API endpoint.
     * <p>
     * This test uses an invalid user message ID to see if the API handles the response correctly.
     */
    @Test
    public void validSessionInvalidUserMessagesIdPutTest() {
        given()
                .header("X-session-ID", sessionId)
                .put(String.format("/api/v1/messages/%d", 1000123))
                .then()
                .statusCode(404);
    }
}
