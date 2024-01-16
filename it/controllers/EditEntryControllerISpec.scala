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
import models.ProtectedUserRecord
import play.api.libs.json.Json

class EditEntryControllerISpec extends BaseISpec {
  "EditEntryController" should {
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

  private def expectUserToBeStrideAuthenticated(pid: String) = stubFor(
    post("/auth/authorise").willReturn(okJson(Json.obj("clientId" -> pid).toString))
  )

  private def expectEditEntryToBeSuccessful(record: ProtectedUserRecord) = stubFor {
    val updatedBody = record.body.copy(addedByUser = None, updatedByTeam = record.body.addedByTeam)
    val expectedPayload = Json.toJsObject(updatedBody).toString

    patch(urlEqualTo(s"$backendBaseUrl/update/${record.entryId}"))
      .withRequestBody(equalToJson(expectedPayload))
      .willReturn(ok(Json.toJsObject(record).toString))
  }

  private def expectEditEntryToFailWithStatus(record: ProtectedUserRecord, status: Int) = stubFor {
    val updatedBody = record.body.copy(addedByUser = None, updatedByTeam = record.body.addedByTeam)
    val expectedPayload = Json.toJsObject(updatedBody).toString

    patch(urlEqualTo(s"$backendBaseUrl/update/${record.entryId}"))
      .withRequestBody(equalToJson(expectedPayload))
      .willReturn(aResponse().withStatus(status))
  }

  private def toEditRequestFields(record: ProtectedUserRecord) = Map(
    "action"             -> Some(if (record.body.identityProviderId.isDefined) "LOCK" else "BLOCK"),
    "nino"               -> record.nino,
    "sautr"              -> record.sautr,
    "identityProvider"   -> record.body.identityProviderId.map(_.name),
    "identityProviderId" -> record.body.identityProviderId.map(_.value),
    "group"              -> Some(record.body.group),
    "addedByTeam"        -> record.body.addedByTeam,
    "updatedByTeam"      -> record.body.updatedByTeam
  ).collect { case (k, Some(v)) => k -> v }
}
