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
import org.scalatest.matchers.should
import play.api.libs.Files.{SingletonTemporaryFileCreator, TemporaryFile}
import play.api.libs.json.Json
import play.api.mvc.MultipartFormData._
import play.api.test.ResultExtractors

import java.io.PrintWriter
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

  "searching for deleted username results in search page showing not found message" in new Setup {
    expectUserToBeStrideAuthenticated("123456")
    val response = await(
      wsClient
        .url(resource(s"$backendBaseUrl/search"))
        .withHttpHeaders("Csrf-Token" -> "nocheck")
        .withCookies(mockSessionCookie)
        .post(Map("name" -> "123456789012", "org" -> "some_orgname", "requesterEmail" -> "some@email.com"))
    )
    response.status shouldBe 404
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
