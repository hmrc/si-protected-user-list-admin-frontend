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

import config.SiProtectedUserConfig
import connectors.SiProtectedUserListAdminConnector
import models.Upload
import org.mockito.captor.{ArgCaptor, Captor}
import uk.gov.hmrc.gg.test.UnitSpec
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent
import util.Generators

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DataProcessServiceSpec extends UnitSpec with Generators {

  trait Setup {
    val siProtectedUserConfig = siProtectedUserConfigGen.sample.get
    val mockAdminConnector = mock[SiProtectedUserListAdminConnector]
    val mockAuditConnector = mock[AuditConnector]

    def dataProcessService(siProtectedUserConfig: SiProtectedUserConfig = siProtectedUserConfig) =
      new DataProcessService(mockAdminConnector, siProtectedUserConfig, mockAuditConnector)
  }

  "processBulkData" should {
    "give error if there format error in the csv file" in new Setup {
      val list = Seq(Some(Upload("123456789012", "someOrg", "some@email.com")), None, Some(Upload("123456789012", "someOrg", "some@email.com")))
      val result = dataProcessService().processBulkData(allowListData = list, "", None)(HeaderCarrier())

      verifyZeroInteractions(mockAdminConnector)
      assert(result.toOption.get.contains("CSV line number 3, invalid format. Please check data and format"))
    }

    "give error if the file is too large" in new Setup {
      val list: Seq[Some[Upload]] = Seq(
        Some(Upload("123456789012", "someOrg", "some@email.com")),
        Some(Upload("123456789012", "someOrg", "some@email.com")),
        Some(Upload("123456789012", "someOrg", "some@email.com"))
      )

      val service = dataProcessService(siProtectedUserConfig.copy(bulkUploadRowLimit = 1))
      val result = service.processBulkData(list, "", None)(HeaderCarrier())
      verifyZeroInteractions(mockAdminConnector)
      assert(result.toOption.get.contains("CSV file line limit 1 exceeded. File ignored"))
    }

    "process the data correctly and show the correct page and return 200 status" in new Setup {
      val list: Seq[Some[Upload]] = Seq(
        Some(Upload("123456789012", "someOrg", "some@email.com")),
        Some(Upload("123456789012", "someOrg", "some@email.com")),
        Some(Upload("123456789012", "someOrg", "some@email.com"))
      )

      when(mockAdminConnector.updateEntryList(any)(any[HeaderCarrier])).thenReturn(Future.successful(()))

      val service = dataProcessService(siProtectedUserConfig.copy(bulkUploadRowLimit = 3, bulkUploadBatchSize = 100, bulkUploadBatchDelaySecs = 0))
      val result = service.processBulkData(list, "", Some("someUserId"))(HeaderCarrier())

      Thread.sleep(3000)

      val captorDataEvent: Captor[DataEvent] = ArgCaptor[DataEvent]
      verify(mockAuditConnector).sendEvent(captorDataEvent.capture)(any, any)

      captorDataEvent.value.auditType               shouldBe "ListUpdate"
      captorDataEvent.value.detail("strideUserPid") shouldBe "someUserId"
      captorDataEvent.value.detail("rowsInFile")    shouldBe "3"

      verify(mockAdminConnector, times(3)).updateEntryList(any)(any)
      assert(result.isLeft)
    }
  }
}
