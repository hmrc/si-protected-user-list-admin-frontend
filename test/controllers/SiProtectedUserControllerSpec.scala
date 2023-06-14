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

import connectors.SiProtectedUserAdminBackendConnector
import controllers.base.StrideAction
import models.ProtectedUserRecord
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.test.FakeRequest
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
      mockBackendConnector,
      app.injector.instanceOf[views.Views],
      Stubs.stubMessagesControllerComponents()
    )

  "homepage" should {
    "display the correct html page" in {
      forAll { protectedUserRecords: Seq[ProtectedUserRecord] =>
        when {
          mockBackendConnector.findEntries()(any[HeaderCarrier])
        } thenReturn Future(protectedUserRecords)

        val result = await(siProtectedUserController.homepage()(FakeRequest()))
        status(result) shouldBe OK
        val body = contentAsString(result)
        body should include("home.page.title")
      }
    }
  }

  "view" should {
    "Retrieve user and forward to details template" in {
      forAll { protectedUser: ProtectedUserRecord =>
        when(mockBackendConnector.findEntry(eqTo(protectedUser.entryId))(*)).thenReturn(Future.successful(Some(protectedUser)))

        val result = await(siProtectedUserController.view(entryId = protectedUser.entryId)(FakeRequest()))
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
      forAll { pu: ProtectedUserRecord =>
        when(mockBackendConnector.findEntry(eqTo(pu.entryId))(*)).thenReturn(Future.successful(None))
        val result = await(siProtectedUserController.view(pu.entryId)(FakeRequest()))
        status(result) shouldBe NOT_FOUND
        val body = contentAsString(result)
        body should include("error.not.found")
        body should include("protectedUser.details.not.found")
      }
    }

    "Forward to error page with INTERNAL_SERVER_ERROR when there is an exception" in {
      forAll { protectedUserRecord: ProtectedUserRecord =>
        when(mockBackendConnector.findEntry(eqTo(protectedUserRecord.entryId))(*)).thenReturn(Future.failed(new Exception("some exception")))
        val result = await(siProtectedUserController.view(protectedUserRecord.entryId)(FakeRequest()))
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

  private val mockBackendConnector = mock[SiProtectedUserAdminBackendConnector]

}
