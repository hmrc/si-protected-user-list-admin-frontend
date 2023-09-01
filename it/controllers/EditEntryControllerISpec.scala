/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import controllers.scenarios._
import models.forms.Update

class EditEntryControllerISpec extends BaseISpec {
  "GET /edit/:entryID" should {
    s"return $OK when given entry ID does exist" in
      forAllScenarios { scenario: EditForm200Scenario =>
        val response = await(
          frontendRequest(s"/edit/${scenario.record.entryId}")
            .withCookies(mockSessionCookie)
            .get()
        )

        response.status shouldBe OK
      }

    s"return $NOT_FOUND when given entry ID does not exist" in
      forAllScenarios { scenario: EditForm404Scenario =>
        val response = await(
          frontendRequest(s"/edit/${scenario.entryID}")
            .withCookies(mockSessionCookie)
            .get()
        )

        response.status shouldBe NOT_FOUND
      }
  }
  "POST /edit/:entryID" should {
    s"return $OK when entry ID does exist & form is valid" in
      forAllScenarios { scenario: Edit200Scenario =>
        val payload = Update.form.mapping unbind scenario.update
        val response = await(
          frontendRequest(s"/edit/${scenario.record.entryId}")
            .withHttpHeaders("Csrf-Token" -> "nocheck")
            .withCookies(mockSessionCookie)
            .withFollowRedirects(false)
            .post(payload)
        )
        response.status shouldBe OK
      }
    s"return $BAD_REQUEST when form is invalid" in
      forAllScenarios { scenario: Edit400Scenario =>
        val payload = Update.form.mapping unbind scenario.update
        val response = await(
          frontendRequest(s"/edit/${scenario.entryID}")
            .withHttpHeaders("Csrf-Token" -> "nocheck")
            .withCookies(mockSessionCookie)
            .post(payload)
        )

        response.status shouldBe BAD_REQUEST
      }
    s"return $NOT_FOUND when upstream api return not found" in
      forAllScenarios { scenario: Edit404Scenario =>
        val payload = Update.form.mapping unbind scenario.update
        val response = await(
          frontendRequest(s"/edit/${scenario.entryID}")
            .withHttpHeaders("Csrf-Token" -> "nocheck")
            .withCookies(mockSessionCookie)
            .post(payload)
        )

        response.status shouldBe NOT_FOUND
      }
    s"return $CONFLICT when submitting same IDP ID as existing record with same tax ID" in
      forAllScenarios { scenario: Edit409Scenario =>
        val payload = Update.form.mapping unbind scenario.update
        val response = await(
          frontendRequest(s"/edit/${scenario.secondRecord.entryId}")
            .withHttpHeaders("Csrf-Token" -> "nocheck")
            .withCookies(mockSessionCookie)
            .post(payload)
        )
        println(s"Response body: ${response.body}")
        response.status shouldBe CONFLICT
      }
  }
}
