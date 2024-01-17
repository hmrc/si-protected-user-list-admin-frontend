/*
 * Copyright 2024 HM Revenue & Customs
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
import play.api.libs.json.Json

trait BaseWireMockStubs {
  protected val backendBaseUrl = "/si-protected-user-list-admin"

  protected def expectUserToBeStrideAuthenticated(clientId: String): Unit = stubFor {
    post("/auth/authorise") willReturn okJson(Json.obj("clientId" -> clientId).toString)
  }

  protected def expectFindEntryToBeSuccessful(protectedUser: ProtectedUserRecord): Unit = stubFor {
    get(s"$backendBaseUrl/entry-id/${protectedUser.entryId}") willReturn okJson(Json.toJsObject(protectedUser).toString)
  }

  protected def expectFindEntryToFailWithNotFound(entryId: String): Unit = stubFor {
    get(s"$backendBaseUrl/entry-id/$entryId") willReturn notFound()
  }
}
