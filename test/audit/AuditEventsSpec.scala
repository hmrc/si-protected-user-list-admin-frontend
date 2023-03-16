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

package audit

import helpers.AuditHelper
import models.User
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.gg.test.UnitSpec

class AuditEventsSpec extends UnitSpec with AuditHelper {
  implicit val hc: HeaderCarrier = HeaderCarrier().copy(deviceID = Some("someDeviceId"))
  implicit val request: FakeRequest[_] = FakeRequest("GET", "583242842")

  val user: User = User("someUserID", "someOrganisationName", "some@email.com")
  val userId = "123456789012"

  "HelpdeskDeregistration Event" when {
    "deregistrationSuccessful" should {
      "allowListAddEventSuccess should contain the correct details" in {
        val dataEvent = AuditEvents.allowListAddEventSuccess(userId, user)
        verifyDataEvent(dataEvent, userId, user, success = true, delete = false)
      }

      "allowListAddEventFailure should contain the correct details" in {
        val dataEvent = AuditEvents.allowListAddEventFailure(userId, user)
        verifyDataEvent(dataEvent, userId, user, success = false, delete = false)
      }

      "allowListDeleteEventSuccess should contain the correct details" in {
        val dataEvent = AuditEvents.allowListDeleteEventSuccess(userId, user)
        verifyDataEvent(dataEvent, userId, user, success = true, delete = true)
      }

      "allowListDeleteEventFailure should contain the correct details" in {
        val dataEvent = AuditEvents.allowListDeleteEventFailure(userId, user)
        verifyDataEvent(dataEvent, userId, user, success = false, delete = true)
      }
    }
  }
}
