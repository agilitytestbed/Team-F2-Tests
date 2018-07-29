/*
 * Copyright (c) 2018, Joost Prins <github.com/joostprins> All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package nl.utwente.ing;

import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URI;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchema;
import static org.junit.Assert.assertEquals;

public class BalanceHistoryTests {

    private static final URI BALANCE_HISTORY_SCHEMA = Paths.get
            ("src/test/java/nl/utwente/ing/schemas/balance-history.json").toAbsolutePath().toUri();

    private static final String TRANSACTION_INPUT_FORMAT =
            "{" +
                    "\"date\": \"%s\", " +
                    "\"amount\": %s, " +
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
        insertTransaction(sessionId, "200.00", DATE_FORMAT.format(calendar.getTime()), "deposit");

        calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, -2);
        insertTransaction(sessionId, "500.00", DATE_FORMAT.format(calendar.getTime()), "deposit");

        calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, -2);
        insertTransaction(sessionId, "100.00", DATE_FORMAT.format(calendar.getTime()), "withdrawal");

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
                .jsonPath();

        assertEquals( 200.00, response.getDouble("[0].open"), 0.01);
        assertEquals(600.00, response.getDouble("[0].close"), 0.01);
        assertEquals(700.00, response.getDouble("[0].high"), 0.01);
        assertEquals(200.00, response.getDouble("[0].low"), 0.01);
        assertEquals(600.00, response.getDouble("[0].volume"), 0.01);
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
        insertTransaction(sessionId, "400.00", DATE_FORMAT.format(calendar.getTime()), "deposit");

        calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -1);
        insertTransaction(sessionId, "100.00", DATE_FORMAT.format(calendar.getTime()), "withdrawal");


        calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, -2);
        insertTransaction(sessionId, "200.00", DATE_FORMAT.format(calendar.getTime()), "deposit");

        calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, -2);
        insertTransaction(sessionId, "50.00", DATE_FORMAT.format(calendar.getTime()), "withdrawal");

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
                .jsonPath();

        assertEquals( 300.00, response.getDouble("[0].open"), 0.01);
        assertEquals(450.00, response.getDouble("[0].close"), 0.01);
        assertEquals(500.00, response.getDouble("[0].high"), 0.01);
        assertEquals(300.00, response.getDouble("[0].low"), 0.01);
        assertEquals(250.00, response.getDouble("[0].volume"), 0.01);
    }

    /**
     * Performs a GET request on the balanceHistory endpoint.
     *
     * This test uses a valid session with transactions  with multiple intervals to check whether the API result
     * adheres to the balanceHistory format.
     */
    @Test
    public void validSessionValidTransactionsValidIntervalMultipleIntervalsBalanceHistoryTest() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, -10);
        insertTransaction(sessionId, "400.00", DATE_FORMAT.format(calendar.getTime()), "deposit");

        calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, -10);
        insertTransaction(sessionId, "100.00", DATE_FORMAT.format(calendar.getTime()), "withdrawal");

        calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, -4);
        calendar.add(Calendar.HOUR, -2);
        insertTransaction(sessionId, "200.00", DATE_FORMAT.format(calendar.getTime()), "deposit");

        calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, -4);
        calendar.add(Calendar.HOUR, -2);
        insertTransaction(sessionId, "50.00", DATE_FORMAT.format(calendar.getTime()), "withdrawal");

        calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, -3);
        calendar.add(Calendar.HOUR, -2);
        insertTransaction(sessionId, "200.00", DATE_FORMAT.format(calendar.getTime()), "deposit");

        calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, -3);
        calendar.add(Calendar.HOUR, -2);
        insertTransaction(sessionId, "50.00", DATE_FORMAT.format(calendar.getTime()), "withdrawal");

        calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, -2);
        calendar.add(Calendar.HOUR, -2);
        insertTransaction(sessionId, "200.00", DATE_FORMAT.format(calendar.getTime()), "deposit");

        calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, -2);
        calendar.add(Calendar.HOUR, -2);
        insertTransaction(sessionId, "50.00", DATE_FORMAT.format(calendar.getTime()), "withdrawal");

        calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, -2);
        insertTransaction(sessionId, "200.00", DATE_FORMAT.format(calendar.getTime()), "deposit");

        calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, -2);
        insertTransaction(sessionId, "50.00", DATE_FORMAT.format(calendar.getTime()), "withdrawal");

        JsonPath response = given()
                .header("X-session-ID", sessionId)
                .queryParam("interval", "year")
                .queryParam("intervals", 5)
                .get("/api/v1/balance/history")
                .then()
                .assertThat()
                .statusCode(200)
                .body(matchesJsonSchema(BALANCE_HISTORY_SCHEMA))
                .extract()
                .response()
                .getBody()
                .jsonPath();

        assertEquals( 300.00, response.getDouble("[0].open"), 0.01);
        assertEquals(450.00, response.getDouble("[0].close"), 0.01);
        assertEquals(500.00, response.getDouble("[0].high"), 0.01);
        assertEquals(300.00, response.getDouble("[0].low"), 0.01);
        assertEquals(250.00, response.getDouble("[0].volume"), 0.01);

        assertEquals( 450.00, response.getDouble("[1].open"), 0.01);
        assertEquals(600.00, response.getDouble("[1].close"), 0.01);
        assertEquals(650.00, response.getDouble("[1].high"), 0.01);
        assertEquals(450.00, response.getDouble("[1].low"), 0.01);
        assertEquals(250.00, response.getDouble("[1].volume"), 0.01);

        assertEquals( 600.00, response.getDouble("[2].open"), 0.01);
        assertEquals(750.00, response.getDouble("[2].close"), 0.01);
        assertEquals(800.00, response.getDouble("[2].high"), 0.01);
        assertEquals(600.00, response.getDouble("[2].low"), 0.01);
        assertEquals(250.00, response.getDouble("[2].volume"), 0.01);

        assertEquals( 750.00, response.getDouble("[4].open"), 0.01);
        assertEquals(900.00, response.getDouble("[4].close"), 0.01);
        assertEquals(950.00, response.getDouble("[4].high"), 0.01);
        assertEquals(750.00, response.getDouble("[4].low"), 0.01);
        assertEquals(250.00, response.getDouble("[4].volume"), 0.01);
    }


        /**
         * Helper function to accompany easy transaction creation.
         * @param sessionId The session ID which needs to be used to create the transaction.
         * @param amount Amount the transaction needs to be.
         * @param date Date which the transaction was done.
         * @param type Either deposit or withdrawal.
         */
    private void insertTransaction(String sessionId, String amount, String date, String type) {
        transactions.add(given()
                .header("X-session-ID", sessionId)
                .body(String.format(TRANSACTION_INPUT_FORMAT, date, amount, type))
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


