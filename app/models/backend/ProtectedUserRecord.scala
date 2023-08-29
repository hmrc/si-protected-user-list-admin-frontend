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

package models.backend

import play.api.libs.json._

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDateTime, ZoneId}

case class ProtectedUserRecord(
  entryId: String,
  created: Modified,
  updated: Option[Modified],
  body:    ProtectedUser
) {
  def formattedFirstCreated(): String = formatted(created.at)

  def formattedLastUpdated(): Option[String] = updated.map(u => formatted(u.at))

  private def formatted(instant: Instant): String =
    LocalDateTime.ofInstant(instant, ZoneId of "UTC").format(DateTimeFormatter ofPattern "dd/MM/yyyy")

  val action: String = body.identityProviderId.map(_ => "LOCK").getOrElse("BLOCK")
  val nino: Option[String] = body.taxId.name match {
    case TaxIdentifierType.NINO => Some(body.taxId.value)
    case _                      => None
  }
  val sautr: Option[String] = body.taxId.name match {
    case TaxIdentifierType.SAUTR => Some(body.taxId.value)
    case _                       => None
  }
}

object ProtectedUserRecord {
  implicit val rds: Reads[ProtectedUserRecord] = Json.reads
}
