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
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.libs.json.Json
import uk.gov.hmrc.http.{ConflictException, HeaderCarrier, NotFoundException}
import util.Generators

class SiProtectedUserAdminBackendConnectorISpec extends BaseISpec with Generators with ScalaCheckDrivenPropertyChecks with ScalaFutures with EitherValues {
  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(5, Millis))

  trait Setup {
    implicit val headerCarrier = HeaderCarrier()
    val siProtectedUserAdminBackendConnector = inject[SiProtectedUserAdminBackendConnector]

  }

  "SiProtectedUserAdminBackendConnector" should {
    "return a ProtectedUserRecord when addEntry is successful" in new Setup {
      forAll(protectedUserGen, protectedUserRecordGen) { (user, userRecord) =>
        stubFor(
          post(urlEqualTo(s"$backendBaseUrl/add"))
            .withRequestBody(equalToJson(Json.toJsObject(user).toString()))
            .willReturn(ok(Json.toJsObject(userRecord).toString()))
        )

        val result = siProtectedUserAdminBackendConnector.addEntry(user).futureValue

        result shouldBe userRecord
      }
    }

    "Fail with conflict exception for addEntry when 409 conflict is returned from the backend api" in new Setup {
      forAll(protectedUserGen) { user =>
        stubFor(
          post(urlEqualTo(s"$backendBaseUrl/add"))
            .withRequestBody(equalToJson(Json.toJsObject(user).toString()))
            .willReturn(aResponse().withStatus(CONFLICT))
        )

        val result = siProtectedUserAdminBackendConnector.addEntry(user).failed.futureValue
        result shouldBe a[ConflictException]
      }
    }

    "return a ProtectedUserRecord when updateEntry is successful" in new Setup {
      forAll(protectedUserRecordGen) { userRecord =>
        stubFor(
          patch(urlEqualTo(s"$backendBaseUrl/update/${userRecord.entryId}"))
            .withRequestBody(equalToJson(Json.toJsObject(userRecord.body).toString()))
            .willReturn(ok(Json.toJsObject(userRecord).toString()))
        )

        val result = siProtectedUserAdminBackendConnector.updateEntry(userRecord.entryId, userRecord.body).futureValue

        result shouldBe userRecord
      }
    }

    "Fail with conflict exception for updateEntry when 409 conflict is returned from the backend api" in new Setup {
      forAll(protectedUserRecordGen) { protectedUserRecord =>
        stubFor(
          patch(urlEqualTo(s"$backendBaseUrl/update/${protectedUserRecord.entryId}"))
            .withRequestBody(equalToJson(Json.toJsObject(protectedUserRecord.body).toString()))
            .willReturn(aResponse().withStatus(CONFLICT))
        )

        val result = siProtectedUserAdminBackendConnector.updateEntry(protectedUserRecord.entryId, protectedUserRecord.body).failed.futureValue
        result shouldBe a[ConflictException]
      }
    }

    "Fail with not found exception for updateEntry when 404 not found is returned from the backend api" in new Setup {
      forAll(protectedUserRecordGen) { protectedUserRecord =>
        stubFor(
          patch(urlEqualTo(s"$backendBaseUrl/update/${protectedUserRecord.entryId}"))
            .withRequestBody(equalToJson(Json.toJsObject(protectedUserRecord.body).toString()))
            .willReturn(aResponse().withStatus(NOT_FOUND))
        )

        val result = siProtectedUserAdminBackendConnector.updateEntry(protectedUserRecord.entryId, protectedUserRecord.body).failed.futureValue
        result shouldBe a[NotFoundException]
      }
    }

    "Return a ProtectedUserRecord for findEntry valid tax id" in new Setup {
      forAll(protectedUserRecordGen) { protectedUser =>
        protectedUser.body.taxId.name.toString
        stubFor(
          get(urlEqualTo(s"$backendBaseUrl/entry-id/${protectedUser.entryId}"))
            .willReturn(ok(Json.toJsObject(protectedUser).toString()))
        )

        val result = siProtectedUserAdminBackendConnector.findEntry(protectedUser.entryId).futureValue
        result shouldBe Some(protectedUser)
      }
    }

    "Return None for findEntry when api returns 404" in new Setup {
      forAll(protectedUserRecordGen) { protectedUser =>
        protectedUser.body.taxId.name.toString
        stubFor(
          get(urlEqualTo(s"$backendBaseUrl/entry-id/${protectedUser.entryId}"))
            .willReturn(aResponse().withStatus(NOT_FOUND))
        )

        val result = siProtectedUserAdminBackendConnector.findEntry(protectedUser.entryId).futureValue
        result shouldBe None
      }
    }

    "Return NO_CONTENT http response for deleteEntry when successful" in new Setup {
      forAll(protectedUserRecordGen) { protectedUser =>
        protectedUser.body.taxId.name.toString
        stubFor(
          delete(urlEqualTo(s"$backendBaseUrl/entry-id/${protectedUser.entryId}"))
            .willReturn(noContent())
        )

        val result = siProtectedUserAdminBackendConnector.deleteEntry(protectedUser.entryId).futureValue.value
        result.status shouldBe NO_CONTENT
      }
    }

    "Return UpstreamErrorResponse with 404 response when deleteEntry returns 404" in new Setup {
      forAll(protectedUserRecordGen) { protectedUser =>
        protectedUser.body.taxId.name.toString
        stubFor(
          delete(urlEqualTo(s"$backendBaseUrl/entry-id/${protectedUser.entryId}"))
            .willReturn(notFound())
        )

        val result = siProtectedUserAdminBackendConnector.deleteEntry(protectedUser.entryId).futureValue.left.value
        result.statusCode shouldBe NOT_FOUND
      }
    }

  }
}
