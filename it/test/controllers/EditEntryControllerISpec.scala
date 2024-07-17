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

package test.controllers

import com.github.tomakehurst.wiremock.client.WireMock._
import models.{Entry, ProtectedUser, ProtectedUserRecord}
import play.api.libs.json.Json
import play.api.test.ResultExtractors

class EditEntryControllerISpec extends BaseISpec with ResultExtractors {
  "EditEntryController" should {
    "return OK when edit is successful" in
      forAll(nonEmptyStringGen, validEditEntryGen, protectedUserRecords, nonEmptyStringGen) { (entryId, entry, record, pid) =>
        expectUserToBeStrideAuthenticated(pid)
        val expectedEntry = entry.copy(updatedByUser = Some(pid), updatedByTeam = Option(entry.addedByTeam))

        expectEditEntryToBeSuccessful(entryId, record, expectedEntry.toProtectedUserImpl(isUpdate = true, pid))

        val response = wsClient
          .url(resource(s"$frontEndBaseUrl/edit/$entryId"))
          .withHttpHeaders("Csrf-Token" -> "nocheck")
          .withCookies(mockSessionCookie)
          .withFollowRedirects(false)
          .post(toEditRequestFields(expectedEntry).toMap)
          .futureValue

        response.status shouldBe OK
      }

    "Return NOT_FOUND when upstream api returns 'credId not found' response and populate the error" in
      forAll(nonEmptyStringGen, validEditEntryGen, nonEmptyStringGen) { (entryId, entry, pid) =>
        expectUserToBeStrideAuthenticated(pid)
        val expectedEntry = entry.copy(updatedByUser = Some(pid), updatedByTeam = Option(entry.addedByTeam))

        stubFor(
          patch(urlEqualTo(s"$backendBaseUrl/update/$entryId"))
            .willReturn(aResponse().withStatus(NOT_FOUND).withBody(Json.obj("error" -> "CREDID_DOES_NOT_EXIST").toString))
        )

        val response = wsClient
          .url(resource(s"$frontEndBaseUrl/edit/$entryId"))
          .withHttpHeaders("Csrf-Token" -> "nocheck")
          .withCookies(mockSessionCookie)
          .post(toEditRequestFields(expectedEntry).toMap)
          .futureValue

        response.status shouldBe NOT_FOUND
        response.body should include ("The credId does not exist")
      }

    "Return NOT_FOUND when upstream api returns generic Not Found response" in
      forAll(nonEmptyStringGen, validEditEntryGen, nonEmptyStringGen) { (entryId, entry, pid) =>
        expectUserToBeStrideAuthenticated(pid)
        val expectedEntry = entry.copy(updatedByUser = Some(pid), updatedByTeam = Option(entry.addedByTeam))

        expectEditEntryToFailWithStatus(entryId, expectedEntry.toProtectedUserImpl(isUpdate = true, pid), NOT_FOUND)
        val response = wsClient
          .url(resource(s"$frontEndBaseUrl/edit/$entryId"))
          .withHttpHeaders("Csrf-Token" -> "nocheck")
          .withCookies(mockSessionCookie)
          .post(toEditRequestFields(expectedEntry).toMap)
          .futureValue

        response.status shouldBe NOT_FOUND
      }

    "Return CONFLICT when upstream api indicates a conflict" in
      forAll(nonEmptyStringGen, validEditEntryGen, nonEmptyStringGen) { (entryId, entry, pid) =>
        expectUserToBeStrideAuthenticated(pid)
        val expectedEntry = entry.copy(updatedByUser = Some(pid), updatedByTeam = Option(entry.addedByTeam))

        expectEditEntryToFailWithStatus(entryId, expectedEntry.toProtectedUserImpl(isUpdate = true, pid), CONFLICT)
        val response = wsClient
          .url(resource(s"$frontEndBaseUrl/edit/$entryId"))
          .withHttpHeaders("Csrf-Token" -> "nocheck")
          .withCookies(mockSessionCookie)
          .post(toEditRequestFields(expectedEntry).toMap)
          .futureValue

        response.status shouldBe CONFLICT
      }
  }

  private def expectUserToBeStrideAuthenticated(pid: String) = {
    stubFor(post("/auth/authorise").willReturn(okJson(Json.obj("clientId" -> pid).toString())))
  }

  private def expectEditEntryToBeSuccessful(entryId: String, protectedUserRecord: ProtectedUserRecord, protectedUser: ProtectedUser) = {
    stubFor(
      patch(urlEqualTo(s"$backendBaseUrl/update/$entryId"))
        .withRequestBody(equalToJson(Json.toJsObject(protectedUser).toString()))
        .willReturn(ok(Json.toJsObject(protectedUserRecord).toString()))
    )
  }

  private def expectEditEntryToFailWithStatus(entryId: String, protectedUser: ProtectedUser, status: Int) = {
    stubFor(
      patch(urlEqualTo(s"$backendBaseUrl/update/$entryId"))
        .withRequestBody(equalToJson(Json.toJsObject(protectedUser).toString()))
        .willReturn(aResponse().withStatus(status))
    )
  }

  private def toEditRequestFields(entry: Entry): Seq[(String, String)] = {
    Seq(
      Some("action" -> entry.action),
      entry.nino.map(n => "nino" -> n),
      entry.sautr.map(s => "sautr" -> s),
      entry.identityProvider.map(s => "identityProvider" -> s),
      entry.identityProviderId.map(s => "identityProviderId" -> s),
      entry.group.map(s => "group" -> s),
      Some("addedByTeam" -> entry.addedByTeam),
      entry.updatedByTeam.map(s => "updatedByTeam" -> s)
    ).flatten
  }
}
