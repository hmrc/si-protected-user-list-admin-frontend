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
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.test.{FakeRequest, Injecting}
import services.SiProtectedUserListService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.gg.test.UnitSpec
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.tools.Stubs
import util.Generators
import views.Views

import scala.concurrent.{ExecutionContext, Future}

class SiProtectedUserControllerSpec extends UnitSpec with Injecting with GuiceOneAppPerSuite with Generators with ScalaCheckDrivenPropertyChecks {
  implicit val hc: HeaderCarrier = HeaderCarrier()

  trait Setup {
    private implicit val ec: ExecutionContext = inject[ExecutionContext]
    val defaultSiProtectedUserConfig = siProtectedUserConfigGen.sample.get
    val defaultAuthStrideEnrolmentsConfigGen = authStrideEnrolmentsConfigGen.sample.get
    val appName = nonEmptyStringGen.sample.get

    val mockAuthConnector: AuthConnector = mock[AuthConnector]
    when(mockAuthConnector.authorise[Option[String]](any, any)(any, any)).thenReturn(Future.successful(Some("stride-pid")))
    val mockProtectedUserService = mock[SiProtectedUserListService]
    val views = inject[Views]

    def siProtectedUserController(siProtectedUserConfig: SiProtectedUserConfig = defaultSiProtectedUserConfig): SiProtectedUserController =
      new SiProtectedUserController(
        siProtectedUserConfig,
        mockProtectedUserService,
        views,
        Stubs.stubMessagesControllerComponents(),
        new StrideAction(mockAuthConnector, defaultAuthStrideEnrolmentsConfigGen, appName)
      )(ExecutionContext.Implicits.global)
  }

  "homepage" should {
    "display the correct html page" in new Setup {
      val result = await(siProtectedUserController().homepage()(FakeRequest()))
      status(result) shouldBe OK
      val body = contentAsString(result)
      body should include("home.page.title")
    }
  }

  "view" should {
    "Retrieve user and forward to details template" in new Setup {
      forAll(protectedUserRecordGen) { protectedUser =>
        when(mockProtectedUserService.findEntry(eqTo(protectedUser.entryId))(*)).thenReturn(Future.successful(Some(protectedUser)))

        val result = await(siProtectedUserController().view(entryId = protectedUser.entryId)(FakeRequest()))
        status(result) shouldBe OK
        val body = contentAsString(result)
        body should include("view.entry.title")
        body should include("view.entry.header")
        body should include("protectedUser.details.entryId")
        body should include("protectedUser.details.status")
        body should include("protectedUser.details.identityProvider")
        body should include("protectedUser.details.identityProviderId")
        body should include("protectedUser.details.group")
        body should include("protectedUser.details.addedByTeam")
        body should include("protectedUser.details.addedOn")
        body should include("protectedUser.details.updatedOn")
        body should include("edit.button")
        body should include("delete.button")
      }
    }

    "Forward to error page with NOT_FOUND when entry doesnt exist" in new Setup {
      forAll(protectedUserRecordGen) { pu =>
        when(mockProtectedUserService.findEntry(eqTo(pu.entryId))(*)).thenReturn(Future.successful(None))
        val result = await(siProtectedUserController().view(pu.entryId)(FakeRequest()))
        status(result) shouldBe NOT_FOUND
        val body = contentAsString(result)
        body should include("error.not.found")
        body should include("protectedUser.details.not.found")
      }
    }

    "Forward to error page with INTERNAL_SERVER_ERROR when there is an exception" in new Setup {
      forAll(protectedUserRecordGen) { protectedUserRecord =>
        when(mockProtectedUserService.findEntry(eqTo(protectedUserRecord.entryId))(*)).thenReturn(Future.failed(new Exception("some exception")))
        val result = await(siProtectedUserController().view(protectedUserRecord.entryId)(FakeRequest()))
        status(result) shouldBe INTERNAL_SERVER_ERROR
        val body = contentAsString(result)
        body should include("error.internal_server_error")
      }
    }

  }

}
