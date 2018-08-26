package nl.utwente.ing;

import io.restassured.path.json.JsonPath;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URI;
import java.nio.file.Paths;
import java.util.Calendar;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchema;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PaymentRequestTests {

    private static final URI PAYMENT_REQUEST_SCHEMA = Paths.get
            ("src/test/java/nl/utwente/ing/schemas/paymentrequests/payment-request.json").toAbsolutePath().toUri();
    private static final URI PAYMENT_REQUEST_LIST_SCHEMA = Paths.get
            ("src/test/java/nl/utwente/ing/schemas/paymentrequests/payment-request-list.json").toAbsolutePath().toUri();

    private static String sessionId;
    private Integer paymentRequestId;

    /**
     * Makes sure each test is run with a new session ID.
     */
    @BeforeClass
    public static void setTestData() {
        sessionId = Util.getSessionID();
    }

    /*
     *  Tests related to POST requests on the /paymentRequests API endpoint.
     *  API Documentation:
     *  https://app.swaggerhub.com/apis/djhuistra/INGHonours-SavingsGoals/1.0#/paymentRequests/addpaymentRequest
     */
    /**
     *  Performs a POST request on the PaymentRequests API endpoint.
     *
     *  This test uses a valid session to check whether the API result adheres to the paymentRequest format.
     */
    @Test
    public void validSessionValidPaymentRequestsCreateTest() {
        String validPaymentRequest = "{\n" +
                "  \"description\": \"Payback fo rlunch\",\n" +
                "  \"due_date\": \"2018-08-22T16:09:32.998Z\",\n" +
                "  \"amount\": 213.04,\n" +
                "  \"number_of_requests\": 2\n" +
                "}";

        paymentRequestId = given()
                .header("X-session-ID", sessionId)
                .body(validPaymentRequest)
                .post("/api/v1/paymentRequests")
                .then()
                .statusCode(201)
                .body(matchesJsonSchema(PAYMENT_REQUEST_SCHEMA))
                .extract()
                .jsonPath()
                .getInt("id");
    }

    /**
     * Performs a POST request on the PaymentRequests API endpoint.
     *
     * This test uses an invalid PaymentRequest to test whether the API result adheres to the paymentRequest format.
     */
    @Test
    public void validSessionInvalidPaymentRequestsCreateTest() {
        String invalidPaymentRequest = "{\n" +
                "  \"description\": \"Payback fo rlunch\",\n" +
                "  \"amount\": 213.04,\n" +
                "  \"number_of_requests\": 2\n" +
                "}";
        given()
                .header("X-session-ID", sessionId)
                .body(invalidPaymentRequest)
                .post("/api/v1/paymentRequests")
                .then()
                .statusCode(405);
    }

    /**
     * Performs a POST request on the PaymentRequests API endpoint.
     *
     * This test uses an invalid session to test whether the API result adheres to the paymentRequest format.
     */
    @Test
    public void invalidSessionValidPaymentRequestsCreateTest() {
        String validPaymentRequest = "{\n" +
                "  \"name\": \"China holiday\",\n" +
                "  \"goal\": 5000,\n" +
                "  \"savePerMonth\": 250,\n" +
                "  \"minBalanceRequired\": 0\n" +
                "}";

        given()
                .body(validPaymentRequest)
                .post("/api/v1/paymentRequests")
                .then()
                .statusCode(401);
    }

    /*
     *  Tests related to GET requests on the /paymentRequests API endpoint.
     *  API Documentation:
     *  https://app.swaggerhub.com/apis/djhuistra/INGHonours-SavingsGoals/1.0#/paymentRequests/getPaymentRequests
     */
    /**
     * Performs a GET request on the paymentRequests API endpoint.
     *
     * This test uses a valid session to get all the created paymentRequests and checks if it adheres to the paymentRequests
     * format.
     */
    @Test
    public void validSessionPaymentRequestsGetTest() {
        //Make sure there is at least one paymentRequest to retrieve.
        validSessionValidPaymentRequestsCreateTest();

        given()
                .header("X-session-ID", sessionId)
                .get("/api/v1/paymentRequests")
                .then()
                .statusCode(200)
                .body(matchesJsonSchema(PAYMENT_REQUEST_LIST_SCHEMA));
    }

    /**
     * Performs a GET request on the paymentRequests API endpoint.
     *
     * This test uses an invalid session to check whether the API result adheres to the paymentRequests format.
     */
    @Test
    public void invalidSessionPaymentRequestsGetTest() {
        given()
                .get("/api/v1/paymentRequests")
                .then()
                .statusCode(401);
    }

    /**
     * Performs a GET request on the paymentRequests API endpoint.
     *
     * This test uses a valid session to test if the API handles the transaction inputs correctly in combination with
     * the saving goals.
     */
    @Test
    public void validSessionPaymentRequestsCalculationGetTest() {
        validSessionValidPaymentRequestsCreateTest();

        System.out.println(Util.insertTransaction(sessionId, "213.04", "2018-08-21T16:18:36.915Z", "deposit", null,
                null));
        Util.insertTransaction(sessionId, "100.04", "2018-08-23T16:18:36.915Z", "deposit", null, null);
        Util.insertTransaction(sessionId, "212.04", "2018-08-24T16:18:36.915Z", "deposit", null, null);
        Util.insertTransaction(sessionId, "213.04", "2018-08-20T16:18:36.915Z", "deposit", null, null);

        JsonPath response = given()
                .header("X-session-ID", sessionId)
                .get("/api/v1/paymentRequests")
                .then()
                .statusCode(200)
                .body(matchesJsonSchema(PAYMENT_REQUEST_LIST_SCHEMA))
                .extract()
                .jsonPath();
        System.out.println(sessionId);

        System.out.println(response.getString(""));
        assertTrue(response.getBoolean("[0].filled"));
    }

}
