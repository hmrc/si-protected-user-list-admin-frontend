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

import config.AppConfig
import connectors.SiProtectedUserListAdminConnector
import models.User
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.mockito.captor.ArgCaptor
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.Files.{SingletonTemporaryFileCreator, TemporaryFile}
import play.api.mvc._
import play.api.test.{FakeRequest, Injecting}
import services.{AllowListSessionCache, DataProcessService}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.gg.test.UnitSpec
import uk.gov.hmrc.http.{ConflictException, HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.tools.Stubs
import views.Views

import java.io.PrintWriter
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

class SiProtectedUserControllerSpec extends UnitSpec with Injecting with GuiceOneAppPerSuite {
  implicit val hc: HeaderCarrier = HeaderCarrier()

  trait Setup {
    val mockAppConfig = mock[AppConfig]
    val mockAudit = mock[AuditConnector]
    val mockAdminConnector = mock[SiProtectedUserListAdminConnector]
    val mockAllowlistCache = mock[AllowListSessionCache]
    val mockServicesConfig = mock[ServicesConfig]
    val mockDataProcessService = mock[DataProcessService]
    val mockAuthConnector = mock[AuthConnector]

    val auditEventCaptor = ArgCaptor[DataEvent]

    val siProtectedUserController: SiProtectedUserController = new SiProtectedUserController(
      mockAllowlistCache,
      mockDataProcessService,
      mockAudit,
      mockAdminConnector,
      inject[Views],
      Stubs.stubMessagesControllerComponents(),
      mockAuthConnector
    )(ExecutionContext.Implicits.global, mockAppConfig, mockServicesConfig)
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
      when(mockServicesConfig.getBoolean("siprotecteduser.allowlist.bulkupload.screen.enabled")).thenReturn(false)

      val lines: String = "UserId,OrganisationName,RequesterEmail\n01 23 45 67 89 01,\"some,org\",some@email.com"
      val createdFile: TemporaryFile = writeTempFile(lines, None, Some(".csv"))
      val filePart = new MultipartFormData.FilePart[TemporaryFile]("file", "text-to-upload.csv", None, createdFile)
      val formDataBody: MultipartFormData[TemporaryFile] = new MultipartFormData[TemporaryFile](
        dataParts = Map.empty,
        files = Seq(filePart),
        badParts = Nil
      )

      val result: Result = await(siProtectedUserController.upload()(FakeRequest().withBody(formDataBody)))
      status(result) shouldBe 503
    }

    "return no file found if a file is uploaded but not mapped to the correct key" in new Setup {
      when(mockServicesConfig.getBoolean("siprotecteduser.allowlist.bulkupload.screen.enabled")).thenReturn(true)

      val lines = "UserId,OrganisationName,RequesterEmail\n01 23 45 67 89 01,\"some,org\",some@email.com"
      val createdFile: TemporaryFile = writeTempFile(lines, None, Some(".csv"))
      val filePart = new MultipartFormData.FilePart[TemporaryFile]("file", "text-to-upload.csv", None, createdFile)
      val formDataBody: MultipartFormData[TemporaryFile] = new MultipartFormData[TemporaryFile](
        dataParts = Map.empty,
        files = Seq(filePart),
        badParts = Nil
      )

      val result: Result = await(siProtectedUserController.upload()(FakeRequest().withBody(formDataBody)))
      status(result)                        shouldBe 303
      result.newFlash.flatMap(_.get("error")) should contain("Only .csv files are supported. Please try again")
    }

    "return 303 with the flashing error message notCsv when the file is not a csv file" in new Setup {
      when(mockServicesConfig.getBoolean("siprotecteduser.allowlist.bulkupload.screen.enabled")).thenReturn(true)

      val lines = "sometext"
      val createdFile: TemporaryFile = writeTempFile(lines, None, Some(".txt"))
      val filePart = new MultipartFormData.FilePart[TemporaryFile]("csvfile", "csvfile.txt", Some("text"), createdFile)
      val formDataBody: MultipartFormData[TemporaryFile] = new MultipartFormData[TemporaryFile](
        dataParts = Map.empty,
        files = Seq(filePart),
        badParts = Nil
      )

      val result: Result = await(siProtectedUserController.upload()(FakeRequest().withBody(formDataBody)))
      status(result)                        shouldBe 303
      result.newFlash.flatMap(_.get("error")) should contain("Only .csv files are supported. Please try again")
    }

    "return 200 if the file has been processed correctly" in new Setup {
      when(mockServicesConfig.getBoolean("siprotecteduser.allowlist.bulkupload.screen.enabled")).thenReturn(true)
      when(mockAppConfig.analyticsHost).thenReturn("analytics-host")
      when(mockAppConfig.analyticsToken).thenReturn("analytics-token")
      when(mockDataProcessService.processBulkData(any, any, any)(any)).thenReturn(Left((0, 0, 0)))
      when(mockAuthConnector.authorise[Option[String]](any, any)(any, any)).thenReturn(Future.successful(Some("stride-pid")))

      val lines = "UserID,OrganisationName,RequesterEmail\n01 23 45 67 89 01,\"some,org\",some@email.com"
      val createdFile: TemporaryFile = writeTempFile(lines, None, Some(".csv"))
      val filePart = new MultipartFormData.FilePart[TemporaryFile]("csvfile", "csvfile.csv", Some("text/csv"), createdFile)
      val formDataBody: MultipartFormData[TemporaryFile] = new MultipartFormData[TemporaryFile](
        dataParts = Map.empty,
        files = Seq(filePart),
        badParts = Nil
      )

      val result = await(siProtectedUserController.upload()(FakeRequest().withBody(formDataBody)))
      status(result) shouldBe 200
    }

    "return no file found if there is no file in the post body" in new Setup {
      when(mockServicesConfig.getBoolean("siprotecteduser.allowlist.bulkupload.screen.enabled")).thenReturn(true)
      val lines = ""
      val createdFile: TemporaryFile = writeTempFile(lines, None, Some(".csv"))
      val filePart = new MultipartFormData.FilePart[TemporaryFile]("csvfile", "", Some("application/octet-stream"), createdFile)
      val formDataBody: MultipartFormData[TemporaryFile] = new MultipartFormData[TemporaryFile](
        dataParts = Map.empty,
        files = Seq(filePart),
        badParts = Nil
      )

      val result: Result = await(siProtectedUserController.upload()(FakeRequest().withBody(formDataBody)))
      status(result)                        shouldBe 303
      result.newFlash.flatMap(_.get("error")) should contain("Only .csv files are supported. Please try again")
    }

    "return formatError if the headers aren't correct in the csv file" in new Setup {
      when(mockServicesConfig.getBoolean("siprotecteduser.allowlist.bulkupload.screen.enabled")).thenReturn(true)

      val lines = "U,OrganisationName,RequesterEmail\n01 23 45 67 89 01,\"some,org\",some@email.com"
      val createdFile: TemporaryFile = writeTempFile(lines, None, Some(".csv"))
      val filePart = new MultipartFormData.FilePart[TemporaryFile]("csvfile", "text-to-upload.csv", Some("text/csv"), createdFile)
      val formDataBody: MultipartFormData[TemporaryFile] = new MultipartFormData[TemporaryFile](
        dataParts = Map.empty,
        files = Seq(filePart),
        badParts = Nil
      )

      val result: Result = await(siProtectedUserController.upload()(FakeRequest().withBody(formDataBody)))
      status(result) shouldBe 303
      result.newFlash.flatMap(_.get("error")) should contain(
        "1. Check the header row exists AND contains the case-sensitive string: UserID,OrganisationName,RequesterEmail"
      )
    }
  }

  "loading the add user to allowlist page" should {
    "reset the 'add multiple users' session" in new Setup {
      when(mockServicesConfig.getBoolean("siprotecteduser.allowlist.shutter.service")).thenReturn(false)
      when(mockAppConfig.analyticsHost).thenReturn("analytics-host")
      when(mockAppConfig.analyticsToken).thenReturn("analytics-token")

      when(mockAllowlistCache.clear()(any)).thenReturn(Future.successful(()))

      val res = await(siProtectedUserController.reload()(FakeRequest()))
      status(res) shouldBe OK

      verify(mockAllowlistCache).clear()(any)
    }

    "return a HTML document with the 'add user to allowlist' form" in new Setup {
      when(mockServicesConfig.getBoolean("siprotecteduser.allowlist.shutter.service")).thenReturn(false)
      when(mockAppConfig.analyticsHost).thenReturn("analytics-host")
      when(mockAppConfig.analyticsToken).thenReturn("analytics-token")

      when(mockAllowlistCache.clear()(any)).thenReturn(Future.successful(()))

      val res: Result = await(siProtectedUserController.reload()(FakeRequest()))
      status(res) shouldBe OK

      val html: Document = Jsoup.parse(contentAsString(res))
      val fieldIds = html.select("input[type=text]").asScala.map(_.id)
      fieldIds should contain("name")
      fieldIds should contain("org")
      fieldIds should contain("requester_email")
    }
  }

  "submitting the 'add user to allowlist' form" should {
    "return 400 Bad Request if the form is invalid" in new Setup {
      when(mockAppConfig.analyticsHost).thenReturn("analytics-host")
      when(mockAppConfig.analyticsToken).thenReturn("analytics-token")
      when(mockAllowlistCache.getAll()(any)).thenReturn(Future.successful(Nil))

      val res: Result = await(siProtectedUserController.submit()(FakeRequest().withHeaders("Csrf-Token" -> "nocheck").withFormUrlEncodedBody()))
      status(res) shouldBe BAD_REQUEST

      val html: Document = Jsoup.parse(contentAsString(res))
      val errors = html.select(".govuk-error-message").asScala
      errors should have size 3
    }

    "return 409 Conflict if the user is already allowlisted" in new Setup {
      when(mockAppConfig.analyticsHost).thenReturn("analytics-host")
      when(mockAppConfig.analyticsToken).thenReturn("analytics-token")
      when(mockAdminConnector.addEntry(any)(any)).thenReturn(Future.failed(new ConflictException("conflict")))
      when(mockAdminConnector.findEntry(any)(any))
        .thenReturn(Future.successful(User("112233445566", "some org", "aa@bb.cc")))
      when(mockAuthConnector.authorise[Option[String]](any, any)(any, any)).thenReturn(Future.successful(Some("stride-pid")))

      val res: Result = await(
        siProtectedUserController.submit()(
          FakeRequest()
            .withSession("userId" -> "someUserId")
            .withFormUrlEncodedBody(
              "name"            -> "112233445566",
              "org"             -> "some org",
              "requester_email" -> "aa@bb.cc"
            )
            .withMethod("POST")
        )
      )
      status(res) shouldBe CONFLICT

      val html: Document = Jsoup.parse(contentAsString(res))
      html.select("#notice").text shouldBe "Entry not added, already exists, see below"
      verify(mockAudit).sendEvent(auditEventCaptor.capture)(any[HeaderCarrier], any[ExecutionContext])

      val event: DataEvent = auditEventCaptor.value

      event.auditSource               shouldEqual "si-protected-user-list-admin-frontend"
      event.auditType                 shouldEqual "ListUpdate"
      event.detail.get("strideUserPid")  shouldBe Some("stride-pid")
      event.detail.get("operation")      shouldBe Some("add")
      event.detail.get("success")        shouldBe Some("false")
      event.detail.get("failureReason")  shouldBe Some("Record already exists")
      event.detail.get("orgLoginId")     shouldBe Some("112233445566")
      event.detail.get("orgName")        shouldBe Some("some org")
      event.detail.get("requesterEmail") shouldBe Some("aa@bb.cc")

    }

    "return 200 Ok, add the user to the allowlist, and clear the 'user ID' field if a valid non-allowlisted user is added" in new Setup {
      val user = User("112233445566", "some org", "aa@bb.cc")
      when(mockAdminConnector.addEntry(any)(any)).thenReturn(Future.unit)
      when(mockAllowlistCache.add(any)(any)).thenReturn(Future.successful(List(user)))
      when(mockAppConfig.analyticsHost).thenReturn("analytics-host")
      when(mockAppConfig.analyticsToken).thenReturn("analytics-token")
      when(mockAuthConnector.authorise[Option[String]](any, any)(any, any)).thenReturn(Future.successful(Some("stride-pid")))

      val res: Result = await(
        siProtectedUserController.submit()(
          FakeRequest()
            .withSession("userId" -> "someUserId")
            .withFormUrlEncodedBody(
              "name"            -> "112233445566",
              "org"             -> "some org",
              "requester_email" -> "aa@bb.cc"
            )
            .withMethod("POST")
        )
      )
      status(res) shouldBe OK

      verify(mockAdminConnector).addEntry(eqTo(user))(any)
      verify(mockAllowlistCache).add(eqTo(user))(any)
      verify(mockAudit).sendEvent(auditEventCaptor.capture)(any[HeaderCarrier], any[ExecutionContext])

      val event: DataEvent = auditEventCaptor.value
      event.auditSource                  shouldEqual "si-protected-user-list-admin-frontend"
      event.auditType                    shouldEqual "ListUpdate"
      event.detail.get("strideUserPid")  shouldEqual Some("stride-pid")
      event.detail.get("operation")      shouldEqual Some("add")
      event.detail.get("success")        shouldEqual Some("true")
      event.detail.get("orgLoginId")     shouldEqual Some("112233445566")
      event.detail.get("orgName")        shouldEqual Some("some org")
      event.detail.get("requesterEmail") shouldEqual Some("aa@bb.cc")

      val html: Document = Jsoup.parse(contentAsString(res))

      val userIdField: Elements = html.select("#name")
      userIdField.`val` shouldBe empty

      val orgNameField: Elements = html.select("#org")
      orgNameField.`val` shouldBe "some org"

      val requesterEmailField: Elements = html.select("#requester_email")
      requesterEmailField.`val` shouldBe "aa@bb.cc"

      html.select("#notice").text shouldBe "User record saved to allowlist"
    }
  }

  "addPage" should {
    "open home page with both buttons for reset" in new Setup {
      when(mockAppConfig.analyticsHost).thenReturn("analytics-host")
      when(mockAppConfig.analyticsToken).thenReturn("analytics-token")
      when(mockServicesConfig.getBoolean("siprotecteduser.allowlist.shutter.service")).thenReturn(false)

      when(mockAllowlistCache.clear()(any)).thenReturn(Future.successful(()))

      val result: Result = await(siProtectedUserController.reload()(FakeRequest()))

      status(result) shouldBe 200

      val body: String = contentAsString(result)

      body should include("home.page.title")
      body should include("page.header")
      body should include("page.add")
      body should include("page.scp")
      body should include("page.org")
    }
  }

  "showSearchForm" should {
    "display the correct html page" in new Setup {
      when(mockServicesConfig.getBoolean("siprotecteduser.allowlist.shutter.service")).thenReturn(false)
      when(mockAppConfig.analyticsHost).thenReturn("analytics-host")
      when(mockAppConfig.analyticsToken).thenReturn("analytics-token")

      val result: Result = await(siProtectedUserController.showSearchForm()(FakeRequest()))
      status(result) shouldBe 200
      val body: String = contentAsString(result)
      body should include("search.header")
    }
  }

  "handleSearchRequest" should {
    "show delete confirmation page if found" in new Setup {
      when(mockAppConfig.analyticsHost).thenReturn("analytics-host")
      when(mockAppConfig.analyticsToken).thenReturn("analytics-token")

      val loginUser = User("something", "orgname", "some@email.com")
      when(mockAdminConnector.findEntry(any)(any)).thenReturn(Future.successful(loginUser))
      val req = FakeRequest()
        .withFormUrlEncodedBody(
          "name"            -> "123456789012",
          "org"             -> "somethingElse",
          "requester_email" -> "some@email.com"
        )
        .withMethod("POST")
      val result = await(siProtectedUserController.handleSearchRequest()(req))
      status(result) shouldBe 200
      val body = contentAsString(result)
      body should include("delete.confirm.header")
    }

    "show search form again if not found" in new Setup {
      when(mockAdminConnector.findEntry(any)(any)).thenReturn(Future.failed(UpstreamErrorResponse("", 404)))
      when(mockAppConfig.analyticsHost).thenReturn("analytics-host")
      when(mockAppConfig.analyticsToken).thenReturn("analytics-token")

      val req = FakeRequest()
        .withFormUrlEncodedBody(
          "name"            -> "123456789012",
          "org"             -> "somethingElse",
          "requester_email" -> "some@email.com"
        )
        .withMethod("POST")

      val result: Result = await(siProtectedUserController.handleSearchRequest()(req))
      status(result) shouldBe NOT_FOUND

      val body: String = contentAsString(result)
      body should include("search.header")
      body should include("form.username.search")
    }
  }

  "handleDeleteConfirmation" should {
    "show delete complete page if deleted" in new Setup {
      when(mockAdminConnector.deleteUserEntry(any)(any)).thenReturn(Future.successful(()))
      when(mockAppConfig.analyticsHost).thenReturn("analytics-host")
      when(mockAppConfig.analyticsToken).thenReturn("analytics-token")
      when(mockAuthConnector.authorise[Option[String]](any, any)(any, any)).thenReturn(Future.successful(Some("stride-pid")))

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest()
        .withSession("userId" -> "someUserId")
        .withFormUrlEncodedBody(
          "name"            -> "123456789012",
          "org"             -> "somethingElse",
          "requester_email" -> "some@email.com"
        )
        .withMethod("POST")

      val result: Result = await(siProtectedUserController.handleDeleteConfirmation()(req))
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
      when(mockAppConfig.analyticsHost).thenReturn("analytics-host")
      when(mockAppConfig.analyticsToken).thenReturn("analytics-token")
      when(mockAuthConnector.authorise[Option[String]](any, any)(any, any)).thenReturn(Future.successful(Some("stride-pid")))

      val req = FakeRequest()
        .withSession("userId" -> "someUserId")
        .withFormUrlEncodedBody(
          "name"            -> "123456789012",
          "org"             -> "somethingElse",
          "requester_email" -> "some@email.com"
        )
        .withMethod("POST")

      val result = await(siProtectedUserController.handleDeleteConfirmation()(req))
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
      when(mockServicesConfig.getBoolean("siprotecteduser.allowlist.shutter.service")).thenReturn(false)
      when(mockServicesConfig.getBoolean("siprotecteduser.allowlist.show.all.enabled")).thenReturn(false)

      val result: Result = await(siProtectedUserController.sortAllAllowlistedUsers()(FakeRequest()))
      status(result) shouldBe NOT_FOUND
    }

    "show the sort page if siprotecteduser.allowlist.show.all.enabled is true" in new Setup {
      when(mockServicesConfig.getBoolean("siprotecteduser.allowlist.shutter.service")).thenReturn(false)
      when(mockServicesConfig.getBoolean("siprotecteduser.allowlist.show.all.enabled")).thenReturn(true)
      when(mockAppConfig.analyticsHost).thenReturn("analytics-host")
      when(mockAppConfig.analyticsToken).thenReturn("analytics-token")

      val result: Result = await(siProtectedUserController.sortAllAllowlistedUsers()(FakeRequest()))
      status(result) shouldBe OK

      val body: String = contentAsString(result)
      body should include("all.users.sort.userid")
      body should include("all.users.sort.organisation")
    }
  }

  "getAllAllowlist" should {
    "return not found if siprotecteduser.allowlist.show.all.enabled is false" in new Setup {
      when(mockServicesConfig.getBoolean("siprotecteduser.allowlist.show.all.enabled")).thenReturn(false)

      val result = await(siProtectedUserController.getAllAllowlist(false)(FakeRequest()))
      status(result) shouldBe NOT_FOUND
    }

    "return the allowlist entries page if siprotecteduser.allowlist.show.all.enabled is true" in new Setup {
      when(mockServicesConfig.getBoolean("siprotecteduser.allowlist.show.all.enabled")).thenReturn(true)
      when(mockServicesConfig.getInt("siprotecteduser.allowlist.listscreen.rowlimit")).thenReturn(1)
      when(mockAdminConnector.getAllEntries()(any))
        .thenReturn(Future.successful(List(User("someUsername", "someOrgName", "some@email.com"))))
      when(mockAppConfig.analyticsHost).thenReturn("analytics-host")
      when(mockAppConfig.analyticsToken).thenReturn("analytics-token")

      val result = await(siProtectedUserController.getAllAllowlist()(FakeRequest()))
      status(result) shouldBe 200
      val body = contentAsString(result)
      body should include("someUsername")
      body should include("someOrgName")
      body should include("some@email.com")
    }

    "return the allowlist entries page if siprotecteduser.allowlist.show.all.enabled is true and truncate list based on config when sort by org is false" in new Setup {
      when(mockServicesConfig.getBoolean("siprotecteduser.allowlist.show.all.enabled")).thenReturn(true)
      when(mockServicesConfig.getInt("siprotecteduser.allowlist.listscreen.rowlimit")).thenReturn(1)
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
      when(mockAppConfig.analyticsHost).thenReturn("analytics-host")
      when(mockAppConfig.analyticsToken).thenReturn("analytics-token")

      val result: Result = await(siProtectedUserController.getAllAllowlist(false)(FakeRequest()))
      status(result) shouldBe 200
      val body: String = contentAsString(result)
      body should include("someUsername")
      body should include("someOrgName")
      body should include("some@email.com")

      body should not include "someUsername1"
      body should not include "someUsername2"
    }

    "return the allowlist entries page if siprotecteduser.allowlist.show.all.enabled is true and truncate list based on config when sort by org is true" in new Setup {
      when(mockServicesConfig.getBoolean("siprotecteduser.allowlist.show.all.enabled")).thenReturn(true)
      when(mockServicesConfig.getInt("siprotecteduser.allowlist.listscreen.rowlimit")).thenReturn(2)
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
      when(mockAppConfig.analyticsHost).thenReturn("analytics-host")
      when(mockAppConfig.analyticsToken).thenReturn("analytics-token")

      val result: Result = await(siProtectedUserController.getAllAllowlist()(FakeRequest()))
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

  "Shuttered Service" should {
    "return Service is unavailable on the home page" in new Setup {
      when(mockServicesConfig.getBoolean("siprotecteduser.allowlist.shutter.service")).thenReturn(true)
      when(mockAppConfig.analyticsHost).thenReturn("analytics-host")
      when(mockAppConfig.analyticsToken).thenReturn("analytics-token")

      val result: Result = await(siProtectedUserController.homepage()(FakeRequest()))
      status(result) shouldBe 200
      val body: String = contentAsString(result)
      body should include("This service is shuttered and currently unavailable")
    }

    "return Service is unavailable on the add page" in new Setup {
      when(mockServicesConfig.getBoolean("siprotecteduser.allowlist.shutter.service")).thenReturn(true)
      when(mockAppConfig.analyticsHost).thenReturn("analytics-host")
      when(mockAppConfig.analyticsToken).thenReturn("analytics-token")

      val result: Result = await(siProtectedUserController.reload()(FakeRequest()))
      status(result) shouldBe 200
      val body: String = contentAsString(result)
      body should include("This service is shuttered and currently unavailable")
    }

    "return Service is unavailable on the upload page" in new Setup {
      when(mockServicesConfig.getBoolean("siprotecteduser.allowlist.shutter.service")).thenReturn(true)
      when(mockAppConfig.analyticsHost).thenReturn("analytics-host")
      when(mockAppConfig.analyticsToken).thenReturn("analytics-token")

      val result: Result = await(siProtectedUserController.fileUploadPage()(FakeRequest()))
      status(result) shouldBe 200
      val body: String = contentAsString(result)
      body should include("This service is shuttered and currently unavailable")
    }

    "return Service is unavailable on the sort-by page" in new Setup {
      when(mockServicesConfig.getBoolean("siprotecteduser.allowlist.shutter.service")).thenReturn(true)
      when(mockAppConfig.analyticsHost).thenReturn("analytics-host")
      when(mockAppConfig.analyticsToken).thenReturn("analytics-token")

      val result: Result = await(siProtectedUserController.sortAllAllowlistedUsers()(FakeRequest()))
      status(result) shouldBe 200
      val body: String = contentAsString(result)
      body should include("This service is shuttered and currently unavailable")
    }

    "return Service is unavailable on the show-find-form page" in new Setup {
      when(mockServicesConfig.getBoolean("siprotecteduser.allowlist.shutter.service")).thenReturn(true)
      when(mockAppConfig.analyticsHost).thenReturn("analytics-host")
      when(mockAppConfig.analyticsToken).thenReturn("analytics-token")

      val result: Result = await(siProtectedUserController.showSearchForm()(FakeRequest()))
      status(result) shouldBe 200
      val body: String = contentAsString(result)
      body should include("This service is shuttered and currently unavailable")
    }
  }
}
