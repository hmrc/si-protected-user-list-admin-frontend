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

import play.api.data.Form
import play.api.data.Forms.{mapping, nonEmptyText, text}

package object models {
  private val nameRegex = "^[A-Za-z0-9]{12}$"
  private val orgNameRegex = """^.{2,300}$"""
  private val emailRegex = """^.{0,62}@.{1,64}\..{1,64}$"""

  val addAllowListForm: Form[User] = Form(
    mapping(
      "name" -> nonEmptyText
        .transform(s => s.replaceAll(" ", ""), identity[String])
        .verifying("form.username.regex", _.matches(nameRegex)),
      "org" -> nonEmptyText
        .transform(_.trim, identity[String])
        .verifying("form.org.regex", _.matches(orgNameRegex)),
      "requester_email" -> nonEmptyText.verifying(
        "form.requester_email.regex",
        _.matches(emailRegex)
      )
    )(User.apply)(User.unapply)
  )

  val searchAllowListForm: Form[Search] = Form(
    mapping(
      "name" -> text
        .transform(s => s.replaceAll(" ", ""), identity[String])
        .verifying("form.username.regex", _.matches("([A-Za-z0-9 ]{12})"))
    )(Search.apply)(Search.unapply)
  )
}
