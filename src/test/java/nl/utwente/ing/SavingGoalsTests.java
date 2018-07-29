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

import io.restassured.path.json.JsonPath;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.nio.file.Paths;
import java.util.Calendar;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchema;
import static org.junit.Assert.assertEquals;

public class SavingGoalsTests {

    private static final URI SAVING_GOAL_SCHEMA = Paths.get
            ("src/test/java/nl/utwente/ing/schemas/savinggoals/saving-goal.json").toAbsolutePath().toUri();
    private static final URI SAVING_GOAL_LIST_SCHEMA = Paths.get
            ("src/test/java/nl/utwente/ing/schemas/savinggoals/saving-goal-list.json").toAbsolutePath().toUri();

    private static String sessionId;
    private Integer savingGoalId;

    /**
     * Makes sure each test is run with a new session ID.
     */
    @Before
    public void setTestData() {
        sessionId = Util.getSessionID();
    }

    /*
     *  Tests related to POST requests on the /savingGoals API endpoint.
     *  API Documentation:
     *  https://app.swaggerhub.com/apis/djhuistra/INGHonours-SavingsGoals/1.0#/savingGoals/addsavingGoal
     */
    /**
     *  Performs a POST request on the SavingGoals API endpoint.
     *
     *  This test uses a valid session to check whether the API result adheres to the savingGoal format.
     */
    @Test
    public void validSessionValidSavingGoalsCreateTest() {
        String validSavingGoal = "{\n" +
                "  \"name\": \"China holiday\",\n" +
                "  \"goal\": 5000,\n" +
                "  \"savePerMonth\": 250,\n" +
                "  \"minBalanceRequired\": 0\n" +
                "}";

        savingGoalId = given()
                .header("X-session-ID", sessionId)
                .body(validSavingGoal)
                .post("/api/v1/savingGoals")
                .then()
                .statusCode(201)
                .body(matchesJsonSchema(SAVING_GOAL_SCHEMA))
                .extract()
                .jsonPath()
                .getInt("id");
    }

    /**
     * Performs a POST request on the SavingGoals API endpoint.
     *
     * This test uses an invalid SavingGoal to test whether the API result adheres to the savingGoal format.
     */
    @Test
    public void validSessionInvalidSavingGoalsCreateTest() {
        String invalidSavingGoal = "{\n" +
                "  \"description\": \"University of Twente\",\n" +
                "  \"iBAN\": \"NL39RABO0300065264\",\n" +
                "  \"type\": \"deposit\",\n" +
                "  \"applyOnHistory\": true\n" +
                "}";
        given()
                .header("X-session-ID", sessionId)
                .body(invalidSavingGoal)
                .post("/api/v1/savingGoals")
                .then()
                .statusCode(405);
    }

    /**
     * Performs a POST request on the SavingGoals API endpoint.
     *
     * This test uses an invalid session to test whether the API result adheres to the savingGoal format.
     */
    @Test
    public void invalidSessionValidSavingGoalsCreateTest() {
        String validSavingGoal = "{\n" +
                "  \"name\": \"China holiday\",\n" +
                "  \"goal\": 5000,\n" +
                "  \"savePerMonth\": 250,\n" +
                "  \"minBalanceRequired\": 0\n" +
                "}";

        given()
                .body(validSavingGoal)
                .post("/api/v1/savingGoals")
                .then()
                .statusCode(401);
    }

    /*
     *  Tests related to GET requests on the /savingGoals API endpoint.
     *  API Documentation:
     *  https://app.swaggerhub.com/apis/djhuistra/INGHonours-SavingsGoals/1.0#/savingGoals/getSavingGoals
     */
    /**
     * Performs a GET request on the savingGoals API endpoint.
     *
     * This test uses a valid session to get all the created savingGoals and checks if it adheres to the savingGoals
     * format.
     */
    @Test
    public void validSessionSavingGoalsGetTest() {
        //Make sure there is at least one savingGoal to retrieve.
        validSessionValidSavingGoalsCreateTest();

        given()
                .header("X-session-ID", sessionId)
                .get("/api/v1/savingGoals")
                .then()
                .statusCode(200)
                .body(matchesJsonSchema(SAVING_GOAL_LIST_SCHEMA));
    }

    /**
     * Performs a GET request on the savingGoals API endpoint.
     *
     * This test uses an invalid session to check whether the API result adheres to the savingGoals format.
     */
    @Test
    public void invalidSessionSavingGoalsGetTest() {
        given()
                .get("/api/v1/savingGoals")
                .then()
                .statusCode(401);
    }

    /**
     * Performs a GET request on the savingGoals API endpoint.
     *
     * This test uses a valid session to test if the API handles the transaction inputs correctly in combination with
     * the saving goals.
     */
    @Test
    public void validSessionSavingGoalsCalculationGetTest() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -1);

        //Insert base balance into the API
        Util.insertTransaction(sessionId, "1500.00", Util.DATE_FORMAT.format(calendar), "deposit", null, null);

        //Insert the saving goal into the API
        validSessionValidSavingGoalsCreateTest();

        //Withdraw money from the session to update internal time of the API
        calendar = Calendar.getInstance();
        Util.insertTransaction(sessionId, "50.00", Util.DATE_FORMAT.format(calendar), "withdrawal", null, null);

        //Check to see if the savingGoal balance is updated to withdrawal of 1 month
        JsonPath response = given()
                .header("X-session-ID", sessionId)
                .get("/api/v1/savingGoals")
                .then()
                .body(matchesJsonSchema(SAVING_GOAL_LIST_SCHEMA))
                .extract()
                .jsonPath();

        assertEquals(250, response.get("[0].balance"), 0.01);
    }

    /*
     *  Tests related to DELETE requests on the /savingGoals API endpoint.
     *  API Documentation: https://app.swaggerhub.com/apis/djhuistra/INGHonours-SavingGoals/1.1.0#/savingGoals/deleteSavingGoalById
     */
    /**
     *  Performs a DELETE request on the SavingGoals API endpoint.
     *
     *  This test uses a valid session to check whether the API result adheres to the savingGoal format.
     */
    @Test
    public void validSessionValidSavingGoalsIdDeleteTest() {
        validSessionValidSavingGoalsCreateTest();

        given()
                .header("X-session-ID", sessionId)
                .delete(String.format("/api/v1/savingGoals/%d", savingGoalId))
                .then()
                .statusCode(204);
    }

    /**
     *  Performs a DELETE request on the SavingGoals API endpoint.
     *
     *  This test uses an invalid session to check whether the API result adheres to the savingGoal format.
     */
    @Test
    public void invalidSessionValidSavingGoalsIdDeleteTest() {
        validSessionValidSavingGoalsCreateTest();

        given()
            .delete(String.format("/api/v1/savingGoals/%d", savingGoalId))
            .then()
            .statusCode(401);
    }

    /**
     *  Performs a DELETE request on the SavingGoals API endpoint.
     *
     *  This test uses a valid session and invalid saving goal ID to check whether the API result adheres to the
     *  savingGoal format.
     */
    @Test
    public void validSessionInvalidSavingGoalsIdDeleteTest() {
        invalidSessionValidSavingGoalsIdDeleteTest();

        given()
                .header("X-session-ID", sessionId)
                .delete(String.format("/api/v1/savingGoals/%d", savingGoalId))
                .then()
                .statusCode(404);
    }
}
