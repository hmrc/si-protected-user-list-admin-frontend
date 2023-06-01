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

import play.api.libs.json._

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDateTime, ZoneId}

case class ProtectedUserRecord(
  entryId: String,
  firstCreated: Long,
  lastUpdated: Option[Long],
  body: ProtectedUser
) {
  def formattedFirstCreated(): String = {
    LocalDateTime.ofInstant(Instant.ofEpochMilli(firstCreated), ZoneId.of("UTC")).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
  }

  def formattedLastUpdated(): Option[String] = {
    lastUpdated.map(lu => LocalDateTime.ofInstant(Instant.ofEpochMilli(lu), ZoneId.of("UTC")).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
  }
}

object ProtectedUserRecord {
  implicit val format: OFormat[ProtectedUserRecord] = Json.format
}
