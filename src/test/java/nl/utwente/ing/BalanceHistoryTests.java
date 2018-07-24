package nl.utwente.ing;

import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import org.junit.*;

import java.net.URI;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchema;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class BalanceHistoryTests {

    private static final URI BALANCE_HISTORY_SCHEMA = Paths.get
            ("src/test/java/nl/utwente/ing/schemas/balance-history.json").toAbsolutePath().toUri();

    private static final String TRANSACTION_INPUT_FORMAT =
            "{" +
                    "\"date\": \"%s\", " +
                    "\"amount\": %f, " +
                    "\"externalIBAN\": \"NL05INGB0374182583\", " +
                    "\"type\": \"%s\", " +
                    "\"description\": \"test\"" +
                    "}";
    private static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    private static String sessionId;

    private List<Integer> transactions = new ArrayList<>();

    /**
     * Makes sure all tests share the same session ID by setting sessionId if it does not exist yet.
     */
    @BeforeClass
    public static void getTestSession() {
        if (sessionId == null) {
            sessionId = Util.getSessionID();
        }
    }

    /**
     * Makes sure all test data is removed after all the tests are run.
     */
    @After
    public void removeTestData() {
        for (int transactionId : transactions) {
            Util.deleteTestTransaction(transactionId, sessionId);
        }
    }

    /**
     * Performs a GET request on the balanceHistory endpoint.
     *
     * This test uses a valid session to check whether the API result adheres to the balanceHistory format.
     */
    @Test
    public void validSessionBalanceHistoryTest() {
        given()
                .header("X-session-ID", sessionId)
                .get("/api/v1/balance/history")
                .then()
                .assertThat()
                .statusCode(200)
                .body(matchesJsonSchema(BALANCE_HISTORY_SCHEMA))
                .contentType(ContentType.JSON);
    }

    /**
     * Performs a GET request on the balanceHistory endpoint.
     *
     * This test uses an invalid session to check whether the API result adheres to the balanceHistory format.
     */
    @Test
    public void invalidSessionBalanceHistoryTest() {
        given()
                .get("/api/v1/balance/history")
                .then()
                .assertThat()
                .statusCode(401);
    }

    /**
     * Performs a GET request on the balanceHistory endpoint.
     *
     * This test uses a valid session and invalid parameter to check whether the API result adheres to the
     * balanceHistory format.
     */
    @Test
    public void validSessionInvalidParameterBalanceHistoryTest() {
        given()
                .header("X-session-ID", sessionId)
                .queryParam("interval", "wrong")
                .get("/api/v1/balance/history")
                .then()
                .assertThat()
                .statusCode(405);
    }

    /**
     * Performs a GET request on the balanceHistory endpoint.
     *
     * This test uses a valid session with transactions to check whether the API result adheres to the
     * balanceHistory format.
     */
    @Test
    public void validSessionValidTransactionsBalanceHistoryTest() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -3);
        insertTransaction(sessionId, 20000L, DATE_FORMAT.format(calendar.getTime()), "deposit");

        calendar = Calendar.getInstance();
        insertTransaction(sessionId, 50000L, DATE_FORMAT.format(calendar.getTime()), "deposit");

        calendar = Calendar.getInstance();
        insertTransaction(sessionId, 10000L, DATE_FORMAT.format(calendar.getTime()), "withdrawal");

        JsonPath response = given()
                .header("X-session-ID", sessionId)
                .queryParam("intervals", 1)
                .get("/api/v1/balance/history")
                .then()
                .assertThat()
                .statusCode(200)
                .body(matchesJsonSchema(BALANCE_HISTORY_SCHEMA))
                .extract()
                .response()
                .getBody()
                .jsonPath()
                .get("[0]");

        assertThat(response.get("open"), equalTo(200.00));
        assertThat(response.get("close"), equalTo(600.00));
        assertThat(response.get("high"), equalTo(700.00));
        assertThat(response.get("low"), equalTo(200.00));
        assertThat(response.get("volume"), equalTo(600.00));
    }

    /**
     * Performs a GET request on the balanceHistory endpoint.
     *
     * This test uses a valid session with transactions to check whether the API result adheres to the
     * balanceHistory format.
     */
    @Test
    public void validSessionValidTransactionsValidIntervalBalanceHistoryTest() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -1);
        insertTransaction(sessionId, 40000L, DATE_FORMAT.format(calendar.getTime()), "deposit");

        calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -1);
        insertTransaction(sessionId, 10000L, DATE_FORMAT.format(calendar.getTime()), "withdrawal");


        calendar = Calendar.getInstance();
        insertTransaction(sessionId, 20000L, DATE_FORMAT.format(calendar.getTime()), "deposit");

        calendar = Calendar.getInstance();
        insertTransaction(sessionId, 5000L, DATE_FORMAT.format(calendar.getTime()), "withdrawal");

        JsonPath response = given()
                .header("X-session-ID", sessionId)
                .queryParam("interval", "week")
                .queryParam("intervals", 1)
                .get("/api/v1/balance/history")
                .then()
                .assertThat()
                .statusCode(200)
                .body(matchesJsonSchema(BALANCE_HISTORY_SCHEMA))
                .extract()
                .response()
                .getBody()
                .jsonPath()
                .get("[0]");

        System.out.println(response.toString());
        assertThat(response.get("open"), equalTo(300.00));
        assertThat(response.get("close"), equalTo(450.00));
        assertThat(response.get("high"), equalTo(500.00));
        assertThat(response.get("low"), equalTo(300.00));
        assertThat(response.get("volume"), equalTo(250.00));
    }


    /**
     * Helper function to accompany easy transaction creation.
     * @param sessionId The session ID which needs to be used to create the transaction.
     * @param amount Amount the transaction needs to be.
     * @param date Date which the transaction was done.
     * @param type Either deposit or withdrawal.
     */
    private void insertTransaction(String sessionId, Long amount, String date, String type) {
        System.out.println(String.format(TRANSACTION_INPUT_FORMAT, date, amount / 100.0, type));

        transactions.add(given()
                .header("X-session-ID", sessionId)
                .body(String.format(TRANSACTION_INPUT_FORMAT, date, amount / 100.0, type))
                .post("/api/v1/transactions")
                .then()
                .assertThat()
                .statusCode(201)
                .extract()
                .response()
                .getBody()
                .jsonPath()
                .getInt("id"));
    }
}


