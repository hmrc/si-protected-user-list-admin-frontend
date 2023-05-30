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
                )
object Entry {
  implicit val formats: OFormat[Entry] = Json.format[Entry]

  implicit class EntryConversionOps(entry: Entry) {
    def toProtectedUser(): ProtectedUser = {
      ProtectedUser(
        taxId = (entry.nino, entry.sautr) match {
          case (Some(nino), _)     => TaxIdentifier(TaxIdentifierType.NINO, nino)
          case (None, Some(sautr)) => TaxIdentifier(TaxIdentifierType.SAUTR, sautr)
          case _                   => throw new IllegalStateException(s"at least one of ${TaxIdentifierType.values} is required ")
        },
        identityProviderId = for {
          provider <- entry.identityProvider
          creds    <- entry.identityProviderId
        } yield IdentityProviderId(provider, creds),
        addedByUser = entry.addedByUser,
        addedByTeam = entry.addedByTeam,
        updatedByUser = entry.updatedByUser,
        updatedByTeam = entry.updatedByTeam,
        group = entry.group.getOrElse("")
      )
    }
  }
}
