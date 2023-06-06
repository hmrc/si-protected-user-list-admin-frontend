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
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.libs.json.Json
import play.api.test.ResultExtractors
import util.Generators

class SiProtectedUserControllerISpec extends BaseISpec with ResultExtractors with Generators with ScalaFutures with ScalaCheckDrivenPropertyChecks {
  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(5, Millis))

  "SiProtectedUserController" should {

    "return OK when view is successful" in new Setup {
      forAll(protectedUserRecordGen, nonEmptyStringGen) { (protectedUserRecord, pid) =>
        expectUserToBeStrideAuthenticated(pid)
        expectFindEntryToBeSuccessful(protectedUserRecord)

        val response = wsClient
          .url(resource(s"$frontEndBaseUrl/view-entry/${protectedUserRecord.entryId}"))
          .withHttpHeaders("Csrf-Token" -> "nocheck")
          .withCookies(mockSessionCookie)
          .get()
          .futureValue

        response.status shouldBe OK
      }
    }

    "return NOT_FOUND when entry doesnt exist" in new Setup {
      forAll(protectedUserRecordGen, nonEmptyStringGen) { (protectedUserRecord, pid) =>
        expectUserToBeStrideAuthenticated(pid)
        expectFindEntryToFailWithNotFound(protectedUserRecord)

        val response = wsClient
          .url(resource(s"$frontEndBaseUrl/view-entry/${protectedUserRecord.entryId}"))
          .withHttpHeaders("Csrf-Token" -> "nocheck")
          .withCookies(mockSessionCookie)
          .get()
          .futureValue

        response.status shouldBe NOT_FOUND
      }
    }

  }

  trait Setup {

    def expectUserToBeStrideAuthenticated(pid: String): Unit = {
      stubFor(post("/auth/authorise").willReturn(okJson(Json.obj("clientId" -> pid).toString())))
    }

    def expectFindEntryToBeSuccessful(protectedUser: ProtectedUserRecord): Unit = {
      stubFor(
        get(urlEqualTo(s"$backendBaseUrl/entry-id/${protectedUser.entryId}"))
          .willReturn(ok(Json.toJsObject(protectedUser).toString()))
      )
    }

    def expectFindEntryToFailWithNotFound(protectedUser: ProtectedUserRecord): Unit = {
      stubFor(
        get(urlEqualTo(s"$backendBaseUrl/entry-id/${protectedUser.entryId}"))
          .willReturn(aResponse().withStatus(NOT_FOUND))
      )
    }
  }
}
