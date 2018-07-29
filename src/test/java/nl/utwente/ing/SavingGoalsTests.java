package nl.utwente.ing;

import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URI;
import java.nio.file.Paths;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchema;

public class SavingGoalsTests {

    private static final URI SAVING_GOAL_SCHEMA = Paths.get
            ("src/test/java/nl/utwente/ing/schemas/savinggoals/saving-goal.json").toAbsolutePath().toUri();
    private static final URI SAVING_GOAL_LIST_SCHEMA = Paths.get
            ("src/test/java/nl/utwente/ing/schemas/savinggoals/saving-goal-list.json").toAbsolutePath().toUri();

    private static String sessionId;
    private Integer savingGoalId;

    /**
     * Makes sure test data is present to test with.
     */
    @BeforeClass
    public static void setTestData() {
        if (sessionId == null) {
            sessionId = Util.getSessionID();
        }
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
        if (savingGoalId == null) validSessionValidSavingGoalsCreateTest();

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
        if (savingGoalId == null) validSessionValidSavingGoalsCreateTest();

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
        if (savingGoalId == null) validSessionValidSavingGoalsCreateTest();

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
