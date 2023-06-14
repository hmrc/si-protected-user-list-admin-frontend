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

package services

import connectors.SiProtectedUserAdminBackendConnector
import models.ProtectedUserRecord
import org.mockito.scalatest.MockitoSugar
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{EitherValues, OptionValues}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.http.Status
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import util.Generators

import scala.concurrent.Future
class SiProtectedUserListServiceSpec
    extends AnyWordSpec
    with Matchers
    with Generators
    with ScalaCheckDrivenPropertyChecks
    with ScalaFutures
    with IntegrationPatience
    with EitherValues
    with OptionValues {
  import SiProtectedUserListServiceSpec._
  import org.scalacheck.Arbitrary.arbitrary

  "SiProtectedUserListService" should {
    "Call add on the connector when adding" in {
      forAll(entryGen, arbitrary) { (entry, protectedUserRecord) =>
        val expectedProtectedUser = entry.toProtectedUser()
        when(mockBackendConnector.addEntry(expectedProtectedUser)).thenReturn(Future.successful(protectedUserRecord))

        val result = siProtectedUserListService.addEntry(entry).futureValue

        result shouldBe protectedUserRecord
        verify(mockBackendConnector).addEntry(expectedProtectedUser)
      }
    }

    "Call findEntry on the connector when finding" in {
      forAll { protectedUserRecord: ProtectedUserRecord =>
        when(mockBackendConnector.findEntry(protectedUserRecord.entryId)).thenReturn(Future.successful(Some(protectedUserRecord)))

        val result = siProtectedUserListService.findEntry(protectedUserRecord.entryId).futureValue.value
        result shouldBe protectedUserRecord
      }
    }

    "Call deleteEntry on the connector when deleting" in {
      forAll { protectedUserRecord: ProtectedUserRecord =>
        val expectedResponse = HttpResponse(Status.NO_CONTENT, "")
        when(mockBackendConnector.deleteEntry(protectedUserRecord.entryId)).thenReturn(Future.successful(Right(expectedResponse)))

        val result = siProtectedUserListService.deleteEntry(protectedUserRecord.entryId).futureValue.value
        result shouldBe expectedResponse
      }
    }
  }
}
object SiProtectedUserListServiceSpec extends MockitoSugar {
  private val mockBackendConnector = mock[SiProtectedUserAdminBackendConnector]
  private val siProtectedUserListService = new SiProtectedUserListService(mockBackendConnector)

  implicit private val headerCarrier: HeaderCarrier = HeaderCarrier()
}
