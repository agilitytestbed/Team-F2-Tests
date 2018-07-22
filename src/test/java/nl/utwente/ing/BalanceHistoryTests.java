package nl.utwente.ing;

import io.restassured.http.ContentType;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.nio.file.Paths;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchema;

public class BalanceHistoryTests {

    private static final URI BALANCE_HISTORY_SCHEMA = Paths.get
            ("src/test/java/nl/utwente/ing/schemas/balance-history.json").toAbsolutePath().toUri();

    private static String sessionId;

    /**
     * Makes sure all tests share the same session ID by setting sessionId if it does not exist yet.
     */
    @Before
    public void getTestSession() {
        if (sessionId == null) {
            sessionId = Util.getSessionID();
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


}
