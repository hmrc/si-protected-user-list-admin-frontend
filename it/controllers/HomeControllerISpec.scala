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

import controllers.scenarios.{View200Scenario, View404Scenario}

class HomeControllerISpec extends BaseISpec {
  "GET /" should {
    s"return $OK" in
      forAllStridePIDs {
        val response = await(
          frontendRequest("/")
            .withCookies(mockSessionCookie)
            .get()
        )
        response.status shouldBe OK
      }
  }
  "GET /view-entry/:entryId" should {
    "return OK when view is successful" in
      forAllScenarios { scenario: View200Scenario =>
        val response = frontendRequest(s"/view-entry/${scenario.record.entryId}")
          .withHttpHeaders("Csrf-Token" -> "nocheck")
          .withCookies(mockSessionCookie)
          .get()
          .futureValue

        response.status shouldBe OK
      }

    "return NOT_FOUND when entry doesnt exist" in
      forAllScenarios { scenario: View404Scenario =>
        val response = frontendRequest(s"/view-entry/${scenario.entryID}")
          .withHttpHeaders("Csrf-Token" -> "nocheck")
          .withCookies(mockSessionCookie)
          .get()
          .futureValue

        response.status shouldBe NOT_FOUND
      }
  }
}
