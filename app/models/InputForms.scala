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

package models

import play.api.data.Form
import play.api.data.Forms._
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfEqual

object InputForms {
  val ninoRegex = "((?!(BG|GB|KN|NK|NT|TN|ZZ)|(D|F|I|Q|U|V)[A-Z]|[A-Z](D|F|I|O|Q|U|V))[A-Z]{2})[0-9]{6}[A-D]"
  val saUtrRegex = "[0-9]{10}"
  val addEntryActionBlock = "BLOCK"
  val addEntryActionLock = "LOCK"
  val addEntryActions = Seq(addEntryActionBlock, addEntryActionLock)
  val groupMaxLength = 12
  val entryForm: Form[Entry] = Form(
    mapping(
      "entryId"            -> optional(nonEmptyText),
      "addedByUser"        -> ignored(Option.empty[String]),
      "updatedByUser"      -> ignored(Option.empty[String]),
      "action"             -> nonEmptyText,
      "nino"               -> optional(nonEmptyText.verifying("form.nino.regex", _.matches(ninoRegex))),
      "sautr"              -> optional(nonEmptyText.verifying("form.sautr.regex", _.matches(saUtrRegex))),
      "identityProvider"   -> mandatoryIfEqual("action", addEntryActionLock, nonEmptyText),
      "identityProviderId" -> mandatoryIfEqual("action", addEntryActionLock, text.verifying("form.identityProviderId.required", !_.trim.isEmpty)),
      "group"              -> optional(nonEmptyText(maxLength = groupMaxLength)),
      "addedByTeam"        -> optional(nonEmptyText),
      "updatedByTeam"      -> optional(nonEmptyText)
    )(Entry.apply)(Entry.unapply)
      .verifying("form.nino.sautr.required", entry => entry.sautr.isDefined || entry.nino.isDefined)
  )

}
