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
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.i18n.{Lang, MessagesApi, MessagesImpl, MessagesProvider}
import play.api.libs.ws.WSCookie
import play.api.mvc.{Cookie, Session, SessionCookieBaker}
import play.api.test.Injecting
import uk.gov.hmrc.crypto.PlainText
import support.WireMockSpec
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCrypto
import util.Generators

trait BaseISpec extends WireMockSpec with GuiceOneServerPerSuite with Injecting with ScalaFutures with Generators with ScalaCheckDrivenPropertyChecks {
  protected implicit val pc: PatienceConfig = PatienceConfig(timeout = Span(5, Seconds), interval = Span(5, Millis))

  val backendBaseUrl = "/si-protected-user-list-admin"
  val frontEndBaseUrl = "/account-protection-tools/protected-user-list"
  implicit val mp: MessagesProvider = MessagesImpl(Lang("en"), inject[MessagesApi])

  def mockSessionCookie: WSCookie = {

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
      override def name: String = cookie.name
      override def value: String = cookie.value
      override def domain: Option[String] = cookie.domain
      override def path: Option[String] = Some(cookie.path)
      override def maxAge: Option[Long] = cookie.maxAge.map(_.toLong)
      override def secure: Boolean = cookie.secure
      override def httpOnly: Boolean = cookie.httpOnly
    }
  }
}
