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
import models.backend.{IdentityProviderId, TaxIdentifier, TaxIdentifierType}
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.{JsValue, Json}
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfEqual

final case class Insert(
  optNINO:  Option[String],
  optSAUTR: Option[String],
  optIdpID: Option[IdentityProviderId],
  group:    String,
  team:     String
) {
  import TaxIdentifierType._

  def toRequestJSON(implicit request: StrideRequest[_]): JsValue = {
    val taxID = optNINO.map(TaxIdentifier(NINO, _)) orElse optSAUTR.map(TaxIdentifier(SAUTR, _))

    Json.obj(
      "stride_pid" -> request.getUserPid,
      "protectedUser" -> Json.obj(
        "tax_id" -> taxID,
        "idp_id" -> optIdpID,
        "group"  -> group,
        "team"   -> team
      )
    )
  }
}
object Insert {
  private val ninoRegex = "((?!(BG|GB|KN|NK|NT|TN|ZZ)|(D|F|I|Q|U|V)[A-Z]|[A-Z](D|F|I|O|Q|U|V))[A-Z]{2})[0-9]{6}[A-D]"
  private val saUtrRegex = "[0-9]{10}"

  val form: Form[Insert] = Form(
    mapping(
      "action"             -> nonEmptyText,
      "nino"               -> optional(nonEmptyText.verifying("form.nino.regex", _ matches ninoRegex)),
      "sautr"              -> optional(nonEmptyText.verifying("form.sautr.regex", _ matches saUtrRegex)),
      "identityProvider"   -> mandatoryIfEqual("action", addEntryActionLock, nonEmptyText),
      "identityProviderId" -> mandatoryIfEqual("action", addEntryActionLock, text.verifying("form.identityProviderId.required", !_.isBlank)),
      "group"              -> text(maxLength = groupMaxLength),
      "team"               -> nonEmptyText
    ) { (_, optNINO, optSAUTR, optIdpName, optIdpValue, group, team) =>
      apply(
        optNINO,
        optSAUTR,
        for {
          idpName  <- optIdpName
          idpValue <- optIdpValue
        } yield IdentityProviderId(idpName, idpValue),
        group,
        team
      )
    } { insert =>
      Some(
        (
          insert.optIdpID.fold(addEntryActionBlock)(_ => addEntryActionLock),
          insert.optNINO,
          insert.optSAUTR,
          insert.optIdpID.map(_.name),
          insert.optIdpID.map(_.value),
          insert.group,
          insert.team
        )
      )
    }
      .verifying("form.nino.sautr.required", insert => (insert.optNINO orElse insert.optSAUTR).isDefined)
  )
}
