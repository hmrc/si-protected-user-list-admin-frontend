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

import com.github.tomakehurst.wiremock.client.WireMock._
import models.ProtectedUserRecord
import org.jsoup.Jsoup
import play.api.http.HeaderNames
import play.api.libs.json.Json
import play.api.test.ResultExtractors

class SiProtectedUserControllerISpec extends BaseISpec with ResultExtractors {
  "SiProtectedUserController" should {
    "return OK when view is successful" in new Setup {
      forAll(protectedUserRecords, nonEmptyStringGen) { (record, pid) =>
        expectUserToBeStrideAuthenticated(pid)
        expectFindEntryToBeSuccessful(record)

        val response = wsClient
          .url(resource(s"$frontEndBaseUrl/view-entry/${record.entryId}"))
          .withHttpHeaders("Csrf-Token" -> "nocheck")
          .withCookies(mockSessionCookie)
          .get()
          .futureValue

        response.status shouldBe OK
      }
    }

    "return OK when search is successful" in new Setup {
      forAll(validEditEntryGen, protectedUserRecords, nonEmptyStringGen) { (entry, record, pid) =>
        expectUserToBeStrideAuthenticated(pid)
        expectFindEntriesToBeSuccessful(record, entry.entryId.get)

        val body = Map[String, Seq[String]]("filterByTeam" -> Seq("All"), "searchQuery" -> Seq(s"${entry.entryId.get}"))

        val response = wsClient
          .url(resource(s"$frontEndBaseUrl/search"))
          .withHttpHeaders("Csrf-Token" -> "nocheck", HeaderNames.CONTENT_TYPE -> "application/x-www-form-urlencoded")
          .withCookies(mockSessionCookie)
          .withFollowRedirects(false)
          .post(body)
          .futureValue

        response.status shouldBe OK
      }
    }

    "return BAD_REQUEST when a search term hasn't been entered" in new Setup {
      forAll(nonEmptyStringGen) { pid =>
        expectUserToBeStrideAuthenticated(pid)

        val body = Map[String, Seq[String]]("filterByTeam" -> Seq("All"))

        val response = wsClient
          .url(resource(s"$frontEndBaseUrl/search"))
          .withHttpHeaders("Csrf-Token" -> "nocheck", HeaderNames.CONTENT_TYPE -> "application/x-www-form-urlencoded")
          .withCookies(mockSessionCookie)
          .withFollowRedirects(false)
          .post(body)
          .futureValue

        val document = Jsoup.parse(response.body)

        document.select("a[href*=#searchQuery]").text() shouldBe "Enter a search term"
        response.status                                 shouldBe BAD_REQUEST
      }
    }

    "return BAD_REQUEST when a search term exceeds 64 characters" in new Setup {
      forAll(nonEmptyStringGen) { pid =>
        expectUserToBeStrideAuthenticated(pid)

        val body = Map[String, Seq[String]]("filterByTeam" -> Seq("All"), "searchQuery" -> Seq("a".repeat(65)))

        val response = wsClient
          .url(resource(s"$frontEndBaseUrl/search"))
          .withHttpHeaders("Csrf-Token" -> "nocheck", HeaderNames.CONTENT_TYPE -> "application/x-www-form-urlencoded")
          .withCookies(mockSessionCookie)
          .withFollowRedirects(false)
          .post(body)
          .futureValue

        val document = Jsoup.parse(response.body)

        document.select("a[href*=#searchQuery]").text() shouldBe "Search queries must be 64 characters or less"
        response.status                                 shouldBe BAD_REQUEST
      }
    }

    "return BAD_REQUEST when a search term is invalid" in new Setup {
      forAll(nonEmptyStringGen, disallowedCharStringGen) { (pid, disallowedCharacters) =>
        expectUserToBeStrideAuthenticated(pid)

        val body = Map[String, Seq[String]]("filterByTeam" -> Seq("All"), "searchQuery" -> Seq(disallowedCharacters))

        val response = wsClient
          .url(resource(s"$frontEndBaseUrl/search"))
          .withHttpHeaders("Csrf-Token" -> "nocheck", HeaderNames.CONTENT_TYPE -> "application/x-www-form-urlencoded")
          .withCookies(mockSessionCookie)
          .withFollowRedirects(false)
          .post(body)
          .futureValue

        val document = Jsoup.parse(response.body)

        document
          .select("a[href*=#searchQuery]")
          .text() shouldBe "Search queries must not include a period, plus sign, asterisk, question mark, caret, dollar sign, parenthesis, square bracket, brace, vertical bar, backslash or any control characters"
        response.status shouldBe BAD_REQUEST
      }
    }

    "return BAD_REQUEST when an invalid 'Filter by' option is specified" in new Setup {
      forAll(nonEmptyStringGen) { pid =>
        expectUserToBeStrideAuthenticated(pid)

        val body = Map[String, Seq[String]]("filterByTeam" -> Seq("foo"), "searchQuery" -> Seq("bar"))

        val response = wsClient
          .url(resource(s"$frontEndBaseUrl/search"))
          .withHttpHeaders("Csrf-Token" -> "nocheck", HeaderNames.CONTENT_TYPE -> "application/x-www-form-urlencoded")
          .withCookies(mockSessionCookie)
          .withFollowRedirects(false)
          .post(body)
          .futureValue

        val document = Jsoup.parse(response.body)

        document
          .select("a[href*=#filterByTeam]")
          .text()       shouldBe "Select a valid option"
        response.status shouldBe BAD_REQUEST
      }
    }

    "return NOT_FOUND when entry doesnt exist" in new Setup {
      forAll(protectedUserRecords, nonEmptyStringGen) { (record, pid) =>
        expectUserToBeStrideAuthenticated(pid)
        expectFindEntryToFailWithNotFound(record)

        val response = wsClient
          .url(resource(s"$frontEndBaseUrl/view-entry/${record.entryId}"))
          .withHttpHeaders("Csrf-Token" -> "nocheck")
          .withCookies(mockSessionCookie)
          .get()
          .futureValue

        response.status shouldBe NOT_FOUND
      }
    }

  }

  trait Setup {

    def expectUserToBeStrideAuthenticated(pid: String): Unit = {
      stubFor(post("/auth/authorise").willReturn(okJson(Json.obj("clientId" -> pid).toString())))
    }

    def expectFindEntryToBeSuccessful(protectedUser: ProtectedUserRecord): Unit = {
      stubFor(
        get(urlEqualTo(s"$backendBaseUrl/entry-id/${protectedUser.entryId}"))
          .willReturn(ok(Json.toJsObject(protectedUser).toString()))
      )
    }

    def expectFindEntryToFailWithNotFound(protectedUser: ProtectedUserRecord): Unit = {
      stubFor(
        get(urlEqualTo(s"$backendBaseUrl/entry-id/${protectedUser.entryId}"))
          .willReturn(aResponse().withStatus(NOT_FOUND))
      )
    }

    def expectFindEntriesToBeSuccessful(entry: ProtectedUserRecord, searchQuery: String) = {
      stubFor(
        get(urlEqualTo(s"$backendBaseUrl/record/?searchQuery=$searchQuery"))
          .willReturn(ok(Json.toJson(List(entry)).toString()))
      )
    }
  }
}
