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

import controllers.scenarios.{Delete200Scenario, Delete404Scenario}
import play.api.test.ResultExtractors

class DeleteEntryControllerISpec extends BaseISpec with ResultExtractors {
  "GET /confirm-delete/:entryId" should {
    s"return $OK when entry ID does exist" in
      forAllScenarios { scenario: Delete200Scenario =>
        val response = await(
          frontendRequest(s"/confirm-delete/${scenario.record.entryId}")
            .withCookies(mockSessionCookie)
            .get()
        )

        response.status shouldBe OK
      }
    s"return $NOT_FOUND when entry ID does not exist" in
      forAllScenarios { scenario: Delete404Scenario =>
        val response = await(
          frontendRequest(s"/confirm-delete/${scenario.entryID}")
            .withCookies(mockSessionCookie)
            .get()
        )

        response.status shouldBe NOT_FOUND
      }
  }
  "GET /delete-entry/:entryId" should {
    s"return $OK when entry ID does exist" in
      forAllScenarios { scenario: Delete200Scenario =>
        val response = await(
          frontendRequest(s"/delete-entry/${scenario.record.entryId}")
            .withCookies(mockSessionCookie)
            .get()
        )

        response.status shouldBe OK
      }
    s"return $NOT_FOUND when entry ID does not exist" in
      forAllScenarios { scenario: Delete404Scenario =>
        val response = await(
          frontendRequest(s"/delete-entry/${scenario.entryID}")
            .withCookies(mockSessionCookie)
            .get()
        )

        response.status shouldBe NOT_FOUND
      }
  }
}
