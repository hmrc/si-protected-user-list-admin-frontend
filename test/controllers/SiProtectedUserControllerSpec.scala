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

import org.jsoup.Jsoup
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalacheck.Gen
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.retrieve.{Name, ~}
import uk.gov.hmrc.auth.core.{InsufficientEnrolments, MissingBearerToken}
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames}
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.bootstrap.tools.Stubs

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SiProtectedUserControllerSpec extends BaseControllerSpec {

  private def siProtectedUserController =
    new SiProtectedUserController(
      stubStrideActions.sample.get,
      mockBackendService,
      injectViews,
      Stubs.stubMessagesControllerComponents(),
      inputForms,
      mockAuditConnector,
      "si-protected-user-list-admin-frontend"
    )

  "homepage" should {
    "display the correct html page" in {
      forAll(Gen listOf protectedUserRecords) { listOfRecords =>
        expectStrideAuthenticated() { (_, _) =>
          when(mockAuditConnector.sendEvent(any)(any, any)).thenReturn(Future.successful(AuditResult.Success))
          when(mockBackendService.findEntries(any[Option[String]], any[Option[String]])(any[HeaderCarrier])).thenReturn(Future(listOfRecords))

          val result = await(siProtectedUserController.homepage()(FakeRequest()))
          status(result) shouldBe OK
          val body = contentAsString(result)
          body should include("home.page.title")
        }
      }
    }

    "send correct audit event" in {
      expectStrideAuthenticated(forceStridePid = false) { (stridePidOpt, nameOpt) =>
        when(mockAuditConnector.sendEvent(any)(any, any)).thenReturn(Future.successful(AuditResult.Success))
        when(mockBackendService.findEntries(any[Option[String]], any[Option[String]])(any[HeaderCarrier])).thenReturn(Future(Seq.empty))

        val result = await(siProtectedUserController.homepage()(FakeRequest()))

        val dataEventCaptor = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(mockAuditConnector).sendEvent(dataEventCaptor.capture())(any, any)
        val dataEvent = dataEventCaptor.getValue

        dataEvent.auditSource shouldBe "si-protected-user-list-admin-frontend"
        dataEvent.auditType   shouldBe "ViewProtectedUserList"
        dataEvent.tags shouldBe Map(
          "clientIP"                   -> "-",
          "path"                       -> "/",
          HeaderNames.xSessionId       -> "-",
          HeaderNames.akamaiReputation -> "-",
          HeaderNames.xRequestId       -> "-",
          HeaderNames.deviceID         -> "-",
          "clientPort"                 -> "-",
          "transactionName"            -> "HMRC - SI Protected User List Admin - Home Page - view all users in the list"
        )
        dataEvent.detail shouldBe Map(
          "pid"  -> stridePidOpt.getOrElse("Unknown_User_Pid"),
          "name" -> nameOpt.map(name => s"${name.name.getOrElse("")} ${name.lastName.getOrElse("")}".trim).getOrElse("-")
        )

        status(result) shouldBe OK
        val body = contentAsString(result)
        body should include("home.page.title")
      }
    }

    "redirect to sign in when no active session" in {
      forAll(Gen listOf protectedUserRecords) { listOfRecords =>
        when(mockBackendService.findEntries(any[Option[String]], any[Option[String]])(any[HeaderCarrier])).thenReturn(Future(listOfRecords))
        when(mockAuthConnector.authorise[Option[String] ~ Option[Name]](any, any)(any, any)).thenReturn(Future.failed(MissingBearerToken()))

        val result = await(siProtectedUserController.homepage()(FakeRequest()))
        status(result) shouldBe SEE_OTHER
      }
    }

    "return unauthorized when insufficient privs" in {
      forAll(Gen listOf protectedUserRecords) { listOfRecords =>
        when(mockBackendService.findEntries(any[Option[String]], any[Option[String]])(any[HeaderCarrier])).thenReturn(Future(listOfRecords))
        when(mockAuthConnector.authorise[Option[String] ~ Option[Name]](any, any)(any, any)).thenReturn(Future.failed(InsufficientEnrolments()))

        val result = await(siProtectedUserController.homepage()(FakeRequest()))
        status(result) shouldBe UNAUTHORIZED
      }
    }
  }

  "search" should {
    "display results from backend" in {
      forAll(Gen listOf protectedUserRecords) { listOfRecords =>
        expectStrideAuthenticated() { (_, _) =>
          when(mockAuditConnector.sendEvent(any)(any, any)).thenReturn(Future.successful(AuditResult.Success))
          when(mockBackendService.findEntries(any[Option[String]], any[Option[String]])(any[HeaderCarrier])).thenReturn(Future(listOfRecords))

          val teams = "All" +: appConfig.siProtectedUserConfig.addedByTeams

          val filterByTeam = Gen.option(Gen.oneOf(teams)).sample.get.map("filterByTeam" -> _)
          val searchQuery = nonEmptyStringGen.sample.map("searchQuery" -> _)

          val formData: Seq[(String, String)] = filterByTeam.toSeq ++ searchQuery.toSeq

          val result = await(siProtectedUserController.search()(FakeRequest().withFormUrlEncodedBody(formData*).withMethod("POST")))
          status(result) shouldBe OK
          val body = contentAsString(result)

          val html = Jsoup.parse(body)
          val entryCount = html.select("table tbody tr").size()

          body         should include("home.page.title")
          entryCount shouldBe listOfRecords.size
        }
      }
    }

    "send correct audit event" in {
      expectStrideAuthenticated(forceStridePid = false) { (stridePidOpt, nameOpt) =>
        when(mockAuditConnector.sendEvent(any)(any, any)).thenReturn(Future.successful(AuditResult.Success))
        when(mockBackendService.findEntries(any[Option[String]], any[Option[String]])(any[HeaderCarrier])).thenReturn(Future(Seq.empty))

        val teams = "All" +: appConfig.siProtectedUserConfig.addedByTeams

        val filterByTeam = Gen.option(Gen.oneOf(teams)).sample.get.map("filterByTeam" -> _)
        val searchQuery = nonEmptyStringGen.sample.map("searchQuery" -> _)

        val formData: Seq[(String, String)] = filterByTeam.toSeq ++ searchQuery.toSeq

        val result = await(siProtectedUserController.search()(FakeRequest().withFormUrlEncodedBody(formData*).withMethod("POST")))

        val dataEventCaptor = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(mockAuditConnector).sendEvent(dataEventCaptor.capture())(any, any)
        val dataEvent = dataEventCaptor.getValue

        dataEvent.auditSource shouldBe "si-protected-user-list-admin-frontend"
        dataEvent.auditType   shouldBe "SearchProtectedUserList"
        dataEvent.tags shouldBe Map(
          "clientIP"                   -> "-",
          "path"                       -> "/",
          HeaderNames.xSessionId       -> "-",
          HeaderNames.akamaiReputation -> "-",
          HeaderNames.xRequestId       -> "-",
          HeaderNames.deviceID         -> "-",
          "clientPort"                 -> "-",
          "transactionName"            -> "HMRC - SI Protected User List Admin - Search - filter users in the list"
        )
        dataEvent.detail shouldBe Map(
          "pid"  -> stridePidOpt.getOrElse("Unknown_User_Pid"),
          "name" -> nameOpt.map(name => s"${name.name.getOrElse("")} ${name.lastName.getOrElse("")}".trim).getOrElse("-"),
          filterByTeam.getOrElse("filterByTeam" -> "-"),
          searchQuery.getOrElse("searchQuery"   -> "-")
        )

        status(result) shouldBe OK
        val body = contentAsString(result)
        body should include("home.page.title")
      }
    }
  }

  "view" should {
    "Retrieve user and forward to details template" in {
      forAll(protectedUserRecords) { record =>
        expectStrideAuthenticated() { (_, _) =>
          when(mockBackendService.findEntry(eqTo(record.entryId))(any)).thenReturn(Future.successful(Some(record)))

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
    }

    "Forward to error page with NOT_FOUND when entry doesnt exist" in {
      forAll(protectedUserRecords) { record =>
        expectStrideAuthenticated() { (_, _) =>
          when(mockBackendService.findEntry(eqTo(record.entryId))(any)).thenReturn(Future.successful(None))

          val result = siProtectedUserController.view(record.entryId)(FakeRequest())
          status(result) shouldBe NOT_FOUND
          val body = contentAsString(result)
          body should include("error.not.found")
          body should include("protectedUser.details.not.found")
        }
      }
    }

    "Forward to error page with INTERNAL_SERVER_ERROR when there is an exception" in {
      forAll(protectedUserRecords) { record =>
        expectStrideAuthenticated() { (_, _) =>
          when(mockBackendService.findEntry(eqTo(record.entryId))(any)).thenReturn(Future.failed(new Exception("some exception")))

          val result = siProtectedUserController.view(record.entryId)(FakeRequest())

          status(result)        shouldBe INTERNAL_SERVER_ERROR
          contentAsString(result) should include("something.wrong.heading")
        }
      }
    }
  }
}
