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

import controllers.scenarios.{Edit200Scenario, Edit404Scenario, EditForm200Scenario, EditForm404Scenario}
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
//
//    "Return CONFLICT when upstream api indicates a conflict" in
//      forAll(validEditEntryGen, nonEmptyStringGen) { (entry, pid) =>
//        expectUserToBeStrideAuthenticated(pid)
//        val expectedEntry = entry.copy(updatedByUser = Some(pid), updatedByTeam = entry.addedByTeam)
//
//        expectEditEntryToFailWithStatus(expectedEntry.entryId.value, expectedEntry.toProtectedUser(), CONFLICT)
//        val response = wsClient
//          .url(resource(s"$frontEndBaseUrl/edit"))
//          .withHttpHeaders("Csrf-Token" -> "nocheck")
//          .withCookies(mockSessionCookie)
//          .post(toEditRequestFields(expectedEntry).toMap)
//          .futureValue
//
//        response.status shouldBe CONFLICT
//      }
//  }
}
