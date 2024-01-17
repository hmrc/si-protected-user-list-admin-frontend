/*
 * Copyright 2024 HM Revenue & Customs
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

package models.request

import controllers.base.StrideRequest
import models.TaxIdentifierType.NINO
import models.{IdentityProviderId, ProtectedUser, TaxIdentifier}
import play.api.data.Form
import play.api.data.Forms.{mapping, nonEmptyText, optional, text}
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfEqual

case class Update(
  optIdpId: Option[IdentityProviderId],
  group: String,
  team: String
) {
  def toProtectedUser(implicit req: StrideRequest[_]): ProtectedUser = ProtectedUser(
    taxId = TaxIdentifier(NINO, ""), // This is just a placeholder. Consider refining backend API to not take unused fields.
    identityProviderId = optIdpId,
    addedByUser = None, // This is just a placeholder. Consider refining backend API to not take unused fields.
    addedByTeam = None, // This is just a placeholder. Consider refining backend API to not take unused fields.
    updatedByUser = req.userPidOpt,
    updatedByTeam = Some(team),
    group = group
  )
}
object Update {
  def from(protectedUser: ProtectedUser): Update = apply(
    protectedUser.identityProviderId,
    protectedUser.group,
    protectedUser.updatedByTeam getOrElse ""
  )

  val form: Form[Update] = Form(
    mapping(
      "action"             -> nonEmptyText,
      "identityProvider"   -> mandatoryIfEqual("action", addEntryActionLock, nonEmptyText),
      "identityProviderId" -> mandatoryIfEqual("action", addEntryActionLock, text.verifying("form.identityProviderId.required", !_.isBlank)),
      "group"              -> optional(nonEmptyText(maxLength = groupMaxLength)),
      "team"               -> text.verifying("form.team.required", !_.isBlank)
    )((_, optIdp, optIdpId, optGroup, team) =>
      apply(
        for {
          idp   <- optIdp
          idpId <- optIdpId
        } yield IdentityProviderId(idp, idpId),
        optGroup getOrElse "",
        team
      )
    )(insert =>
      Some(
        (
          if (insert.optIdpId.isDefined) addEntryActionLock else addEntryActionBlock,
          insert.optIdpId.map(_.name),
          insert.optIdpId.map(_.value),
          Some(insert.group),
          insert.team
        )
      )
    )
  )
}
