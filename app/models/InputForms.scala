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

import com.google.inject.Inject
import config.SiProtectedUserConfig
import models.InputForms.{disallowedCharacters, searchQueryMaxLength, searchRegex}
import models.utils.StopOnFirstFail
import models.utils.StopOnFirstFail.constraint
import play.api.data.Form
import play.api.data.Forms._

class InputForms @Inject() (config: SiProtectedUserConfig) {
  def searchForm: Form[Search] = Form(
    mapping(
      "filterByTeam" -> optional(text.verifying("form.filterByTeam.invalid", ("All" +: config.addedByTeams).contains(_))),
      "searchQuery" -> optional(
        text
          .verifying(
            StopOnFirstFail(
              constraint[String]("form.searchQuery.maxLength", _.sizeIs <= searchQueryMaxLength),
              constraint[String]("form.searchQuery.regex", entryText => entryText.matches(searchRegex) && !entryText.exists(disallowedCharacters.contains(_)))
            )
          )
      ).verifying("form.searchQuery.minLength", value => value.isDefined && !value.get.isBlank)
    )(Search.apply)(Search.unapply)
  )

}

object InputForms {
  val searchQueryMaxLength = 64
  val searchRegex = """^[\x20-\x7E]*$"""
  val disallowedCharacters = Seq('.', '+', '*', '?', '^', '$', '(', ')', '[', ']', '{', '}', '|', '\\')
}
