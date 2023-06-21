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

import controllers.base.StrideAction
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalacheck.Gen
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.test.FakeRequest
import services.SiProtectedUserListService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.gg.test.UnitSpec
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.tools.Stubs
import util.Generators

import scala.concurrent.Future

class SiProtectedUserControllerSpec extends UnitSpec with GuiceOneAppPerSuite with ScalaCheckDrivenPropertyChecks {
  import SiProtectedUserControllerSpec._

  import concurrent.ExecutionContext.Implicits.global

  private val siProtectedUserController =
    new SiProtectedUserController(
      new StrideAction(appName, defaultAuthStrideEnrolmentsConfigGen, mockAuthConnector),
      mockBackendService,
      app.injector.instanceOf[views.Views],
      Stubs.stubMessagesControllerComponents()
    )

  "homepage" should {
    "display the correct html page" in {
      forAll(Gen listOf protectedUserRecords) { listOfRecords =>
        when {
          mockBackendService.findEntries(any[Option[String]], any[Option[String]])(any[HeaderCarrier])
        } thenReturn Future(listOfRecords)

        val result = await(siProtectedUserController.homepage(None, None)(FakeRequest()))
        status(result) shouldBe OK
        val body = contentAsString(result)
        body should include("home.page.title")
      }
    }
  }

  "view" should {
    "Retrieve user and forward to details template" in {
      forAll(protectedUserRecords) { record =>
        when(mockBackendService.findEntry(eqTo(record.entryId))(*)).thenReturn(Future.successful(Some(record)))

        val result = await(siProtectedUserController.view(entryId = record.entryId)(FakeRequest()))
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

    "Forward to error page with NOT_FOUND when entry doesnt exist" in {
      forAll(protectedUserRecords) { record =>
        when(mockBackendService.findEntry(eqTo(record.entryId))(*)).thenReturn(Future.successful(None))
        val result = await(siProtectedUserController.view(record.entryId)(FakeRequest()))
        status(result) shouldBe NOT_FOUND
        val body = contentAsString(result)
        body should include("error.not.found")
        body should include("protectedUser.details.not.found")
      }
    }

    "Forward to error page with INTERNAL_SERVER_ERROR when there is an exception" in {
      forAll(protectedUserRecords) { record =>
        when(mockBackendService.findEntry(eqTo(record.entryId))(*)).thenReturn(Future.failed(new Exception("some exception")))
        val result = await(siProtectedUserController.view(record.entryId)(FakeRequest()))
        status(result) shouldBe INTERNAL_SERVER_ERROR
        val body = contentAsString(result)
        body should include("error.internal_server_error")
      }
    }
  }
}
object SiProtectedUserControllerSpec extends MockitoSugar with ArgumentMatchersSugar with Generators {
  private val defaultAuthStrideEnrolmentsConfigGen = authStrideEnrolmentsConfigGen.sample.get
  private val appName = nonEmptyStringGen.sample.get

  private val mockAuthConnector = mock[AuthConnector]
  when(mockAuthConnector.authorise[Option[String]](any, any)(any, any)) thenReturn Future.successful(Some("stride-pid"))

  private val mockBackendService = mock[SiProtectedUserListService]
}
