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

import models.backend.{Modified, ProtectedUser, ProtectedUserRecord}
import play.api.libs.json.{Json, OWrites}

trait JsonWriters {
  implicit val writesProtectedUserRecord: OWrites[ProtectedUserRecord] = record =>
    Json.obj(
      "entryId" -> record.entryId,
      "created" -> record.created,
      "updated" -> record.updated,
      "body"    -> record.body
    )

  implicit val writesModified: OWrites[Modified] = mod => Json.obj("by" -> mod.by, "at" -> mod.at)

  implicit val writesProtectedUser: OWrites[ProtectedUser] = pu =>
    Json.obj(
      "tax_id" -> pu.taxId,
      "idp_id" -> pu.identityProviderId,
      "group"  -> pu.group,
      "team"   -> pu.team
    )
}
