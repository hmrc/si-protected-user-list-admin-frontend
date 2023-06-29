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

import play.api.http.Status
import play.api.test.FakeRequest
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.tools.Stubs

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DeleteEntryControllerSpec extends BaseControllerSpec {
  private val deleteEntryController = {
    new DeleteEntryController(
      mockBackendService,
      injectViews,
      Stubs.stubMessagesControllerComponents(),
      stubStrideActions.sample.get
    )
  }

  "DeleteEntryController" should {
    "forward to the delete confirmation page view when GET /add is called" in {
      forAll(protectedUserRecords) { record =>
        expectStrideAuthenticated {
          when(mockBackendService.findEntry(eqTo(record.entryId))(*)) thenReturn Future.successful(Some(record))

          val result = deleteEntryController.showConfirmDeletePage(record.entryId)(FakeRequest().withMethod("GET"))
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
    }

    "Forward to delete success page when delete is successful" in {
      forAll(protectedUserRecords) { record =>
        expectStrideAuthenticated { _ =>
          when {
            mockBackendService.deleteEntry(eqTo(record.entryId))(*)
          } thenReturn Future.successful(Right(HttpResponse(Status.NO_CONTENT, "")))

          val result = deleteEntryController.delete(record.entryId)(FakeRequest().withMethod("DELETE"))

          status(result) shouldBe OK
          val body = contentAsString(result)
          body should include("delete.entry.success.title")
          body should include("delete.entry.success.header")
          body should include("delete.entry.success.body")
        }
      }
    }

    "Forward to error page when delete is unsuccessful with NOT_FOUND" in {
      forAll(protectedUserRecords) { record =>
        expectStrideAuthenticated {
          when {
            mockBackendService.deleteEntry(eqTo(record.entryId))(*)
          } thenReturn Future.successful(Left(UpstreamErrorResponse("not found", NOT_FOUND)))

          val result = deleteEntryController.delete(record.entryId)(FakeRequest().withMethod("DELETE"))

          status(result) shouldBe NOT_FOUND
          val body = contentAsString(result)
          body should include("delete.entry.not.found")
          body should include("delete.entry.already.deleted")
        }
      }
    }

    "Forward to error page when delete is unsuccessful" in {
      forAll(protectedUserRecords) { record =>
        expectStrideAuthenticated {
          when {
            mockBackendService.deleteEntry(eqTo(record.entryId))(*)
          } thenReturn Future.successful(Left(UpstreamErrorResponse("internal server error", INTERNAL_SERVER_ERROR)))

          val result = deleteEntryController.delete(record.entryId)(FakeRequest().withMethod("DELETE"))

          status(result) shouldBe INTERNAL_SERVER_ERROR
          val body = contentAsString(result)
          body should include("error.internal_server_error")
        }
      }
    }
  }
}
