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

import controllers.scenarios.{Insert201Scenario, Insert400Scenario, Insert409Scenario}
import models.forms.Insert

class AddEntryControllerISpec extends BaseISpec {
  "GET /add" should {
    s"return $OK when viewing insert form" in
      forAllStridePIDs {
        val response = await(frontendRequest("/add").withCookies(mockSessionCookie).get())

        response.status shouldBe OK
      }
  }
  "POST /add" should {
    s"return $SEE_OTHER when add is successful" in
      forAllScenarios { scenario: Insert201Scenario =>
        val payload = Insert.form.mapping unbind scenario.formModel

        val response = frontendRequest("/add")
          .withHttpHeaders("Csrf-Token" -> "nocheck")
          .withCookies(mockSessionCookie)
          .withFollowRedirects(false)
          .post(payload)
          .futureValue

        response.status shouldBe SEE_OTHER
      }

    s"return $BAD_REQUEST when upstream api indicates a conflict" in
      forAllScenarios { scenario: Insert400Scenario =>
        val payload = Insert.form.mapping unbind scenario.formModel

        val response = frontendRequest("/add")
          .withHttpHeaders("Csrf-Token" -> "nocheck")
          .withCookies(mockSessionCookie)
          .post(payload)
          .futureValue

        response.status shouldBe BAD_REQUEST
      }

    s"return $CONFLICT when upstream api indicates a conflict" in
      forAllScenarios { scenario: Insert409Scenario =>
        val payload = Insert.form.mapping unbind scenario.formModel

        val response = frontendRequest("/add")
          .withHttpHeaders("Csrf-Token" -> "nocheck")
          .withCookies(mockSessionCookie)
          .post(payload)
          .futureValue

        response.status shouldBe CONFLICT
      }
  }
}
