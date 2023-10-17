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
import play.api.test.FakeRequest
import uk.gov.hmrc.http.{ConflictException, NotFoundException}
import uk.gov.hmrc.play.bootstrap.tools.Stubs

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EditEntryControllerSpec extends BaseControllerSpec {
  private def editEntryController = new EditEntryController(
    mockBackendService,
    injectViews,
    Stubs.stubMessagesControllerComponents(),
    stubStrideActions.sample.get,
    inputForms
  )

  "EditEntryController" should {
    "forward to the edit entry view when GET /add is called" in
      forAll(validEditEntryGen, protectedUserRecords) { (entry, record) =>
        expectStrideAuthenticated {
          when(mockBackendService.findEntry(eqTo(entry.entryId.value))(*)).thenReturn(Future.successful(Some(record)))
          val result = editEntryController.showEditEntryPage(entry.entryId.value)(FakeRequest().withMethod("GET"))
          status(result) shouldBe OK

          val body = contentAsString(result)
          assertEditPageContainsFormFields(body)
        }
      }

    "Forward to confirmation page when edit is successful" in
      forAll(validEditEntryGen, protectedUserRecords) { (entry, record) =>
        expectStrideAuthenticated { pid =>
          val requestFields = toEditRequestFields(entry)
          val expectedEntry = entry.copy(updatedByUser = Some(pid), updatedByTeam = Option(entry.addedByTeam))

          when(mockBackendService.updateEntry(eqTo(expectedEntry))(*, *)).thenReturn(Future.successful(record))

          val result = editEntryController.submit()(FakeRequest().withFormUrlEncodedBody(requestFields: _*).withMethod("POST"))

          status(result) shouldBe OK
          val body = contentAsString(result)
          body should include("edit.success.title")
          body should include("edit.success.body")
        }
      }

    "Return not found when entry to update is not found" in
      forAll(validEditEntryGen) { entry =>
        expectStrideAuthenticated { pid =>
          val requestFields = toEditRequestFields(entry)
          val expectedEntry = entry.copy(updatedByUser = Some(pid), updatedByTeam = Option(entry.addedByTeam))

          when(mockBackendService.updateEntry(eqTo(expectedEntry))(*, *)).thenReturn(Future.failed(new NotFoundException("not found")))

          val result = editEntryController.submit()(FakeRequest().withFormUrlEncodedBody(requestFields: _*).withMethod("POST"))

          status(result) shouldBe NOT_FOUND
          val body = contentAsString(result)
          body should include("edit.error.not.found")
          body should include("edit.error.already.deleted")
        }
      }

    "Return CONFLICT when /edit results in a conflict exception" in
      forAll(validEditEntryGen) { entry =>
        expectStrideAuthenticated { pid =>
          val requestFields = toEditRequestFields(entry)
          val expectedEntry = entry.copy(updatedByUser = Some(pid), updatedByTeam = Option(entry.addedByTeam))

          when(mockBackendService.updateEntry(eqTo(expectedEntry))(*, *)).thenReturn(Future.failed(new ConflictException("conflict")))

          val result = editEntryController.submit()(FakeRequest().withFormUrlEncodedBody(requestFields: _*).withMethod("POST"))

          status(result) shouldBe CONFLICT
          val body = contentAsString(result)
          body should include("edit.error.conflict")
        }
      }

    "Return BAD_REQUEST when POST /edit is called with invalid fields" in
      expectStrideAuthenticated {
        val result = editEntryController.submit()(FakeRequest().withFormUrlEncodedBody())
        status(result) shouldBe BAD_REQUEST

        val body = contentAsString(result)

        val html = Jsoup.parse(body)
        val errors = html.select(".govuk-error-summary__list").html()
        errors should include("error.required")
      }
  }

  private def assertEditPageContainsFormFields(body: String) = {
    body should include("edit.entry.title")
    body should include("edit.entry.header")
    body should include("entry.form.action")
    body should include("entry.form.nino")
    body should include("entry.form.sautr")
    body should include("entry.form.identityProvider")
    body should include("entry.form.identityProviderId")
    body should include("entry.form.addedByTeam")
    body should include("edit.submit.button")
    body should include("cancel.button")
  }

  private def toEditRequestFields(entry: Entry): Seq[(String, String)] =
    Seq(
      entry.entryId.map(e => "entryId" -> e),
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
