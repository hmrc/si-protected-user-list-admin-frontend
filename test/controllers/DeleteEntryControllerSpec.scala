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
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.http.Status
import play.api.test.{FakeRequest, Injecting}
import services.SiProtectedUserListService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.gg.test.UnitSpec
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.tools.Stubs
import util.Generators
import views.Views

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DeleteEntryControllerSpec extends UnitSpec with Injecting with GuiceOneAppPerSuite with Generators with ScalaFutures with ScalaCheckDrivenPropertyChecks {
  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(5, Millis))

  trait Setup {
    val defaultSiProtectedUserConfig = siProtectedUserConfigGen.sample.get
    val defaultAuthStrideEnrolmentsConfigGen = authStrideEnrolmentsConfigGen.sample.get
    val appName = nonEmptyStringGen.sample.get
    val stridePid = nonEmptyStringGen.sample.get

    val mockSiProtectedUserListService = mock[SiProtectedUserListService]
    val mockAuthConnector = mock[AuthConnector]
    val views = inject[Views]

    def deleteEntryController(siProtectedUserConfig: SiProtectedUserConfig = defaultSiProtectedUserConfig) = {
      new DeleteEntryController(
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
  "DeleteEntryController" should {
    "forward to the delete confirmation page view when GET /add is called" in new Setup {
      forAll(protectedUserRecordGen) { protectedUserRecord =>
        expectStrideAuthenticated()
        when(mockSiProtectedUserListService.findEntry(eqTo(protectedUserRecord.entryId))(*)).thenReturn(Future.successful(Some(protectedUserRecord)))

        val result = deleteEntryController().showConfirmDeletePage(protectedUserRecord.entryId)(FakeRequest().withMethod("GET")).futureValue
        status(result) shouldBe OK

        val body = contentAsString(result)
        body should include("confirm.delete.entry.title")
        body should include("confirm.delete.entry.header")
        body should include("protectedUser.details.entryId")
        body should include("protectedUser.details.status")
        body should include("protectedUser.details.identityProvider")
        body should include("protectedUser.details.identityProviderId")
        body should include("protectedUser.details.group")
        body should include("protectedUser.details.addedByTeam")
        body should include("protectedUser.details.addedOn")
        body should include("protectedUser.details.updatedOn")
        body should include("continue.button")
        body should include("cancel.button")

      }
    }

    "Forward to delete success page when delete is successful" in new Setup {
      forAll(protectedUserRecordGen) { protectedUserRecord =>
        expectStrideAuthenticated()
        when(mockSiProtectedUserListService.deleteEntry(eqTo(protectedUserRecord.entryId))(*)).thenReturn(Future.successful(Right(HttpResponse(Status.NO_CONTENT, ""))))

        val result = deleteEntryController().delete(protectedUserRecord.entryId)(FakeRequest().withMethod("DELETE"))

        status(result) shouldBe OK
        val body = contentAsString(result)
        body should include("delete.entry.success.title")
        body should include("delete.entry.success.header")
        body should include("delete.entry.success.body")

      }
    }

    "Forward to error page when delete is unsuccessful" in new Setup {
      forAll(protectedUserRecordGen) { protectedUserRecord =>
        expectStrideAuthenticated()
        when(mockSiProtectedUserListService.deleteEntry(eqTo(protectedUserRecord.entryId))(*)).thenReturn(Future.successful(Left(UpstreamErrorResponse("not found", NOT_FOUND))))

        val result = deleteEntryController().delete(protectedUserRecord.entryId)(FakeRequest().withMethod("DELETE"))

        status(result) shouldBe NOT_FOUND
        val body = contentAsString(result)
        body should include("delete.entry.not.found")
        body should include("delete.entry.already.deleted")
      }
    }

  }

}
