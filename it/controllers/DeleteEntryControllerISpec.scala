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

import play.api.test.ResultExtractors

class DeleteEntryControllerISpec extends BaseISpec with ResultExtractors {
//  "SiProtectedUserController" should {
//    "return OK when confirm-delete successful" in new Setup {
//      forAll(protectedUserRecords, nonEmptyStringGen) { (record, pid) =>
//        expectUserToBeStrideAuthenticated(pid)
//        expectFindEntryToBeSuccessful(record)
//
//        val response = wsClient
//          .url(resource(s"$frontEndBaseUrl/confirm-delete/${record.entryId}"))
//          .withHttpHeaders("Csrf-Token" -> "nocheck")
//          .withCookies(mockSessionCookie)
//          .get()
//          .futureValue
//
//        response.status shouldBe OK
//      }
//    }
//
//    "return OK when entry is deleted" in new Setup {
//      forAll(protectedUserRecords, nonEmptyStringGen) { (record, pid) =>
//        expectUserToBeStrideAuthenticated(pid)
//        expectDeleteEntryToBeSuccessful(record)
//
//        val response = wsClient
//          .url(resource(s"$frontEndBaseUrl/delete-entry/${record.entryId}"))
//          .withHttpHeaders("Csrf-Token" -> "nocheck")
//          .withCookies(mockSessionCookie)
//          .get()
//          .futureValue
//
//        response.status shouldBe OK
//      }
//    }
//
//    "return NOT_FOUND when no entry is found to delete" in new Setup {
//      forAll(protectedUserRecords, nonEmptyStringGen) { (record, pid) =>
//        expectUserToBeStrideAuthenticated(pid)
//        expectDeleteEntryToFailWithNotFound(record)
//
//        val response = wsClient
//          .url(resource(s"$frontEndBaseUrl/delete-entry/${record.entryId}"))
//          .withHttpHeaders("Csrf-Token" -> "nocheck")
//          .withCookies(mockSessionCookie)
//          .delete()
//          .futureValue
//
//        response.status shouldBe NOT_FOUND
//      }
//    }
//
//  }
//
//  trait Setup {
//
//    def expectUserToBeStrideAuthenticated(pid: String): Unit = {
//      stubFor(post("/auth/authorise").willReturn(okJson(Json.obj("clientId" -> pid).toString())))
//    }
//
//    def expectFindEntryToBeSuccessful(protectedUser: ProtectedUserRecord): Unit = {
//      stubFor(
//        get(urlEqualTo(s"$backendBaseUrl/entry-id/${protectedUser.entryId}"))
//          .willReturn(ok(Json.toJsObject(protectedUser).toString()))
//      )
//    }
//
//    def expectDeleteEntryToBeSuccessful(protectedUser: ProtectedUserRecord): Unit = {
//      stubFor(
//        delete(urlEqualTo(s"$backendBaseUrl/entry-id/${protectedUser.entryId}"))
//          .willReturn(aResponse().withStatus(NO_CONTENT))
//      )
//    }
//
//    def expectDeleteEntryToFailWithNotFound(protectedUser: ProtectedUserRecord): Unit = {
//      stubFor(
//        delete(urlEqualTo(s"$backendBaseUrl/entry-id/${protectedUser.entryId}"))
//          .willReturn(aResponse().withStatus(NOT_FOUND))
//      )
//    }
//  }
}
