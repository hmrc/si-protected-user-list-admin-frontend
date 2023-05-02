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
import play.api.data.Forms.{mapping, nonEmptyText, optional, text}
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfEqual

object InputForms {
  private val nameRegex = "^[A-Za-z0-9]{12}$"
  private val orgNameRegex = """^.{2,300}$"""
  val emailRegex = """^.{0,62}@.{1,64}\..{1,64}$"""
  val ninoRegex = "((?!(BG|GB|KN|NK|NT|TN|ZZ)|(D|F|I|Q|U|V)[A-Z]|[A-Z](D|F|I|O|Q|U|V))[A-Z]{2})[0-9]{6}[A-D]"
  private val saUtrRegex = "[0-9]{10}"
  val addEntryActionBlock = "BLOCK"
  val addEntryActionLock = "LOCK"
  val addEntryActions = Seq(addEntryActionBlock, addEntryActionLock)
  val entryForm: Form[Entry] = Form(
    mapping(
      "action"             -> nonEmptyText,
      "nino"               -> optional(nonEmptyText.verifying("form.nino.regex", _.matches(ninoRegex))),
      "sautr"              -> optional(nonEmptyText.verifying("form.sautr.regex", _.matches(saUtrRegex))),
      "identityProviderId" -> mandatoryIfEqual("action", addEntryActionLock, nonEmptyText),
      "group"              -> optional(nonEmptyText),
      "identityProvider"   -> mandatoryIfEqual("action", addEntryActionLock, nonEmptyText),
      "addedByTeam"        -> nonEmptyText
    )(Entry.apply)(Entry.unapply)
      .verifying("form.nino.sautr.required", entry => entry.sautr.isDefined || entry.nino.isDefined)
  )

  val userForm: Form[User] = Form(
    mapping(
      "name" -> nonEmptyText
        .transform(s => s.replaceAll(" ", ""), identity[String])
        .verifying("form.username.regex", _.matches(nameRegex)),
      "org" -> nonEmptyText
        .transform(_.trim, identity[String])
        .verifying("form.org.regex", _.matches(orgNameRegex)),
      "requester_email" -> nonEmptyText.verifying(
        "form.requester_email.regex",
        _.matches(emailRegex)
      )
    )(User.apply)(User.unapply)
  )

  val searchAllowListForm: Form[Search] = Form(
    mapping(
      "name" -> text
        .transform(s => s.replaceAll(" ", ""), identity[String])
        .verifying("form.username.regex", _.matches("([A-Za-z0-9 ]{12})"))
    )(Search.apply)(Search.unapply)
  )
}
