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
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.libs.json.Json
import uk.gov.hmrc.http.{ConflictException, HeaderCarrier}
import util.Generators

class SiProtectedUserAdminBackendConnectorISpec extends BaseISpec with Generators with ScalaCheckDrivenPropertyChecks with ScalaFutures {
  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(5, Millis))

  trait Setup {
    implicit val headerCarrier = HeaderCarrier()
    val siProtectedUserAdminBackendConnector = inject[SiProtectedUserAdminBackendConnector]

  }

  "SiProtectedUserAdminBackendConnector" should {
    "return a ProtectedUserRecord when add is successful" in new Setup {
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

    "Fail with conflict exception when 409 conflict is returned from the backend api" in new Setup {
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
  }
}