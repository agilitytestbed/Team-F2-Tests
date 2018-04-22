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

import org.junit.Before;

import java.nio.file.Path;
import java.nio.file.Paths;

public class CategoryRuleTests {

    private static final Path CATEGORY_RULE_SCHEME = Paths.get
            ("src/test/java/nl/utwente/ing/schemas/categoryrules/category-rule.json");
    private static final Path CATEGORY_RULE_LIST_SCHEMA_PATH = Paths.get
            ("src/test/java/nl/utwente/ing/schemas/categoryrules/category-rule-list.json");

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



}
