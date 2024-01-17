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
import models.InputForms.{addEntryActionBlock, addEntryActionLock, groupMaxLength}
import models.{ProtectedUser, ProtectedUserRecord}
import org.jsoup.Jsoup
import play.api.data.FormError
import play.api.i18n.Messages
import play.api.libs.json.Json

class AddEntryControllerISpec extends BaseISpec {
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

    "return CREATED when add is successful" in
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

        response.status shouldBe SEE_OTHER
      }

    val allRequestFieldsPresentEntryForm = Map(
      "action"             -> addEntryActionBlock,
      "nino"               -> ninoGen.sample.get.nino,
      "sautr"              -> sautrGen.sample.get.utr,
      "identityProvider"   -> nonEmptyStringGen.sample.get,
      "identityProviderId" -> nonEmptyStringGen.sample.get,
      "group"              -> nonEmptyStringOfGen(groupMaxLength).sample.get,
      "addedByTeam"        -> nonEmptyStringGen.sample.get
    )

    val missingNinoAndSautr = allRequestFieldsPresentEntryForm.updated("sautr", "").updated("nino", "")
    val actionLockNoCredId = allRequestFieldsPresentEntryForm.updated("action", addEntryActionLock).updated("identityProviderId", "")
    val groupIsLongerThanAllowed = allRequestFieldsPresentEntryForm.updated("group", nonEmptyStringOfGen(groupMaxLength + 1).sample.get)
    val missingAddedByTeam = allRequestFieldsPresentEntryForm.updated("addedByTeam", "")

    val badRequestScenarios = Table(
      ("Description", "Request fields"),
      ("NINO is submitted in wrong format", allRequestFieldsPresentEntryForm.updated("nino", "bad_nino")),
      ("SAUTR is submitted in wrong format", allRequestFieldsPresentEntryForm.updated("sautr", "bad_sautr")),
      (s"a group longer than $groupMaxLength is submitted", groupIsLongerThanAllowed),
      ("neither a NINO or SAUTR is submitted", missingNinoAndSautr),
      ("AddedByTeam is missing", missingAddedByTeam),
      ("identityProviderId must be present when action is LOCK", actionLockNoCredId)
    )

    s"respond $BAD_REQUEST" when
      forAll(badRequestScenarios) { (describeScenario, badPayload) =>
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

  private def expectUserToBeStrideAuthenticated(pid: String) = stubFor {
    post("/auth/authorise") willReturn okJson(Json.obj("clientId" -> pid).toString)
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
    "addedByTeam"        -> record.body.addedByTeam
  ).collect { case (k, Some(v)) => k -> v }
}
