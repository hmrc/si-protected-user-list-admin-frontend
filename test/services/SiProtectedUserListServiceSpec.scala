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
import org.scalatest.EitherValues
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.http.Status
import uk.gov.hmrc.gg.test.UnitSpec
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import util.Generators

import scala.concurrent.Future

class SiProtectedUserListServiceSpec extends UnitSpec with Generators with ScalaCheckDrivenPropertyChecks with EitherValues {
  import SiProtectedUserListServiceSpec._

  "SiProtectedUserListService" should {
    "Call add on the connector when adding" in {
      forAll(entryGen, protectedUserRecords) { (entry, record) =>
        val expectedProtectedUser = entry.toProtectedUser()
        when(mockBackendConnector.addEntry(expectedProtectedUser)).thenReturn(Future.successful(record))

        val result = await(siProtectedUserListService.addEntry(entry))

        result shouldBe record
        verify(mockBackendConnector).addEntry(expectedProtectedUser)
      }
    }

    "Call update on the connector when updating" in
      forAll(entryGen, protectedUserRecords) { (entry, record) =>
        val expectedProtectedUser = entry.toProtectedUser()
        when(mockBackendConnector.updateEntry(entry.entryId.value, expectedProtectedUser)).thenReturn(Future.successful(record))

        val result = await(siProtectedUserListService.updateEntry(entry))

        result shouldBe record
        verify(mockBackendConnector).updateEntry(entry.entryId.value, expectedProtectedUser)
      }

    "Fails when no entryId present for updateEntry" in
      forAll(entryGen) { entry =>
        val result = await(siProtectedUserListService.updateEntry(entry.copy(entryId = None)).failed)

        result shouldBe a[IllegalArgumentException]
      }

    "Call findEntry on the connector when finding" in
      forAll(protectedUserRecords) { record =>
        when(mockBackendConnector.findEntry(record.entryId)).thenReturn(Future.successful(Some(record)))

        val result = await(siProtectedUserListService.findEntry(record.entryId)).value
        result shouldBe record
      }

    "Call deleteEntry on the connector when deleting" in
      forAll(protectedUserRecords) { record =>
        val expectedResponse = HttpResponse(Status.NO_CONTENT, "")
        when(mockBackendConnector.deleteEntry(record.entryId)).thenReturn(Future.successful(Right(expectedResponse)))

        val result = await(siProtectedUserListService.deleteEntry(record.entryId)).value
        result shouldBe expectedResponse
      }
  }
}
object SiProtectedUserListServiceSpec extends MockitoSugar {
  private val mockBackendConnector = mock[SiProtectedUserAdminBackendConnector]
  private val siProtectedUserListService = new SiProtectedUserListService(mockBackendConnector)

  implicit private val headerCarrier: HeaderCarrier = HeaderCarrier()
}
