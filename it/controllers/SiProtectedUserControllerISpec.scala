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

import akka.stream.scaladsl.{FileIO, Source}
import com.github.tomakehurst.wiremock.client.WireMock._
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.scalatest.matchers.should
import play.api.i18n.Messages
import play.api.libs.Files.{SingletonTemporaryFileCreator, TemporaryFile}
import play.api.libs.json.Json
import play.api.mvc.MultipartFormData._
import play.api.test.ResultExtractors

import java.io.PrintWriter
import scala.jdk.CollectionConverters._
import scala.util.Random
class SiProtectedUserControllerISpec extends BaseISpec with ResultExtractors with should.Matchers {
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

  "admin frontend" should {
    "process a valid csv file into otp's allowlist" in new Setup {
      expectKeyStoreToReturnSuccessfully()

      val username1 = "01 23 45 67 89 01"
      val username2 = "01 23 45 67 89 02"

      expectInsertUpdateToBeSuccessful("012345678901", "some,org")
      expectInsertUpdateToBeSuccessful("012345678902", "some,org")
      expectInsertUpdateToBeSuccessful("012345678902", "some,org2")
      expectUserToBeStrideAuthenticated("123456")

      val lines: String =
        s"""UserID,OrganisationName,RequesterEmail,
           |$username1,"some,org",some1@email.com,
           |$username2,"some,org",some1@email.com,
           |$username2,"some,org2",some1@email.com""".stripMargin

      val file = writeTempFile(lines, Some("csvfile"), Some(".csv"))

      val source = Source(
        FilePart("csvfile", "csvfile.csv", Some("text/csv"), FileIO.fromPath(file.path)) ::
          DataPart("key", "value") ::
          Nil
      )

      val result = await(
        wsClient
          .url(resource(s"$backendBaseUrl/file-upload"))
          .withHttpHeaders("Csrf-Token" -> "nocheck")
          .withCookies(mockSessionCookie)
          .post(source)
      )
      result.status shouldBe 200
    }
  }

  val username = Random.alphanumeric.take(12).mkString
  val orgName = Random.alphanumeric.take(20).mkString

  "added user should have leading and trailing whitespaces removed from email and organisation" in new Setup {
    expectAddToBeSuccessful(username, orgName)
    expectKeyStoreToReturnSuccessfully()
    expectUserToBeStrideAuthenticated("123456")

    val response = await(
      wsClient
        .url(resource(s"$backendBaseUrl/add"))
        .withHttpHeaders("Csrf-Token" -> "nocheck")
        .withCookies(mockSessionCookie)
        .post(Map("name" -> username, "org" -> orgName, "requester_email" -> "some1@email.com"))
    )
    response.status shouldBe 200
  }

  "ensure that organisation name can not be less than 2 characters" in new Setup {
    val response = await(
      wsClient
        .url(resource(s"$backendBaseUrl/add"))
        .withHttpHeaders("Csrf-Token" -> "nocheck")
        .withCookies(mockSessionCookie)
        .post(Map("name" -> username, "org" -> orgName, "requester_email" -> "some"))
    )
    response.status shouldBe 400
    response.body     should include(Messages("form.requester_email.regex"))
  }

  "ensure that the email when given a invalid email address shows an error to the user" in new Setup {
    val response = await(
      wsClient
        .url(resource(s"$backendBaseUrl/add"))
        .withHttpHeaders("Csrf-Token" -> "nocheck")
        .withCookies(mockSessionCookie)
        .post(Map("name" -> username, "org" -> "1", "requester_email" -> "some@email.com"))
    )
    response.status shouldBe 400
    response.body     should include(Messages("form.org.length.small"))
  }

  "return confirmation of added record to allowlist" in new Setup {
    expectKeyStoreToReturnSuccessfully()
    expectAddToBeSuccessful(username, orgName)
    expectUserToBeStrideAuthenticated("123456")

    val response = await(
      wsClient
        .url(resource(s"$backendBaseUrl/add"))
        .withHttpHeaders("Csrf-Token" -> "nocheck")
        .withCookies(mockSessionCookie)
        .post(Map("name" -> username, "org" -> orgName, "requester_email" -> "some1@email.com"))
    )

    response.status shouldBe 200

    response.body should include(s"$username")
    response.body should include(s"$orgName")
    response.body should include("some1@email.com")

    response.body should not include Messages("form.name.required")
    response.body should not include Messages("form.org.required")
  }

  "return conflict of added record to allowlist" in new Setup {
    expectKeyStoreToReturnSuccessfully()
    expectAddToBeSuccessful(username, orgName)
    expectUserToBeStrideAuthenticated("123456")

    val response = await(
      wsClient
        .url(resource(s"$backendBaseUrl/add"))
        .withHttpHeaders("Csrf-Token" -> "nocheck")
        .withCookies(mockSessionCookie)
        .post(Map("name" -> username, "org" -> orgName, "requester_email" -> "some1@email.com"))
    )

    response.status shouldBe 200

    response.body should include("User record saved to allowlist")
    response.body should include("User ID")
    response.body should include(username)
    response.body should not include Messages("form.name.required")
    response.body should not include Messages("form.org.required")
  }

  "return error when username missing" in new Setup {
    val response = await(
      wsClient
        .url(resource(s"$backendBaseUrl/add"))
        .withHttpHeaders("Csrf-Token" -> "nocheck")
        .withCookies(mockSessionCookie)
        .post(Map("name" -> "", "org" -> orgName, "requester_email" -> "some1@email.com"))
    )

    val document = Jsoup.parse(response.body)

    val element = document.select(".govuk-error-message").text()

    response.status shouldBe 400
    element         shouldBe "Error: This field is required"
  }

  "return failure when organisation missing" in new Setup {
    val response = await(
      wsClient
        .url(resource(s"$backendBaseUrl/add"))
        .withHttpHeaders("Csrf-Token" -> "nocheck")
        .withCookies(mockSessionCookie)
        .post(Map("name" -> username, "org" -> "", "requester_email" -> "some@email.com"))
    )

    val document = Jsoup.parse(response.body)

    val element = document.select(".govuk-error-message").text()

    response.status shouldBe 400

    element shouldBe "Error: This field is required"
  }

  "return failure when organisation too long (over 300)" in new Setup {
    val longOrg = "abc" * 101

    val response = await(
      wsClient
        .url(resource(s"$backendBaseUrl/add"))
        .withHttpHeaders("Csrf-Token" -> "nocheck")
        .withCookies(mockSessionCookie)
        .post(Map("name" -> username, "org" -> longOrg, "requester_email" -> "some@email.com"))
    )

    response.status shouldBe 400

    response.body should not include "This field is required"
    response.body should include(Messages("form.org.length.large"))
  }

  "return failure when both fields are missing" in new Setup {
    val response = await(
      wsClient
        .url(resource(s"$backendBaseUrl/add"))
        .withHttpHeaders("Csrf-Token" -> "nocheck")
        .withCookies(mockSessionCookie)
        .post(Map("name" -> "", "org" -> "", "requester_email" -> "some@email.com"))
    )
    response.status shouldBe 400

    val document = Jsoup.parse(response.body)
    val elements: Seq[Element] = document.select(".govuk-error-message").asScala.toSeq

    elements.foreach {
      _.text shouldBe "Error: This field is required"
    }
  }

  "searching for deleted username results in search page showing not found message" in new Setup {
    val response = await(
      wsClient
        .url(resource(s"$backendBaseUrl/search"))
        .withHttpHeaders("Csrf-Token" -> "nocheck")
        .withCookies(mockSessionCookie)
        .post(Map("name" -> "123456789012", "org" -> "some_orgname", "requesterEmail" -> "some@email.com"))
    )
    response.status shouldBe 404
    response.body     should include("Record Not Found")
  }

  "confirming allowlist entry deletion results in complete page being shown" in new Setup {
    expectDeleteToReturnOk("123456789012")
    expectFindToReturnUser("123456789012", "some_orgname2", "some@email.com")
    expectUserToBeStrideAuthenticated("123456")

    val response = await(
      wsClient
        .url(resource(s"$backendBaseUrl/delete-records"))
        .withHttpHeaders("Csrf-Token" -> "nocheck")
        .withCookies(mockSessionCookie)
        .post(Map("name" -> "123456789012", "org" -> "some_orgname2", "requester_email" -> "some@email.com"))
    )
    response.status shouldBe 200
    response.body     should include("User has been successfully removed from the allowlist")
  }

  "searching for added username results in delete confirmation page showing allowlist entry details" in new Setup {
    expectFindToReturnUser("123456789012", "some_orgname", "some@email.com")

    val response = await(
      wsClient
        .url(resource(s"$backendBaseUrl/search"))
        .withHttpHeaders("Csrf-Token" -> "nocheck")
        .withCookies(mockSessionCookie)
        .post(Map("name" -> "123456789012", "org" -> "some_orgname"))
    )

    response.status shouldBe 200
    response.body     should include("123456789012")
    response.body     should include("some_orgname")
  }

  trait Setup {
    def expectDeleteToReturnOk(username: String): Unit =
      stubFor(
        delete(urlEqualTo(s"$backendBaseUrl/delete/$username"))
          .willReturn(ok())
      )

    def expectFindToReturnUser(username: String, orgName: String, email: String): Unit =
      stubFor(
        get(urlEqualTo(s"$backendBaseUrl/find/$username"))
          .willReturn(okJson(Json.obj("username" -> username, "organisationName" -> orgName, "requesterEmail" -> email).toString()))
      )

    def expectFindToReturnNotFound(username: String): Unit =
      stubFor(
        get(urlEqualTo(s"$backendBaseUrl/find/$username"))
          .willReturn(notFound())
      )

    def expectAddToBeSuccessful(username: String, orgName: String): Unit =
      stubFor(
        post(urlEqualTo(s"$backendBaseUrl/add"))
          .withRequestBody(
            equalToJson(Json.obj("username" -> username, "organisationName" -> orgName, "requesterEmail" -> "some1@email.com").toString())
          )
          .willReturn(ok())
      )

    def expectInsertUpdateToBeSuccessful(username: String, orgName: String): Unit =
      stubFor(
        post(urlEqualTo(s"$backendBaseUrl/insert-update"))
          .withRequestBody(
            equalToJson(Json.obj("username" -> username, "organisationName" -> orgName, "requesterEmail" -> "some1@email.com").toString())
          )
          .willReturn(ok())
      )

    def expectKeyStoreToReturnSuccessfully(): Unit = {
      stubFor(
        get(urlPathMatching("/keystore/si-protected-user-list-admin-frontend/.*"))
          .willReturn(okJson(Json.obj("id" -> "someId", "data" -> Json.obj()).toString()))
      )

      stubFor(
        put(urlPathMatching("/keystore/si-protected-user-list-admin-frontend/.*"))
          .willReturn(okJson(Json.obj("id" -> "someId", "data" -> Json.obj()).toString()))
      )
    }

    def expectUserToBeStrideAuthenticated(pid: String): Unit =
      stubFor(post("/auth/authorise").willReturn(okJson(Json.obj("clientId" -> pid).toString())))
  }
}
