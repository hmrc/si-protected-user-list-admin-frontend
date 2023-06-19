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
import models.Entry
import org.jsoup.Jsoup
import org.scalacheck.Arbitrary
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.test.{FakeRequest, Injecting}
import services.SiProtectedUserListService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.gg.test.UnitSpec
import uk.gov.hmrc.http.ConflictException
import uk.gov.hmrc.play.bootstrap.tools.Stubs
import util.Generators
import views.Views

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AddEntryControllerSpec extends UnitSpec with Injecting with GuiceOneAppPerSuite with Generators with ScalaFutures with ScalaCheckDrivenPropertyChecks {
  import Arbitrary.arbitrary

  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(5, Millis))

  trait Setup {
    val defaultSiProtectedUserConfig = siProtectedUserConfigGen.sample.get
    val defaultAuthStrideEnrolmentsConfigGen = authStrideEnrolmentsConfigGen.sample.get
    val appName = nonEmptyStringGen.sample.get
    val stridePid = nonEmptyStringGen.sample.get

    val mockSiProtectedUserListService = mock[SiProtectedUserListService]
    val mockAuthConnector = mock[AuthConnector]
    val views = inject[Views]
    def addEntryController = {
      new AddEntryController(
        mockSiProtectedUserListService,
        views,
        Stubs.stubMessagesControllerComponents(),
        new StrideAction(appName, defaultAuthStrideEnrolmentsConfigGen, mockAuthConnector)
      )
    }
    def expectStrideAuthenticated(): Unit = {
      when(mockAuthConnector.authorise[Option[String]](any, any)(any, any)).thenReturn(Future.successful(Some(stridePid)))
    }

    def assertAddPageContainsFormFields(body: String): Unit = {
      body should include("add.page.title")
      body should include("entry.form.action")
      body should include("entry.form.nino")
      body should include("entry.form.sautr")
      body should include("entry.form.identityProvider")
      body should include("entry.form.identityProviderId")
      body should include("entry.form.addedByTeam")
      body should include("add.submit.button")
      body should include("cancel.button")
    }

    def toRequestFields(entry: Entry): Seq[(String, String)] = {
      Seq(
        Some("action" -> entry.action),
        entry.nino.map(n => "nino" -> n),
        entry.sautr.map(s => "sautr" -> s),
        entry.identityProvider.map(s => "identityProvider" -> s),
        entry.identityProviderId.map(s => "identityProviderId" -> s),
        entry.group.map(s => "group" -> s),
        entry.addedByTeam.map(s => "addedByTeam" -> s),
        entry.updatedByTeam.map(s => "updatedByTeam" -> s)
      ).flatten
    }
  }

  "AddEntryController" should {
    "forward to the add entry view when GET /add is called" in new Setup {
      expectStrideAuthenticated()

      val result = addEntryController.showAddEntryPage()(FakeRequest().withMethod("GET")).futureValue
      status(result) shouldBe OK

      val body = contentAsString(result)
      assertAddPageContainsFormFields(body)
    }

    "Forward to confirmation page when add is successful" in new Setup {
      forAll(validRequestEntryGen, arbitrary) { (entry, protectedUserRecord) =>
        expectStrideAuthenticated()
        val requestFields = toRequestFields(entry)
        val expectedEntry = entry.copy(addedByUser = Some(stridePid))

        when(mockSiProtectedUserListService.addEntry(eqTo(expectedEntry))(*)).thenReturn(Future.successful(protectedUserRecord))

        val result = addEntryController.submit()(FakeRequest().withFormUrlEncodedBody(requestFields: _*).withMethod("POST"))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(
          controllers.routes.SiProtectedUserController.view(protectedUserRecord.entryId).url
        )
        verify(mockSiProtectedUserListService).addEntry(eqTo(expectedEntry))(*)
      }
    }

    "Return CONFLICT when upstream api indicates a conflict" in new Setup {
      forAll(validRequestEntryGen) { entry =>
        expectStrideAuthenticated()
        val requestFields = toRequestFields(entry)
        val expectedEntry = entry.copy(addedByUser = Some(stridePid))

        when(mockSiProtectedUserListService.addEntry(eqTo(expectedEntry))(*)).thenReturn(Future.failed(new ConflictException("test conflict")))

        val result = addEntryController.submit()(FakeRequest().withFormUrlEncodedBody(requestFields: _*).withMethod("POST"))
        val body = contentAsString(result)

        status(result) shouldBe CONFLICT
        val html = Jsoup.parse(body)
        val errors = html.select(".govuk-error-summary__list").html()
        errors should include("error.conflict")
      }
    }

    "Return BAD_REQUEST when POST /add is called with invalid fields" in new Setup {
      expectStrideAuthenticated()
      val result = addEntryController.submit()(FakeRequest().withFormUrlEncodedBody())
      status(result) shouldBe BAD_REQUEST

      val body = contentAsString(result)

      val html = Jsoup.parse(body)
      val errors = html.select(".govuk-error-summary__list").html()
      errors should include("error.required")
    }

  }
}
