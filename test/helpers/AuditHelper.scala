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

package helpers

import models.User
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.gg.test.UnitSpec

trait AuditHelper {
  self: UnitSpec =>

  def verifyDataEvent(event: DataEvent, userID: String, details: User, success: Boolean, delete: Boolean) = {
    val deleted: String = if (delete) "delete" else "add"
    val transactionName = if (delete) "HMRC - Login - Restricted User List - delete entry" else "HMRC - Login - Restricted User List - add entry"

    event.auditType   shouldEqual "ListUpdate"
    event.auditSource shouldEqual "si-protected-user-list-admin-frontend"
    event.detail           should contain("strideUserPid" -> userID)
    event.detail           should contain("operation" -> deleted.toString)
    event.detail           should contain("success" -> success.toString)
    event.detail           should contain("orgLoginId" -> details.username)
    event.detail           should contain("orgName" -> details.organisationName)
    event.detail           should contain("requesterEmail" -> details.requesterEmail)
    event.tags             should contain("transactionName" -> transactionName)
    event.tags             should contain("deviceID" -> "someDeviceId")
  }
}
