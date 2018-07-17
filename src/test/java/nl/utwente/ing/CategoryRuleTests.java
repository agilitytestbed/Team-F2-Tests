/*
 * Copyright (c) 2018, Joost Prins <github.com/joostprins>
 * All rights reserved.
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

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.nio.file.Paths;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchema;
import static org.junit.Assert.assertEquals;

public class CategoryRuleTests {

    private static final URI CATEGORY_RULE_SCHEMA = Paths.get
            ("src/test/java/nl/utwente/ing/schemas/categoryrules/category-rule.json").toAbsolutePath().toUri();
    private static final URI CATEGORY_RULE_LIST_SCHEMA = Paths.get
            ("src/test/java/nl/utwente/ing/schemas/categoryrules/category-rule-list.json").toAbsolutePath().toUri();

    private static final String TEST_CATEGORY_NAME = "TEST_CATEGORY";

    private static String sessionId;
    private static Integer categoryId;
    private static Integer transactionId;
    private Integer categoryRuleId;

    private JsonParser parser = new JsonParser();

    private static String  validCategoryRule = "{\n" +
            "  \"description\": \"University of Twente\",\n" +
            "  \"iBAN\": \"NL39RABO0300065264\",\n" +
            "  \"type\": \"deposit\",\n" +
            "  \"category_id\": 0,\n" +
            "  \"applyOnHistory\": true\n" +
            "}";

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
     * Makes sure a test category is present to test with.
     */
    @Before
    public void setTestCategory() {
        getTestSession();

        if (categoryId == null) {
            categoryId = Util.createTestCategory(TEST_CATEGORY_NAME, sessionId);
        }
    }

    /**
     * Makes sure a test Transaction is present to test with.
     */
    @Before
    public void setTestTransaction() {
        getTestSession();

        if (transactionId == null) {
            transactionId = Util.createTestTransaction(categoryId, TEST_CATEGORY_NAME, sessionId);
        }
    }

    /**
     * Makes sure the test transaction is deleted after the tests are run.
     */
    @Before
    public void deleteTestTransaction() {
        getTestSession();

        if (transactionId != null) {
            Util.deleteTestTransaction(transactionId, sessionId);
        }
    }

    /**
     * Makes sure the test category used is deleted after the tests are run.
     */
    @After
    public void deleteTestCategory() {
        getTestSession();

        if (categoryId != null) {
            Util.deleteTestCategory(categoryId, sessionId);
        }
    }

    /*
     *  Tests related to GET requests on the /categoryRules API endpoint.
     *  API Documentation: https://app.swaggerhub.com/apis/djhuistra/INGHonours-CategoryRules/1.1.0#/categoryRules/getCategoryRules
     */
    /**
     * Performs a GET request on the CategoryRules API endpoint.
     *
     * This test uses a valid session to get all the created categoryRules and checks if it adheres to the categoryRule format.
     */
    @Test
    public void validSessionCategoryRulesGetTest() {
        //Make sure there is at least one categoryRule to retrieve.
        validSessionValidCategoryRulesCreateTest();

        given()
                .header("X-session-ID", sessionId)
                .get("/categoryRules")
                .then()
                .assertThat()
                .statusCode(200)
                .assertThat()
                .body(matchesJsonSchema(CATEGORY_RULE_LIST_SCHEMA));
    }

    /**
     * Performs a GET request on the CategoryRules API endpoint.
     *
     * This test uses an invalid session to check whether the API result adheres to the categoryRule format.
     */
    @Test
    public void invalidSessionCategoryRulesGetTest() {
        given()
                .get("/categoryRules")
                .then()
                .assertThat()
                .statusCode(401);
    }

    /**
     * Performs a GET request on the CategoryRules API endpoint.
     *
     * This test uses a valid session and valid categoryRule id to check whether the API result adheres to the categoryRule format.
     */
    @Test
    public void validSessionValidCategoryRulesIdGetTest() {
        validSessionValidCategoryRulesCreateTest();

        JsonElement original = parser.parse(validCategoryRule);

        JsonElement response = parser.parse(given()
                .header("X-session-ID", sessionId)
                .get(String.format("/categoryRules/%d", categoryRuleId))
                .then()
                .assertThat()
                .statusCode(200)
                .body(matchesJsonSchema(CATEGORY_RULE_SCHEMA))
                .extract()
                .response()
                .getBody()
                .asString());

        JsonElement responseWithoutId = response.getAsJsonObject().remove("id");

        assertEquals(responseWithoutId, original);
    }

    /**
     * Performs a GET request on the CategoryRules API endpoint.
     *
     * This test uses an invalid session and valid categoryRule id to check whether the API result adheres to the categoryRule format.
     */
    @Test
    public void invalidSessionValidCategoryRulesIdGetTest() {
        given()
                .get(String.format("/categoryRules/%d", categoryRuleId))
                .then()
                .assertThat()
                .statusCode(401);
    }

    /**
     * Performs a GET request on the CategoryRules API endpoint.
     *
     * This test uses a valid session and invalid categoryRule id to check whether the API result adheres to the categoryRule format.
     */
    @Test
    public void validSessionInvalidCategoryRulesIdGetTest() {
        given()
                .header("X-session-ID", sessionId)
                .get(String.format("/categoryRules/%d", categoryRuleId))
                .then()
                .assertThat()
                .statusCode(404);
    }


    /*
     *  Tests related to POST requests on the /categoryRules API endpoint.
     *  API Documentation: https://app.swaggerhub.com/apis/djhuistra/INGHonours-CategoryRules/1.1.0#/categoryRules/addCategoryRule
     */
    /**
     *  Performs a POST request on the CategoryRules API endpoint.
     *
     *  This test uses a valid session to check whether the API result adheres to the categoryRule format.
     */
    @Test
    public void validSessionValidCategoryRulesCreateTest() {
        categoryRuleId = given()
                .header("X-session-ID", sessionId)
                .body(validCategoryRule)
                .post("/categoryRules")
                .then()
                .assertThat()
                .statusCode(201)
                .body(matchesJsonSchema(CATEGORY_RULE_SCHEMA))
                .extract()
                .jsonPath()
                .getInt("id");
    }

    /**
     * Performs a POST request on the CategoryRules API endpoint.
     *
     * This test uses an invalid CategoryRule to test whether the API result adheres to the categoryRule format.
     */
    @Test
    public void validSessionInvalidCategoryRulesCreateTest() {
        String invalidCategoryRule = "{\n" +
                "  \"description\": \"University of Twente\",\n" +
                "  \"iBAN\": \"NL39RABO0300065264\",\n" +
                "  \"type\": \"deposit\",\n" +
                "  \"applyOnHistory\": true\n" +
                "}";
        given()
                .header("X-session-ID", sessionId)
                .body(invalidCategoryRule)
                .post("/categoryRules")
                .then()
                .assertThat()
                .statusCode(405);
    }

    /**
     * Performs a POST request on the CategoryRules API endpoint.
     *
     * This test uses an invalid session to test whether the API result adheres to the categoryRule format.
     */
    @Test
    public void invalidSessionValidCategoryRulesCreateTest() {
        given()
                .body(validCategoryRule)
                .post("/categoryRules")
                .then()
                .assertThat()
                .statusCode(401);
    }

    /*
     *  Tests related to PUT requests on the /categoryRules API endpoint.
     *  API Documentation: https://app.swaggerhub.com/apis/djhuistra/INGHonours-CategoryRules/1.1.0#/categoryRules/updateCategoryRuleById
     */
    /**
     *  Performs a PUT request on the CategoryRules API endpoint.
     *
     *  This test uses a valid session to check whether the API result adheres to the categoryRule format.
     */
    @Test
    public void validSessionValidCategoryRulesUpdateTest() {
        validSessionValidCategoryRulesCreateTest();

        String validCategoryRule2 = "{\n" +
                "  \"description\": \"University of Twente\",\n" +
                "  \"iBAN\": \"NL39RABO0300065264\",\n" +
                "  \"type\": \"withdrawal\",\n" +
                "  \"category_id\": 0,\n" +
                "  \"applyOnHistory\": true\n" +
                "}";

        JsonElement original = parser.parse(validCategoryRule2);

        JsonElement response = parser.parse(given()
                .header("X-session-ID", sessionId)
                .body(validCategoryRule2)
                .put(String.format("/categoryRules/%d", categoryRuleId))
                .then()
                .assertThat()
                .statusCode(200)
                .body(matchesJsonSchema(CATEGORY_RULE_SCHEMA))
                .extract()
                .response()
                .getBody()
                .asString());

        JsonElement responseWithoutId = response.getAsJsonObject().remove("id");

        assertEquals(responseWithoutId, original);
    }

    /**
     *  Performs a PUT request on the CategoryRules API endpoint.
     *
     *  This test uses a valid session and invalid CategoryRule format to check whether the API result adheres to the categoryRule format.
     */
    @Test
    public void validSessionInvalidCategoryRulesUpdateTest() {
        validSessionValidCategoryRulesCreateTest();

        String invalidCategoryRule = "{\n" +
                "  \"description\": \"University of Twente\",\n" +
                "  \"type\": \"withdrawal\",\n" +
                "  \"category_id\": 0,\n" +
                "  \"applyOnHistory\": true\n" +
                "}";

        given()
                .header("X-session-ID", sessionId)
                .body(invalidCategoryRule)
                .put(String.format("/categoryRules/%d", categoryRuleId))
                .then()
                .assertThat()
                .statusCode(405);
    }

    /**
     *  Performs a PUT request on the CategoryRules API endpoint.
     *
     *  This test uses an invalid session and valid CategoryRule format to check whether the API result adheres to the categoryRule format.
     */
    @Test
    public void invalidSessionValidCategoryRulesUpdateTest() {
        given()
                .body(validCategoryRule)
                .put(String.format("/categoryRules/%d", categoryRuleId))
                .then()
                .assertThat()
                .statusCode(401);
    }

    /**
     *  Performs a PUT request on the CategoryRules API endpoint.
     *
     *  This test uses a valid session and invalid CategoryRule id to check whether the API result adheres to the categoryRule format.
     */
    @Test
    public void validSessionInvalidCategoryRulesIdUpdateTest() {
        given()
                .header("X-session-ID", sessionId)
                .body(validCategoryRule)
                .put(String.format("/categoryRules/%d", categoryRuleId))
                .then()
                .assertThat()
                .statusCode(404);
    }
}
