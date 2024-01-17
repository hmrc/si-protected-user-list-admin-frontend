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

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.i18n.{Lang, MessagesApi, MessagesImpl, MessagesProvider}
import play.api.libs.ws.{DefaultWSCookie, WSCookie}
import play.api.mvc.{Cookie, Session, SessionCookieBaker}
import play.api.test.Injecting
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.gg.test.WireMockSpec
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCrypto
import util.Generators

abstract class BaseISpec
    extends WireMockSpec
    with GuiceOneServerPerSuite
    with Injecting
    with ScalaFutures
    with Generators
    with ScalaCheckDrivenPropertyChecks
    with TableDrivenPropertyChecks {
  protected implicit val pc: PatienceConfig = PatienceConfig(timeout = Span(5, Seconds), interval = Span(5, Millis))

  val frontEndBaseUrl = "/account-protection-tools/protected-user-list"
  implicit val mp: MessagesProvider = MessagesImpl(Lang("en"), inject[MessagesApi])

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

    DefaultWSCookie(
      cookie.name,
      cookie.value,
      cookie.domain,
      Some(cookie.path),
      cookie.maxAge.map(_.toLong),
      cookie.secure,
      cookie.httpOnly
    )
  }

  import com.github.tomakehurst.wiremock.client.WireMock._
  import models.ProtectedUserRecord
  import play.api.libs.json.Json

  protected val backendBaseUrl = "/si-protected-user-list-admin"

  protected def expectUserToBeStrideAuthenticated(clientId: String): Unit = stubFor {
    post("/auth/authorise") willReturn okJson(Json.obj("clientId" -> clientId).toString)
  }
  protected def expectFindEntryToBeSuccessful(protectedUser: ProtectedUserRecord): Unit = stubFor {
    get(s"$backendBaseUrl/entry-id/${protectedUser.entryId}") willReturn okJson(Json.toJsObject(protectedUser).toString)
  }
  protected def expectFindEntryToFailWithNotFound(entryId: String): Unit = stubFor {
    get(s"$backendBaseUrl/entry-id/$entryId") willReturn notFound()
  }
}
