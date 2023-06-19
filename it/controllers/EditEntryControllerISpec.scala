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
import models.{Entry, ProtectedUser, ProtectedUserRecord}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.libs.json.Json
import play.api.test.ResultExtractors
import util.Generators

import scala.concurrent.duration.DurationInt

class EditEntryControllerISpec extends BaseISpec with ResultExtractors with Generators with ScalaFutures with ScalaCheckDrivenPropertyChecks {
  import org.scalacheck.Arbitrary.arbitrary

  private implicit val defaultPatience: PatienceConfig = PatienceConfig(timeout = 5.seconds, interval = 5.millis)

  "EditEntryController" should {
    "return OK when edit is successful" in
      forAll(validEditEntryGen, arbitrary, nonEmptyStringGen) { (entry, protectedUserRecord, pid) =>
        expectUserToBeStrideAuthenticated(pid)
        val expectedEntry = entry.copy(updatedByUser = Some(pid), updatedByTeam = entry.addedByTeam)

        expectEditEntryToBeSuccessful(expectedEntry.entryId.value, protectedUserRecord, expectedEntry.toProtectedUser())

        val response = wsClient
          .url(resource(s"$frontEndBaseUrl/edit"))
          .withHttpHeaders("Csrf-Token" -> "nocheck")
          .withCookies(mockSessionCookie)
          .withFollowRedirects(false)
          .post(toEditRequestFields(expectedEntry).toMap)
          .futureValue

        response.status shouldBe OK
      }

    "Return NOT_FOUND when upstream api return not found" in
      forAll(validEditEntryGen, nonEmptyStringGen) { (entry, pid) =>
        expectUserToBeStrideAuthenticated(pid)
        val expectedEntry = entry.copy(updatedByUser = Some(pid), updatedByTeam = entry.addedByTeam)

        expectEditEntryToFailWithStatus(expectedEntry.entryId.value, expectedEntry.toProtectedUser(), NOT_FOUND)
        val response = wsClient
          .url(resource(s"$frontEndBaseUrl/edit"))
          .withHttpHeaders("Csrf-Token" -> "nocheck")
          .withCookies(mockSessionCookie)
          .post(toEditRequestFields(expectedEntry).toMap)
          .futureValue

        response.status shouldBe NOT_FOUND
      }

    "Return CONFLICT when upstream api indicates a conflict" in
      forAll(validEditEntryGen, nonEmptyStringGen) { (entry, pid) =>
        expectUserToBeStrideAuthenticated(pid)
        val expectedEntry = entry.copy(updatedByUser = Some(pid), updatedByTeam = entry.addedByTeam)

        expectEditEntryToFailWithStatus(expectedEntry.entryId.value, expectedEntry.toProtectedUser(), CONFLICT)
        val response = wsClient
          .url(resource(s"$frontEndBaseUrl/edit"))
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
      entry.entryId.map(e => "entryId" -> e),
      Some("action" -> entry.action),
      entry.nino.map(n => "nino" -> n),
      entry.sautr.map(s => "sautr" -> s),
      entry.identityProvider.map(s => "identityProvider" -> s),
      entry.identityProviderId.map(s => "identityProviderId" -> s),
      entry.group.map(s => "group" -> s),
      entry.addedByTeam.map(s => "addedByTeam" -> s),
      entry.updatedByTeam.map(s => "updatedByTeam" -> s)
    ).flatten
  }
}
