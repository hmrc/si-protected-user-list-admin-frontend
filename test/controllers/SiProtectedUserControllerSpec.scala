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

import config.SiProtectedUserConfig
import controllers.actions.StrideAction
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc._
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.gg.test.UnitSpec
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.tools.Stubs
import util.Generators
import views.Views

import scala.concurrent.{ExecutionContext, Future}

class SiProtectedUserControllerSpec extends UnitSpec with Injecting with GuiceOneAppPerSuite with Generators {
  implicit val hc: HeaderCarrier = HeaderCarrier()

  trait Setup {
    private implicit val ec: ExecutionContext = inject[ExecutionContext]
    val defaultSiProtectedUserConfig = siProtectedUserConfigGen.sample.get
    val defaultAuthStrideEnrolmentsConfigGen = authStrideEnrolmentsConfigGen.sample.get
    val appName = nonEmptyStringGen.sample.get

    val mockAuthConnector: AuthConnector = mock[AuthConnector]
    when(mockAuthConnector.authorise[Option[String]](any, any)(any, any)) thenReturn Future.successful(Some("stride-pid"))
    val mockAudit = mock[AuditConnector]

    val views = inject[Views]

    def siProtectedUserController(siProtectedUserConfig: SiProtectedUserConfig = defaultSiProtectedUserConfig): SiProtectedUserController =
      new SiProtectedUserController(
        siProtectedUserConfig,
        views,
        Stubs.stubMessagesControllerComponents(),
        new StrideAction(mockAuthConnector, defaultAuthStrideEnrolmentsConfigGen, appName)
      )(ExecutionContext.Implicits.global)
  }

  "homepage" should {
    "display the correct html page" in new Setup {

      val result: Result = await(siProtectedUserController().homepage()(FakeRequest()))
      status(result) shouldBe 200
      val body: String = contentAsString(result)
      body should include("home.page.title")
    }
  }

}
