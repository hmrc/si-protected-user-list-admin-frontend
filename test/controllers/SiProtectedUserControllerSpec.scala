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
import connectors.SiProtectedUserListAdminConnector
import controllers.actions.StrideAction
import models.User
import org.mockito.captor.ArgCaptor
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.Files.{SingletonTemporaryFileCreator, TemporaryFile}
import play.api.mvc._
import play.api.test.{FakeRequest, Injecting}
import services.{AllowListSessionCache, DataProcessService}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.gg.test.UnitSpec
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.bootstrap.tools.Stubs
import util.Generators
import views.Views

import java.io.PrintWriter
import scala.concurrent.{ExecutionContext, Future}

class SiProtectedUserControllerSpec extends UnitSpec with Injecting with GuiceOneAppPerSuite with Generators {
  implicit val hc: HeaderCarrier = HeaderCarrier()

  trait Setup {
    private implicit val ec: ExecutionContext = inject[ExecutionContext]
    val defaultSiProtectedUserConfig = siProtectedUserConfigGen.sample.get
    val defaultAuthStrideEnrolmentsConfigGen = authStrideEnrolmentsConfigGen.sample.get
    val appName = nonEmptyStringGen.sample.get

    val mockAuthConnector: AuthConnector = mock[AuthConnector]
    when(mockAuthConnector.authorise[Option[String]](any, any)(any, any)) thenReturn Future.successful(Some("stride-pid"))
    val mockAudit = mock[AuditConnector]
    val mockAdminConnector = mock[SiProtectedUserListAdminConnector]
    val mockAllowlistCache = mock[AllowListSessionCache]

    val mockDataProcessService = mock[DataProcessService]

    val auditEventCaptor = ArgCaptor[DataEvent]

    val views = inject[Views]
    def siProtectedUserController(siProtectedUserConfig: SiProtectedUserConfig = defaultSiProtectedUserConfig): SiProtectedUserController =
      new SiProtectedUserController(
        siProtectedUserConfig,
        mockAllowlistCache,
        mockDataProcessService,
        mockAudit,
        mockAdminConnector,
        views,
        Stubs.stubMessagesControllerComponents(),
        new StrideAction(mockAuthConnector, defaultAuthStrideEnrolmentsConfigGen, appName)
      )(ExecutionContext.Implicits.global)
  }

  def writeTempFile(text: String, fileName: Option[String] = None, extension: Option[String] = None): TemporaryFile = {
    val tempFile = SingletonTemporaryFileCreator.create(fileName.getOrElse("prefix-"), extension.getOrElse("-suffix"))
    tempFile.deleteOnExit()
    new PrintWriter(tempFile) {
      try {
        write(text)
      } finally {
        close()
      }
    }
    tempFile
  }

  "upload" should {
    "return service unavailble if the siprotecteduser.allowlist.bulkupload.screen.enabled is set to false" in new Setup {

      val controller = siProtectedUserController(defaultSiProtectedUserConfig.copy(bulkUploadScreenEnabled = false))

      val lines: String = "UserId,OrganisationName,RequesterEmail\n01 23 45 67 89 01,\"some,org\",some@email.com"
      val createdFile: TemporaryFile = writeTempFile(lines, None, Some(".csv"))
      val filePart = new MultipartFormData.FilePart[TemporaryFile]("file", "text-to-upload.csv", None, createdFile)
      val formDataBody: MultipartFormData[TemporaryFile] = new MultipartFormData[TemporaryFile](
        dataParts = Map.empty,
        files = Seq(filePart),
        badParts = Nil
      )

      val result: Result = await(controller.upload()(FakeRequest().withBody(formDataBody)))
      status(result) shouldBe 503
    }

    "return no file found if a file is uploaded but not mapped to the correct key" in new Setup {

      val lines = "UserId,OrganisationName,RequesterEmail\n01 23 45 67 89 01,\"some,org\",some@email.com"
      val createdFile: TemporaryFile = writeTempFile(lines, None, Some(".csv"))
      val filePart = new MultipartFormData.FilePart[TemporaryFile]("file", "text-to-upload.csv", None, createdFile)
      val formDataBody: MultipartFormData[TemporaryFile] = new MultipartFormData[TemporaryFile](
        dataParts = Map.empty,
        files = Seq(filePart),
        badParts = Nil
      )

      val result: Result = await(siProtectedUserController().upload()(FakeRequest().withBody(formDataBody)))
      status(result)                        shouldBe 303
      result.newFlash.flatMap(_.get("error")) should contain("Only .csv files are supported. Please try again")
    }

    "return 303 with the flashing error message notCsv when the file is not a csv file" in new Setup {

      val lines = "sometext"
      val createdFile: TemporaryFile = writeTempFile(lines, None, Some(".txt"))
      val filePart = new MultipartFormData.FilePart[TemporaryFile]("csvfile", "csvfile.txt", Some("text"), createdFile)
      val formDataBody: MultipartFormData[TemporaryFile] = new MultipartFormData[TemporaryFile](
        dataParts = Map.empty,
        files = Seq(filePart),
        badParts = Nil
      )

      val result: Result = await(siProtectedUserController().upload()(FakeRequest().withBody(formDataBody)))
      status(result)                        shouldBe 303
      result.newFlash.flatMap(_.get("error")) should contain("Only .csv files are supported. Please try again")
    }

    "return 200 if the file has been processed correctly" in new Setup {

      when(mockDataProcessService.processBulkData(any, any, any)(any)).thenReturn(Left((0, 0, 0)))

      val lines = "UserID,OrganisationName,RequesterEmail\n01 23 45 67 89 01,\"some,org\",some@email.com"
      val createdFile: TemporaryFile = writeTempFile(lines, None, Some(".csv"))
      val filePart = new MultipartFormData.FilePart[TemporaryFile]("csvfile", "csvfile.csv", Some("text/csv"), createdFile)
      val formDataBody: MultipartFormData[TemporaryFile] = new MultipartFormData[TemporaryFile](
        dataParts = Map.empty,
        files = Seq(filePart),
        badParts = Nil
      )

      val result = await(siProtectedUserController().upload()(FakeRequest().withBody(formDataBody)))
      status(result) shouldBe 200
    }

    "return no file found if there is no file in the post body" in new Setup {
      val lines = ""
      val createdFile: TemporaryFile = writeTempFile(lines, None, Some(".csv"))
      val filePart = new MultipartFormData.FilePart[TemporaryFile]("csvfile", "", Some("application/octet-stream"), createdFile)
      val formDataBody: MultipartFormData[TemporaryFile] = new MultipartFormData[TemporaryFile](
        dataParts = Map.empty,
        files = Seq(filePart),
        badParts = Nil
      )

      val result: Result = await(siProtectedUserController().upload()(FakeRequest().withBody(formDataBody)))
      status(result)                        shouldBe 303
      result.newFlash.flatMap(_.get("error")) should contain("Only .csv files are supported. Please try again")
    }

    "return formatError if the headers aren't correct in the csv file" in new Setup {

      val lines = "U,OrganisationName,RequesterEmail\n01 23 45 67 89 01,\"some,org\",some@email.com"
      val createdFile: TemporaryFile = writeTempFile(lines, None, Some(".csv"))
      val filePart = new MultipartFormData.FilePart[TemporaryFile]("csvfile", "text-to-upload.csv", Some("text/csv"), createdFile)
      val formDataBody: MultipartFormData[TemporaryFile] = new MultipartFormData[TemporaryFile](
        dataParts = Map.empty,
        files = Seq(filePart),
        badParts = Nil
      )

      val result: Result = await(siProtectedUserController().upload()(FakeRequest().withBody(formDataBody)))
      status(result) shouldBe 303
      result.newFlash.flatMap(_.get("error")) should contain(
        "1. Check the header row exists AND contains the case-sensitive string: UserID,OrganisationName,RequesterEmail"
      )
    }
  }

  "showSearchForm" should {
    "display the correct html page" in new Setup {

      val result: Result = await(siProtectedUserController().showSearchForm()(FakeRequest()))
      status(result) shouldBe 200
      val body: String = contentAsString(result)
      body should include("search.header")
    }
  }

  "handleSearchRequest" should {
    "show delete confirmation page if found" in new Setup {
      val loginUser = User("something", "orgname", "some@email.com")
      when(mockAdminConnector.findEntry(any)(any)).thenReturn(Future.successful(loginUser))
      val req = FakeRequest()
        .withFormUrlEncodedBody(
          "name"            -> "123456789012",
          "org"             -> "somethingElse",
          "requester_email" -> "some@email.com"
        )
        .withMethod("POST")
      val result = await(siProtectedUserController().handleSearchRequest()(req))
      status(result) shouldBe 200
      val body = contentAsString(result)
      body should include("delete.confirm.header")
    }

    "show search form again if not found" in new Setup {
      when(mockAdminConnector.findEntry(any)(any)).thenReturn(Future.failed(UpstreamErrorResponse("", 404)))

      val req = FakeRequest()
        .withFormUrlEncodedBody(
          "name"            -> "123456789012",
          "org"             -> "somethingElse",
          "requester_email" -> "some@email.com"
        )
        .withMethod("POST")

      val result: Result = await(siProtectedUserController().handleSearchRequest()(req))
      status(result) shouldBe NOT_FOUND

      val body: String = contentAsString(result)
      body should include("search.header")
      body should include("form.username.search")
    }
  }

  "handleDeleteConfirmation" should {
    "show delete complete page if deleted" in new Setup {
      when(mockAdminConnector.deleteUserEntry(any)(any)).thenReturn(Future.successful(()))

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest()
        .withSession("userId" -> "someUserId")
        .withFormUrlEncodedBody(
          "name"            -> "123456789012",
          "org"             -> "somethingElse",
          "requester_email" -> "some@email.com"
        )
        .withMethod("POST")

      val result: Result = await(siProtectedUserController().handleDeleteConfirmation()(req))
      status(result) shouldBe 200

      val body: String = contentAsString(result)
      body should include("delete.complete.header")
      verify(mockAudit).sendEvent(auditEventCaptor.capture)(any[HeaderCarrier], any[ExecutionContext])

      val event: DataEvent = auditEventCaptor.value

      event.auditSource               shouldEqual "si-protected-user-list-admin-frontend"
      event.auditType                 shouldEqual "ListUpdate"
      event.detail.get("strideUserPid")  shouldBe Some("stride-pid")
      event.detail.get("operation")      shouldBe Some("delete")
      event.detail.get("success")        shouldBe Some("true")
      event.detail.get("orgLoginId")     shouldBe Some("123456789012")
      event.detail.get("orgName")        shouldBe Some("somethingElse")
      event.detail.get("requesterEmail") shouldBe Some("some@email.com")
    }

    "show confirmation page again if not found" in new Setup {
      when(mockAdminConnector.deleteUserEntry(any)(any)).thenReturn(Future.failed(UpstreamErrorResponse("", 404)))

      val req = FakeRequest()
        .withSession("userId" -> "someUserId")
        .withFormUrlEncodedBody(
          "name"            -> "123456789012",
          "org"             -> "somethingElse",
          "requester_email" -> "some@email.com"
        )
        .withMethod("POST")

      val result = await(siProtectedUserController().handleDeleteConfirmation()(req))
      status(result) shouldBe NOT_FOUND

      val body = contentAsString(result)
      body should include("search.header")
      body should include("form.username.search")
      verify(mockAudit).sendEvent(auditEventCaptor.capture)(any[HeaderCarrier], any[ExecutionContext])

      val event = auditEventCaptor.value

      event.auditSource               shouldEqual "si-protected-user-list-admin-frontend"
      event.auditType                 shouldEqual "ListUpdate"
      event.detail.get("strideUserPid")  shouldBe Some("stride-pid")
      event.detail.get("operation")      shouldBe Some("delete")
      event.detail.get("success")        shouldBe Some("false")
      event.detail.get("failureReason")  shouldBe Some("Record did not exist")
      event.detail.get("orgLoginId")     shouldBe Some("123456789012")
      event.detail.get("orgName")        shouldBe Some("somethingElse")
      event.detail.get("requesterEmail") shouldBe Some("some@email.com")

    }
  }

  "sortAllAllowlistedUsers" should {
    "return not found if siprotecteduser.allowlist.show.all.enabled is false" in new Setup {
      val controller = siProtectedUserController(defaultSiProtectedUserConfig.copy(showAllEnabled = false))
      val result: Result = await(controller.sortAllAllowlistedUsers()(FakeRequest()))
      status(result) shouldBe NOT_FOUND
    }

    "show the sort page if siprotecteduser.allowlist.show.all.enabled is true" in new Setup {

      val controller = siProtectedUserController(defaultSiProtectedUserConfig.copy(showAllEnabled = true, shutterService = false))
      val result: Result = await(controller.sortAllAllowlistedUsers()(FakeRequest()))
      status(result) shouldBe OK

      val body: String = contentAsString(result)
      body should include("all.users.sort.userid")
      body should include("all.users.sort.organisation")
    }
  }

  "getAllAllowlist" should {
    "return not found if siprotecteduser.allowlist.show.all.enabled is false" in new Setup {

      val controller = siProtectedUserController(defaultSiProtectedUserConfig.copy(showAllEnabled = false))
      val result = await(controller.getAllAllowlist(false)(FakeRequest()))
      status(result) shouldBe NOT_FOUND
    }

    "return the allowlist entries page if siprotecteduser.allowlist.show.all.enabled is true" in new Setup {
      when(mockAdminConnector.getAllEntries()(any))
        .thenReturn(Future.successful(List(User("someUsername", "someOrgName", "some@email.com"))))

      val controller = siProtectedUserController(defaultSiProtectedUserConfig.copy(showAllEnabled = true))
      val result = await(controller.getAllAllowlist()(FakeRequest()))
      status(result) shouldBe 200
      val body = contentAsString(result)
      body should include("someUsername")
      body should include("someOrgName")
      body should include("some@email.com")
    }

    "return the allowlist entries page if siprotecteduser.allowlist.show.all.enabled is true and truncate list based on config when sort by org is false" in new Setup {
      when(mockAdminConnector.getAllEntries()(any))
        .thenReturn(
          Future.successful(
            List(
              User("someUsername", "someOrgName", "some@email.com"),
              User("someUsername1", "someOrgName", "some@email.com"),
              User("someUsername2", "someOrgName", "some@email.com")
            )
          )
        )

      val controller = siProtectedUserController(defaultSiProtectedUserConfig.copy(listScreenRowLimit = 1))
      val result: Result = await(controller.getAllAllowlist(false)(FakeRequest()))
      status(result) shouldBe 200
      val body: String = contentAsString(result)
      body should include("someUsername")
      body should include("someOrgName")
      body should include("some@email.com")

      body should not include "someUsername1"
      body should not include "someUsername2"
    }

    "return the allowlist entries page if siprotecteduser.allowlist.show.all.enabled is true and truncate list based on config when sort by org is true" in new Setup {
      when(mockAdminConnector.getAllEntries()(any))
        .thenReturn(
          Future.successful(
            List(
              User("someUsername", "someOrgName1", "some@email.com"),
              User("someUsername1", "someOrgName2", "some@email.com"),
              User("someUsername2", "someOrgName3", "some@email.com")
            )
          )
        )

      val controller = siProtectedUserController(defaultSiProtectedUserConfig.copy(listScreenRowLimit = 2))
      val result: Result = await(controller.getAllAllowlist()(FakeRequest()))
      status(result) shouldBe 200
      val body: String = contentAsString(result)
      body should include("someUsername")
      body should include("someOrgName1")
      body should include("some@email.com")

      body should include("someUsername1")
      body should include("someOrgName2")
      body should include("some@email.com")

      body should not include "someOrgName3"
    }
  }
}
