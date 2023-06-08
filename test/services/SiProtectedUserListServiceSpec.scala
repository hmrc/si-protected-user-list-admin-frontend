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
import org.mockito.scalatest.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
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
    with MockitoSugar
    with EitherValues
    with OptionValues {
  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(5, Millis))

  trait Setup {
    val mockBackendConnector = mock[SiProtectedUserAdminBackendConnector]
    val siProtectedUserListService = new SiProtectedUserListService(mockBackendConnector)
    implicit val headerCarrier = HeaderCarrier()
  }

  "SiProtectedUserListService" should {
    "Call add on the connector when adding" in new Setup {
      forAll(entryGen, protectedUserRecordGen) { (entry, protectedUserRecord) =>
        val expectedProtectedUser = entry.toProtectedUser()
        when(mockBackendConnector.addEntry(expectedProtectedUser)).thenReturn(Future.successful(protectedUserRecord))

        val result = siProtectedUserListService.addEntry(entry).futureValue

        result shouldBe protectedUserRecord
        verify(mockBackendConnector).addEntry(expectedProtectedUser)
      }
    }

    "Call findEntry on the connector when finding" in new Setup {
      forAll(protectedUserRecordGen) { protectedUserRecord =>
        when(mockBackendConnector.findEntry(protectedUserRecord.entryId)).thenReturn(Future.successful(Some(protectedUserRecord)))

        val result = siProtectedUserListService.findEntry(protectedUserRecord.entryId).futureValue.value
        result shouldBe protectedUserRecord
      }
    }

    "Call deleteEntry on the connector when deleting" in new Setup {
      forAll(protectedUserRecordGen) { protectedUserRecord =>
        val expectedResponse = HttpResponse(Status.NO_CONTENT, "")
        when(mockBackendConnector.deleteEntry(protectedUserRecord.entryId)).thenReturn(Future.successful(Right(expectedResponse)))

        val result = siProtectedUserListService.deleteEntry(protectedUserRecord.entryId).futureValue.value
        result shouldBe expectedResponse
      }
    }
  }
}
