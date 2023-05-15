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

import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json.{OFormat, __}

case class ProtectedUser(
  taxId: TaxIdentifier,
  identityProviderId: Option[IdentityProviderId],
  addedByUser: Option[String],
  addedByTeam: Option[String],
  updatedByUser: Option[String],
  updatedByTeam: Option[String],
  group: String = ""
)

object ProtectedUser {
  implicit val format: OFormat[ProtectedUser] = (
    (__ \ "taxId").format[TaxIdentifier] and
      (__ \ "identityProviderId").formatNullable[IdentityProviderId] and
      (__ \ "addedByUser").formatNullable[String] and
      (__ \ "addedByTeam").formatNullable[String] and
      (__ \ "updatedByUser").formatNullable[String] and
      (__ \ "updatedByTeam").formatNullable[String] and
      (__ \ "group").formatWithDefault("")
  )(apply, unlift(unapply))
}
