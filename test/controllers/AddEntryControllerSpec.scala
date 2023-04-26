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

import config.{AppConfigModule, SiProtectedUserConfig}
import controllers.actions.StrideAction
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.{FakeRequest, Injecting}
import services.SiProtectedUserListService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.gg.test.UnitSpec
import uk.gov.hmrc.play.bootstrap.tools.Stubs
import util.Generators
import views.Views

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AddEntryControllerSpec extends UnitSpec with Injecting with GuiceOneAppPerSuite with Generators with ScalaFutures {
  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(5, Millis))

  trait Setup {
    val defaultSiProtectedUserConfig = siProtectedUserConfigGen.sample.get
    val defaultAuthStrideEnrolmentsConfigGen = authStrideEnrolmentsConfigGen.sample.get
    val appName = nonEmptyStringGen.sample.get
    val stridePid = nonEmptyStringGen.sample.get

    val mockSiProtectedUserListService = mock[SiProtectedUserListService]
    val mockAuthConnector = mock[AuthConnector]
    val views = inject[Views]
    def addEntryController(siProtectedUserConfig: SiProtectedUserConfig = defaultSiProtectedUserConfig) = {
      new AddEntryController(
        siProtectedUserConfig,
        mockSiProtectedUserListService,
        views,
        Stubs.stubMessagesControllerComponents(),
        new StrideAction(mockAuthConnector, defaultAuthStrideEnrolmentsConfigGen, appName)
      )
    }
    def expectStrideAuthenticated(): Unit = {
      when(mockAuthConnector.authorise[Option[String]](any, any)(any, any)).thenReturn(Future.successful(Some(stridePid)))
    }
  }
  "showAddEntryPage" should {
    "forward to the add entry view" in new Setup {
      expectStrideAuthenticated()

      val result = addEntryController().showAddEntryPage()(FakeRequest()).futureValue
      status(result) shouldBe 200

      val body: String = contentAsString(result)

      body should include("add.page.title")
      body should include("page.header")
      body should include("page.add")
      body should include("page.scp")
      body should include("page.org")

    }
  }
}
