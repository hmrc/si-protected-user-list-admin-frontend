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

import com.github.tomakehurst.wiremock.client.WireMock._
import models.{ProtectedUser, ProtectedUserRecord}
import org.jsoup.Jsoup
import play.api.libs.json.Json
import util.ScenarioTables

class AddEntryControllerISpec extends BaseISpec with ScenarioTables {
  private val randomClientIds = alphaNumStringsOfLength(1, 255)

  /** Covers [[AddEntryController.showAddEntryPage()]]. */
  "GET /add" should {
    s"respond $OK and show correct heading" when {
      s"Auth responds $OK with a clientId" in {
        expectUserToBeStrideAuthenticated(randomClientIds.sample.get)

        val response = wsClient
          .url(resource(s"$frontEndBaseUrl/add"))
          .withCookies(mockSessionCookie)
          .get()
          .futureValue

        response.status shouldBe OK
        val h1 = Jsoup.parse(response.body).select("h1")
        h1.text shouldBe "Add Entry"
      }
    }
  }

  /** Covers [[AddEntryController.submit()]]. */
  "POST /add" should {
    val newlyAddedRecords = protectedUserRecords.map { record =>
      val newlyAddedBody = record.body.copy(updatedByUser = None, updatedByTeam = None)
      record.copy(lastUpdated = None, body = newlyAddedBody)
    }

    "redirect to /view/:entryId" when {
      s"Auth responds $OK with a clientId and a valid form is submitted" in
        forAll(newlyAddedRecords) { record =>
          expectUserToBeStrideAuthenticated(record.body.addedByUser.value)
          expectAddEntryToBeSuccessful(record)

          val response = wsClient
            .url(resource(s"$frontEndBaseUrl/add"))
            .withHttpHeaders("Csrf-Token" -> "nocheck")
            .withCookies(mockSessionCookie)
            .withFollowRedirects(false)
            .post(toRequestFields(record))
            .futureValue

          response.status                 shouldBe SEE_OTHER
          response.header(LOCATION).value shouldBe s"$frontEndBaseUrl/view-entry/${record.entryId}"
        }
    }

    s"respond $BAD_REQUEST" when
      forAll(invalidAddEntryScenarios) { (describeScenario, badPayload, _) =>
        describeScenario in {
          expectUserToBeStrideAuthenticated(randomClientIds.sample.get)

          val response = wsClient
            .url(resource(s"$frontEndBaseUrl/add"))
            .withHttpHeaders("Csrf-Token" -> "nocheck")
            .withCookies(mockSessionCookie)
            .withFollowRedirects(false)
            .post(badPayload)
            .futureValue

          response.status shouldBe BAD_REQUEST
        }
      }

    "Return CONFLICT when upstream api indicates a conflict" in
      forAll(newlyAddedRecords) { record =>
        expectUserToBeStrideAuthenticated(record.body.addedByUser.value)
        expectAddEntryToFailWithConflictStatus(record.body)

        val response = wsClient
          .url(resource(s"$frontEndBaseUrl/add"))
          .withHttpHeaders("Csrf-Token" -> "nocheck")
          .withCookies(mockSessionCookie)
          .post(toRequestFields(record))
          .futureValue

        response.status shouldBe CONFLICT
      }
  }

  private def expectAddEntryToBeSuccessful(protectedUserRecord: ProtectedUserRecord) = stubFor {
    post(urlEqualTo(s"$backendBaseUrl/add"))
      .withRequestBody(equalToJson(Json.toJsObject(protectedUserRecord.body).toString))
      .willReturn(ok(Json.toJsObject(protectedUserRecord).toString))
  }

  private def expectAddEntryToFailWithConflictStatus(protectedUser: ProtectedUser) = stubFor {
    post(urlEqualTo(s"$backendBaseUrl/add"))
      .withRequestBody(equalToJson(Json.toJsObject(protectedUser).toString))
      .willReturn(aResponse().withStatus(CONFLICT))
  }

  private def toRequestFields(record: ProtectedUserRecord) = Map(
    "action"             -> Some(if (record.body.identityProviderId.isDefined) "LOCK" else "BLOCK"),
    "nino"               -> record.nino,
    "sautr"              -> record.sautr,
    "identityProvider"   -> record.body.identityProviderId.map(_.name),
    "identityProviderId" -> record.body.identityProviderId.map(_.value),
    "group"              -> Some(record.body.group),
    "team"               -> record.body.addedByTeam
  ).collect { case (k, Some(v)) => k -> v }
}
