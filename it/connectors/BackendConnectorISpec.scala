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

package connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import controllers.BaseISpec
import org.scalatest.EitherValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.libs.json.Json
import uk.gov.hmrc.http.{ConflictException, HeaderCarrier, NotFoundException}
import util.Generators

class BackendConnectorISpec extends BaseISpec with Generators with ScalaCheckDrivenPropertyChecks with ScalaFutures with EitherValues {

  implicit private val headerCarrier: HeaderCarrier = HeaderCarrier()
  private val siProtectedUserAdminBackendConnector = inject[BackendConnector]

  "SiProtectedUserAdminBackendConnector" should {
    "return a ProtectedUserRecord when addEntry is successful" in {
      forAll(protectedUserGen, protectedUserRecords) { (user, userRecord) =>
        stubFor(
          post(urlEqualTo(s"$backendBaseUrl/add"))
            .withRequestBody(equalToJson(Json.toJsObject(user).toString()))
            .willReturn(ok(Json.toJsObject(userRecord).toString()))
        )

        val result = siProtectedUserAdminBackendConnector.insertNew(user).futureValue

        result shouldBe userRecord
      }
    }

    "Fail with conflict exception for addEntry when 409 conflict is returned from the backend api" in
      forAll(protectedUserGen) { user =>
        stubFor(
          post(urlEqualTo(s"$backendBaseUrl/add"))
            .withRequestBody(equalToJson(Json.toJsObject(user).toString()))
            .willReturn(aResponse().withStatus(CONFLICT))
        )

        val result = siProtectedUserAdminBackendConnector.insertNew(user).failed.futureValue
        result shouldBe a[ConflictException]
      }

    "return a ProtectedUserRecord when updateEntry is successful" in
      forAll(protectedUserRecords) { record =>
        stubFor(
          patch(urlEqualTo(s"$backendBaseUrl/update/${record.entryId}"))
            .withRequestBody(equalToJson(Json.toJsObject(record.body).toString()))
            .willReturn(ok(Json.toJsObject(record).toString()))
        )

        val result = siProtectedUserAdminBackendConnector.updateBy(record.entryId, record.body).futureValue

        result shouldBe record
      }

    "Fail with conflict exception for updateEntry when 409 conflict is returned from the backend api" in
      forAll(protectedUserRecords) { record =>
        stubFor(
          patch(urlEqualTo(s"$backendBaseUrl/update/${record.entryId}"))
            .withRequestBody(equalToJson(Json.toJsObject(record.body).toString()))
            .willReturn(aResponse().withStatus(CONFLICT))
        )

        val result = siProtectedUserAdminBackendConnector.updateBy(record.entryId, record.body).failed.futureValue
        result shouldBe a[ConflictException]
      }

    "Fail with not found exception for updateEntry when 404 not found is returned from the backend api" in
      forAll(protectedUserRecords) { record =>
        stubFor(
          patch(urlEqualTo(s"$backendBaseUrl/update/${record.entryId}"))
            .withRequestBody(equalToJson(Json.toJsObject(record.body).toString()))
            .willReturn(aResponse().withStatus(NOT_FOUND))
        )

        val result = siProtectedUserAdminBackendConnector.updateBy(record.entryId, record.body).failed.futureValue
        result shouldBe a[NotFoundException]
      }

    "Return a ProtectedUserRecord for findEntry valid tax id" in
      forAll(protectedUserRecords) { record =>
        record.body.taxId.name.toString
        stubFor(
          get(urlEqualTo(s"$backendBaseUrl/entry-id/${record.entryId}"))
            .willReturn(ok(Json.toJsObject(record).toString()))
        )

        val result = siProtectedUserAdminBackendConnector.findBy(record.entryId).futureValue
        result shouldBe Some(record)
      }

    "Return None for findEntry when api returns 404" in
      forAll(protectedUserRecords) { record =>
        record.body.taxId.name.toString
        stubFor(
          get(urlEqualTo(s"$backendBaseUrl/entry-id/${record.entryId}"))
            .willReturn(aResponse().withStatus(NOT_FOUND))
        )

        val result = siProtectedUserAdminBackendConnector.findBy(record.entryId).futureValue
        result shouldBe None
      }

    "Return NO_CONTENT http response for deleteEntry when successful" in
      forAll(protectedUserRecords) { record =>
        record.body.taxId.name.toString
        stubFor(
          delete(urlEqualTo(s"$backendBaseUrl/entry-id/${record.entryId}"))
            .willReturn(noContent())
        )

        val result = siProtectedUserAdminBackendConnector.deleteBy(record.entryId).futureValue.value
        result.status shouldBe NO_CONTENT
      }

    "Return UpstreamErrorResponse with 404 response when deleteEntry returns 404" in
      forAll(protectedUserRecords) { record =>
        record.body.taxId.name.toString
        stubFor(
          delete(urlEqualTo(s"$backendBaseUrl/entry-id/${record.entryId}"))
            .willReturn(notFound())
        )

        val result = siProtectedUserAdminBackendConnector.deleteBy(record.entryId).futureValue.left.value
        result.statusCode shouldBe NOT_FOUND
      }
  }
}
