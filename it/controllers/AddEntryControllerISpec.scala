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

class AddEntryControllerISpec extends BaseISpec with ResultExtractors with Generators with ScalaFutures with ScalaCheckDrivenPropertyChecks {
  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(5, Millis))

  "AddEntryController" should {

    "return CREATED when add is successful" in new Setup {
      forAll(validRequestEntryGen, protectedUserRecordGen, nonEmptyStringGen) { (entry, protectedUserRecord, pid) =>
        expectUserToBeStrideAuthenticated(pid)
        val expectedEntry = entry.copy(addedByUser = Some(pid))

        expectAddEntryToBeSuccessful(protectedUserRecord, expectedEntry.toProtectedUser())

        val response = wsClient
          .url(resource(s"$frontEndBaseUrl/add"))
          .withHttpHeaders("Csrf-Token" -> "nocheck")
          .withCookies(mockSessionCookie)
          .withFollowRedirects(false)
          .post(toRequestFields(expectedEntry).toMap)
          .futureValue

        response.status shouldBe SEE_OTHER
      }
    }

    "Return CONFLICT when upstream api indicates a conflict" in new Setup {
      forAll(validRequestEntryGen, nonEmptyStringGen) { (entry, pid) =>
        expectUserToBeStrideAuthenticated(pid)
        val expectedEntry = entry.copy(addedByUser = Some(pid))

        expectAddEntryToFailWithConflictStatus(expectedEntry.toProtectedUser())
        val response = wsClient
          .url(resource(s"$frontEndBaseUrl/add"))
          .withHttpHeaders("Csrf-Token" -> "nocheck")
          .withCookies(mockSessionCookie)
          .post(toRequestFields(expectedEntry).toMap)
          .futureValue

        response.status shouldBe CONFLICT
      }
    }
  }

  trait Setup {

    def expectUserToBeStrideAuthenticated(pid: String): Unit = {
      stubFor(post("/auth/authorise").willReturn(okJson(Json.obj("clientId" -> pid).toString())))
    }

    def expectAddEntryToBeSuccessful(protectedUserRecord: ProtectedUserRecord, protectedUser: ProtectedUser): Unit = {
      stubFor(
        post(urlEqualTo(s"$backendBaseUrl/add"))
          .withRequestBody(equalToJson(Json.toJsObject(protectedUser).toString()))
          .willReturn(ok(Json.toJsObject(protectedUserRecord).toString()))
      )
    }
    def expectAddEntryToFailWithConflictStatus(protectedUser: ProtectedUser): Unit = {
      stubFor(
        post(urlEqualTo(s"$backendBaseUrl/add"))
          .withRequestBody(equalToJson(Json.toJsObject(protectedUser).toString()))
          .willReturn(aResponse().withStatus(CONFLICT))
      )
    }

    def toRequestFields(entry: Entry): Seq[(String, String)] = {
      Seq(
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
