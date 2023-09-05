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

import controllers.base.StrideRequest
import play.api.libs.json.{Json, OFormat}

case class Entry(entryId: Option[String],
                 addedByUser: Option[String],
                 updatedByUser: Option[String],
                 action: String,
                 nino: Option[String],
                 sautr: Option[String],
                 identityProvider: Option[String],
                 identityProviderId: Option[String],
                 group: Option[String],
                 addedByTeam: Option[String],
                 updatedByTeam: Option[String]
                ) {
  def toProtectedUser(isUpdate: Boolean)(implicit request: StrideRequest[_]): ProtectedUser = {
    ProtectedUser(
      taxId = (nino, sautr) match {
        case (Some(nino), _)     => TaxIdentifier(TaxIdentifierType.NINO, nino)
        case (None, Some(sautr)) => TaxIdentifier(TaxIdentifierType.SAUTR, sautr)
        case _                   => throw new IllegalStateException(s"at least one of ${TaxIdentifierType.values} is required ")
      },
      identityProviderId = for {
        provider <- identityProvider
        creds    <- identityProviderId
      } yield IdentityProviderId(provider, creds),
      addedByUser = if (isUpdate) None else Some(request.getUserPid),
      addedByTeam = addedByTeam,
      updatedByUser = if (isUpdate) Some(request.getUserPid) else None,
      updatedByTeam = updatedByTeam,
      group = group.getOrElse("")
    )
  }
}
object Entry {
  implicit val formats: OFormat[Entry] = Json.format[Entry]

  def from(protectedUserRecord: ProtectedUserRecord): Entry = {
    Entry(
      entryId = Some(protectedUserRecord.entryId),
      addedByUser = protectedUserRecord.body.addedByUser,
      updatedByUser = protectedUserRecord.body.updatedByUser,
      action = protectedUserRecord.action,
      nino = protectedUserRecord.nino,
      sautr = protectedUserRecord.sautr,
      identityProvider = protectedUserRecord.body.identityProviderId.map(_.name),
      identityProviderId = protectedUserRecord.body.identityProviderId.map(_.value),
      group = Some(protectedUserRecord.body.group),
      addedByTeam = protectedUserRecord.body.addedByTeam,
      updatedByTeam = protectedUserRecord.body.updatedByTeam
    )
  }
}
