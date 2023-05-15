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

import zamblauskas.csv.parser._
import zamblauskas.functional._
@Deprecated
case class Upload(username: String, organisationName: String, requesterEmail: String)

object Upload {
  private val nameRegex = "^[A-Za-z0-9]{12}$"
  private val orgNameRegex = """^.{2,300}$"""
  private val emailRegex = """^.{0,62}@.{1,64}\..{1,64}$"""

  def regexApply(username: String, organisationName: String, requesterEmail: String): Option[Upload] = {
    val transformedName: String = username.replaceAll(" ", "")
    val transformedOrg: String = organisationName.trim

    if (transformedName.matches(nameRegex) && transformedOrg.matches(orgNameRegex) && requesterEmail.matches(emailRegex)) {
      Some(Upload(transformedName, transformedOrg, requesterEmail))
    } else {
      None
    }
  }

  implicit val reads: ColumnReads[Option[Upload]] = (
    column("UserID").as[String] and
      column("OrganisationName").as[String] and
      column("RequesterEmail").as[String]
  )(regexApply)
}
