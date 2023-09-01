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

import com.github.tomakehurst.wiremock.client.WireMock.{okJson, post, stubFor}
import controllers.scenarios.AbstractScenario
import models.{Generators, JsonWriters}
import org.scalacheck.{Arbitrary, Shrink}
import org.scalatest.Assertion
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.i18n.{Lang, MessagesApi, MessagesImpl, MessagesProvider}
import play.api.libs.json.Json
import play.api.libs.json.Json.toJson
import play.api.libs.ws.{WSCookie, WSRequest}
import play.api.mvc.{Cookie, Session, SessionCookieBaker}
import play.api.test.Injecting
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.gg.test.WireMockSpec
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCrypto
import org.scalacheck.Gen

trait BaseISpec extends WireMockSpec with Injecting with ScalaFutures with Generators with JsonWriters with ScalaCheckDrivenPropertyChecks {
  protected implicit val pc: PatienceConfig = PatienceConfig(timeout = Span(5, Seconds), interval = Span(5, Millis))

  val frontEndBaseUrl = "/account-protection-tools/protected-user-list"
  implicit val mp: MessagesProvider = MessagesImpl(Lang("en"), inject[MessagesApi])

  private lazy val backendPortConfig = "microservice.services.si-protected-user-list-admin.port" -> 8507

  override def replaceExternalDependenciesWithMockServers: Map[String, Any] =
    super.replaceExternalDependenciesWithMockServers + backendPortConfig

  protected def frontendRequest(path: String): WSRequest = wsClient.url(resource(s"$frontEndBaseUrl$path"))
  protected def backendRequest(path:  String): WSRequest = wsClient.url(s"http://localhost:${backendPortConfig._2}$path")

  private def stubAuthCallFor(stridePID: String) =
    stubFor(post("/auth/authorise") willReturn okJson(Json.obj("clientId" -> stridePID).toString))

  protected def forAllScenarios[S <: AbstractScenario: Arbitrary: Shrink](fun: S => Assertion): Assertion =
    forAll { scenario: S =>
      await(backendRequest("/test-only/delete-all").post(""))
      await(backendRequest("/test-only/insert-many").post(toJson(scenario.initRecords)))

      stubAuthCallFor(scenario.strideUserPID)

      fun(scenario)
    }

  protected def forAllStridePIDs(block: => Assertion): Assertion = {
    val randomStridePIDs = Gen.alphaNumStr.filter(_.nonEmpty)

    forAll(randomStridePIDs) { stridePID =>
      stubAuthCallFor(stridePID)
      block
    }
  }

  protected def mockSessionCookie: WSCookie = {
    def makeSessionCookie(session: Session): Cookie = {
      val cookieCrypto = inject[SessionCookieCrypto]
      val cookieBaker = inject[SessionCookieBaker]
      val sessionCookie = cookieBaker.encodeAsCookie(session)
      val encryptedValue = cookieCrypto.crypto.encrypt(PlainText(sessionCookie.value))
      sessionCookie.copy(value = encryptedValue.value)
    }

    val mockSession = Session(
      Map(
        SessionKeys.lastRequestTimestamp -> System.currentTimeMillis().toString,
        SessionKeys.authToken            -> "mock-bearer-token",
        SessionKeys.sessionId            -> "mock-sessionid"
      )
    )

    val cookie = makeSessionCookie(mockSession)

    new WSCookie() {
      override def name:     String = cookie.name
      override def value:    String = cookie.value
      override def domain:   Option[String] = cookie.domain
      override def path:     Option[String] = Some(cookie.path)
      override def maxAge:   Option[Long] = cookie.maxAge.map(_.toLong)
      override def secure:   Boolean = cookie.secure
      override def httpOnly: Boolean = cookie.httpOnly
    }
  }
}
