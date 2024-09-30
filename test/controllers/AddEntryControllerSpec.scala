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

import models.Entry
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.{ConflictException, NotFoundException}
import uk.gov.hmrc.play.bootstrap.tools.Stubs

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AddEntryControllerSpec extends BaseControllerSpec {
  private def controller =
    new AddEntryController(
      mockBackendService,
      injectViews,
      Stubs.stubMessagesControllerComponents(),
      stubStrideActions.sample.get,
      inputForms
    )

  "AddEntryController" should {
    "forward to the add entry view when GET /add is called" in {
      expectStrideAuthenticated {
        val result = controller.showAddEntryPage()(FakeRequest().withMethod("GET"))
        status(result) shouldBe OK

        val body = contentAsString(result)
        assertAddPageContainsFormFields(body)
      }
    }

    "Forward to confirmation page when add is successful" in {
      forAll(validRequestEntryGen, protectedUserRecords) { (entry, record) =>
        expectStrideAuthenticated { pid =>
          val requestFields = toRequestFields(entry)
          val expectedEntry = entry.copy(addedByUser = Some(pid))

          when(mockBackendService.addEntry(eqTo(expectedEntry))(any, any)).thenReturn(Future.successful(record))

          val result = controller.submit()(FakeRequest().withFormUrlEncodedBody(requestFields*).withMethod("POST"))

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(
            controllers.routes.SiProtectedUserController.view(record.entryId).url
          )
          verify(mockBackendService).addEntry(eqTo(expectedEntry))(any, any)
        }
      }
    }

    "Return CONFLICT when upstream api indicates a conflict" in {
      forAll(validRequestEntryGen) { entry =>
        expectStrideAuthenticated { pid =>
          val requestFields = toRequestFields(entry)
          val expectedEntry = entry.copy(addedByUser = Some(pid))

          when(mockBackendService.addEntry(eqTo(expectedEntry))(any, any)).thenReturn(Future.failed(new ConflictException("test conflict")))

          val result = controller.submit()(FakeRequest().withFormUrlEncodedBody(requestFields*).withMethod("POST"))
          val body = contentAsString(result)

          status(result) shouldBe CONFLICT
          val html = Jsoup.parse(body)
          val errors = html.select(".govuk-error-summary__list").html()
          errors should include("error.conflict")
        }
      }
    }

    "Return NOT_FOUND when upstream api indicates that credId does not exist" in {
      forAll(validRequestEntryGen) { entry =>
        expectStrideAuthenticated { pid =>
          val requestFields = toRequestFields(entry)
          val expectedEntry = entry.copy(addedByUser = Some(pid))

          when(mockBackendService.addEntry(eqTo(expectedEntry))(any, any)).thenReturn(Future.failed(new NotFoundException("credId does not exist")))

          val result = controller.submit()(FakeRequest().withFormUrlEncodedBody(requestFields*).withMethod("POST"))
          val body = contentAsString(result)

          status(result) shouldBe NOT_FOUND
          val html = Jsoup.parse(body)
          val errors = html.select(".govuk-error-summary__list").html()
          errors should include("form.identityProviderId.doesNotExist")
        }
      }
    }

    "Return BAD_REQUEST when POST /add is called with invalid fields" in {
      expectStrideAuthenticated {
        val result = controller.submit()(FakeRequest().withFormUrlEncodedBody())
        status(result) shouldBe BAD_REQUEST

        val body = contentAsString(result)

        val html = Jsoup.parse(body)
        val errors = html.select(".govuk-error-summary__list").html()
        errors should include("error.required")
      }
    }
  }

  private def assertAddPageContainsFormFields(body: String) = {
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

  private def toRequestFields(entry: Entry) = {
    Seq(
      Some("action" -> entry.action),
      entry.nino.map(n => "nino" -> n),
      entry.sautr.map(s => "sautr" -> s),
      entry.identityProvider.map(s => "identityProvider" -> s),
      entry.identityProviderId.map(s => "identityProviderId" -> s),
      entry.group.map(s => "group" -> s),
      Some("addedByTeam" -> entry.addedByTeam),
      entry.updatedByTeam.map(s => "updatedByTeam" -> s)
    ).flatten
  }
}
