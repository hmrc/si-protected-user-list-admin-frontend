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
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.libs.json.Json
import play.api.test.ResultExtractors
import util.Generators

class EditEntryControllerISpec extends BaseISpec with ResultExtractors with Generators with ScalaFutures with ScalaCheckDrivenPropertyChecks {
  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(5, Millis))

  "EditEntryController" should {

    "return OK when edit is successful" in new Setup {
      forAll(validEditEntryGen, protectedUserRecordGen, nonEmptyStringGen) { (entry, protectedUserRecord, pid) =>
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
    }

    "Return NOT_FOUND when upstream api return not found" in new Setup {
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
    }

    "Return CONFLICT when upstream api indicates a conflict" in new Setup {
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
  }

  trait Setup {

    def expectUserToBeStrideAuthenticated(pid: String): Unit = {
      stubFor(post("/auth/authorise").willReturn(okJson(Json.obj("clientId" -> pid).toString())))
    }

    def expectEditEntryToBeSuccessful(entryId: String, protectedUserRecord: ProtectedUserRecord, protectedUser: ProtectedUser): Unit = {
      stubFor(
        patch(urlEqualTo(s"$backendBaseUrl/update/$entryId"))
          .withRequestBody(equalToJson(Json.toJsObject(protectedUser).toString()))
          .willReturn(ok(Json.toJsObject(protectedUserRecord).toString()))
      )
    }
    def expectEditEntryToFailWithStatus(entryId: String, protectedUser: ProtectedUser, status: Int): Unit = {
      stubFor(
        patch(urlEqualTo(s"$backendBaseUrl/update/$entryId"))
          .withRequestBody(equalToJson(Json.toJsObject(protectedUser).toString()))
          .willReturn(aResponse().withStatus(status))
      )
    }

    def toEditRequestFields(entry: Entry): Seq[(String, String)] = {
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
}
