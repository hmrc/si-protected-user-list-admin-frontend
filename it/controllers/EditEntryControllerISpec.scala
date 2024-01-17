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
import models.TaxIdentifierType.NINO
import models.{ProtectedUserRecord, TaxIdentifier}
import org.jsoup.Jsoup
import play.api.libs.json.Json
import util.ScenarioTables

class EditEntryControllerISpec extends BaseISpec with ScenarioTables {

  /** Covers [[EditEntryController.showEditEntryPage()]]. */
  "GET /edit/:entryId" should {
    s"respond $OK and show correct heading" when {
      s"Auth responds $OK with a clientId and backend responds $OK with a ProtectedUserRecord" in
        forAll(protectedUserRecords) { record =>
          expectUserToBeStrideAuthenticated(record.body.updatedByUser.value)
          expectFindEntryToBeSuccessful(record)

          val response = wsClient
            .url(resource(s"$frontEndBaseUrl/edit/${record.entryId}"))
            .withCookies(mockSessionCookie)
            .get()
            .futureValue

          response.status shouldBe OK
          val h1 = Jsoup.parse(response.body).select("h1")
          h1.text shouldBe "Edit Entry"
        }
    }
    s"respond $NOT_FOUND and show correct heading" when {
      s"Auth responds $OK with a clientId but backend responds $NOT_FOUND" in
        forAll(protectedUserRecords) { record =>
          expectUserToBeStrideAuthenticated(record.body.updatedByUser.value)
          expectFindEntryToFailWithNotFound(record.entryId)

          val response = wsClient
            .url(resource(s"$frontEndBaseUrl/edit/${record.entryId}"))
            .withCookies(mockSessionCookie)
            .get()
            .futureValue

          response.status shouldBe NOT_FOUND
          val h1 = Jsoup.parse(response.body).select("h1")
          h1.text shouldBe "Not Found"
        }
    }
  }

  /** Covers [[EditEntryController.submit()]]. */
  "POST /edit/:entry:Id" should {
    "return OK when edit is successful" in
      forAll(protectedUserRecords) { record =>
        expectUserToBeStrideAuthenticated(record.body.updatedByUser.value)
        expectEditEntryToBeSuccessful(record)

        val response = wsClient
          .url(resource(s"$frontEndBaseUrl/edit/${record.entryId}"))
          .withHttpHeaders("Csrf-Token" -> "nocheck")
          .withCookies(mockSessionCookie)
          .withFollowRedirects(false)
          .post(toEditRequestFields(record))
          .futureValue

        response.status shouldBe OK
      }

    s"respond $BAD_REQUEST" when
      forAll(invalidFormScenarios) { (describeScenario, badPayload, _) =>
        describeScenario in
          forAll(protectedUserRecords) { record =>
            expectUserToBeStrideAuthenticated(record.body.updatedByUser.value)

            val response = wsClient
              .url(resource(s"$frontEndBaseUrl/edit/${record.entryId}"))
              .withHttpHeaders("Csrf-Token" -> "nocheck")
              .withCookies(mockSessionCookie)
              .withFollowRedirects(false)
              .post(badPayload)
              .futureValue

            response.status shouldBe BAD_REQUEST
          }
      }

    "Return NOT_FOUND when upstream api return not found" in
      forAll(protectedUserRecords) { record =>
        expectUserToBeStrideAuthenticated(record.body.updatedByUser.value)
        expectEditEntryToFailWithStatus(record, NOT_FOUND)

        val response = wsClient
          .url(resource(s"$frontEndBaseUrl/edit/${record.entryId}"))
          .withHttpHeaders("Csrf-Token" -> "nocheck")
          .withCookies(mockSessionCookie)
          .post(toEditRequestFields(record))
          .futureValue

        response.status shouldBe NOT_FOUND
      }

    "Return CONFLICT when upstream api indicates a conflict" in
      forAll(protectedUserRecords) { record =>
        expectUserToBeStrideAuthenticated(record.body.updatedByUser.value)
        expectEditEntryToFailWithStatus(record, CONFLICT)

        val response = wsClient
          .url(resource(s"$frontEndBaseUrl/edit/${record.entryId}"))
          .withHttpHeaders("Csrf-Token" -> "nocheck")
          .withCookies(mockSessionCookie)
          .post(toEditRequestFields(record))
          .futureValue

        response.status shouldBe CONFLICT
      }
  }

  private def expectEditEntryToBeSuccessful(record: ProtectedUserRecord) = stubFor {
    patch(urlEqualTo(s"$backendBaseUrl/update/${record.entryId}"))
      .withRequestBody(expectedPayloadFrom(record))
      .willReturn(ok(Json.toJsObject(record).toString))
  }

  private def expectEditEntryToFailWithStatus(record: ProtectedUserRecord, status: Int) = stubFor {
    patch(urlEqualTo(s"$backendBaseUrl/update/${record.entryId}"))
      .withRequestBody(expectedPayloadFrom(record))
      .willReturn(aResponse().withStatus(status))
  }

  private def expectedPayloadFrom(record: ProtectedUserRecord) = {
    val updatedBody = record.body.copy(
      taxId = TaxIdentifier(NINO, ""),
      addedByUser = None,
      addedByTeam = None
    )
    equalToJson(Json.toJsObject(updatedBody).toString)
  }

  private def toEditRequestFields(record: ProtectedUserRecord) = Map(
    "action"             -> Some(if (record.body.identityProviderId.isDefined) "LOCK" else "BLOCK"),
    "identityProvider"   -> record.body.identityProviderId.map(_.name),
    "identityProviderId" -> record.body.identityProviderId.map(_.value),
    "group"              -> Some(record.body.group),
    "team"               -> record.body.updatedByTeam
  ).collect { case (k, Some(v)) => k -> v }
}
