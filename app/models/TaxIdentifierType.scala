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

sealed trait TaxIdentifierType

object TaxIdentifierType {
  case object NINO extends TaxIdentifierType
  case object SAUTR extends TaxIdentifierType

  val values: Seq[TaxIdentifierType] = Seq(NINO, SAUTR)

  private def parseErr(str: String) = s"Could not read tax ID type from $str."

  def from(string: String): Either[String, TaxIdentifierType] =
    values
      .find(_.toString.toLowerCase == string)
      .toRight(parseErr(string))

  private val reads: Reads[TaxIdentifierType] =
    _.validate[String].flatMap { str =>
      values.find(_.toString equalsIgnoreCase str) match {
        case Some(taxIdType) => JsSuccess(taxIdType)
        case None            => JsError(parseErr(str))
      }
    }

  private def writes(taxIdType: TaxIdentifierType) = JsString(taxIdType.toString)

  implicit val format: Format[TaxIdentifierType] = Format(reads, writes _)
}
