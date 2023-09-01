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

package models.forms

import controllers.base.StrideRequest
import models.backend.IdentityProviderId
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.{Json, Writes}
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfEqual

final case class Update(
  optIdpID: Option[IdentityProviderId],
  group:    String,
  team:     String
)
object Update {
  val form: Form[Update] = Form(
    mapping(
      "action"             -> nonEmptyText,
      "identityProvider"   -> mandatoryIfEqual("action", addEntryActionLock, nonEmptyText),
      "identityProviderId" -> mandatoryIfEqual("action", addEntryActionLock, text.verifying("form.identityProviderId.required", !_.isBlank)),
      "group"              -> text(maxLength = groupMaxLength),
      "team"               -> nonEmptyText
    ) { (_, optIdpName, optIdpValue, group, team) =>
      apply(
        for {
          idpName  <- optIdpName
          idpValue <- optIdpValue
        } yield IdentityProviderId(idpName, idpValue),
        group,
        team
      )
    } { update =>
      Some(
        (
          update.optIdpID.fold(addEntryActionBlock)(_ => addEntryActionLock),
          update.optIdpID.map(_.name),
          update.optIdpID.map(_.value),
          update.group,
          update.team
        )
      )
    }
  )

  implicit def wrt(implicit request: StrideRequest[_]): Writes[Update] = update =>
    Json.obj(
      "stride_pid" -> request.userPID,
      "idp_id"     -> update.optIdpID,
      "group"      -> update.group,
      "team"       -> update.team
    )
}
