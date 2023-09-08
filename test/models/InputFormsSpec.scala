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

import models.InputForms.{entryForm, groupMaxLength}
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.wordspec.AnyWordSpec
import play.api.data.FormError
import util.Generators
class InputFormsSpec extends AnyWordSpec with Matchers with Generators with TableDrivenPropertyChecks {
  val allRequestFieldsPresent = Map(
    "action"             -> InputForms.addEntryActionBlock,
    "nino"               -> ninoGen.sample.get.nino,
    "sautr"              -> sautrGen.sample.get.utr,
    "identityProvider"   -> nonEmptyStringGen.sample.get,
    "identityProviderId" -> nonEmptyStringGen.sample.get,
    "group"              -> nonEmptyStringOfGen(groupMaxLength).sample.get,
    "addedByTeam"        -> nonEmptyStringGen.sample.get
  )

  val missingNinoAndSautr = allRequestFieldsPresent.updated("sautr", "").updated("nino", "")
  val actionLockNoCredId = allRequestFieldsPresent.updated("action", InputForms.addEntryActionLock).updated("identityProviderId", "")
  val groupIsLongerThanAllowed = allRequestFieldsPresent.updated("group", nonEmptyStringOfGen(groupMaxLength + 1).sample.get)
  val missingAddedByTeam = allRequestFieldsPresent.updated("addedByTeam", "")

  val table = Table(
    ("Scenario", "Request fields", "Expected errors"),
    ("Nino regex fail when present and incorrect", allRequestFieldsPresent.updated("nino", "bad_nino"), Seq(FormError("nino", "form.nino.regex"))),
    ("No Nino regex failure when not entered", allRequestFieldsPresent.updated("nino", ""), Seq()),
    ("sautr regex fails when present and incorrect", allRequestFieldsPresent.updated("sautr", "bad_sautr"), Seq(FormError("sautr", "form.sautr.regex"))),
    ("No sautr regex failure when not entered", allRequestFieldsPresent.updated("sautr", ""), Seq()),
    ("Group is longer than allowed", groupIsLongerThanAllowed, Seq(FormError("group", "error.maxLength", Seq(groupMaxLength)))),
    ("Nino or sautr is required when neither are present", missingNinoAndSautr, Seq(FormError("", "form.nino.sautr.required"))),
    ("AddedByTeam is missing", missingAddedByTeam, Seq(FormError("addedByTeam", "form.addedByTeam.required"))),
    ("identityProviderId must be present when action is LOCK", actionLockNoCredId, Seq(FormError("identityProviderId", "form.identityProviderId.required")))
  )

  "EntryForm" should {
    "handle validation scenarios for table" in {
      forAll(table) { (_, request, expectedErrors) =>
        val form = InputForms.entryForm
        val result = form.bind(request)
        result.errors should contain theSameElementsAs expectedErrors
      }
    }

    "Identity provider should be None when request field is empty string" in {
      val noIdpFields = allRequestFieldsPresent.updated("identityProvider", "").updated("identityProviderId", "")
      val result = entryForm.bind(noIdpFields).get
      result.identityProvider   shouldBe None
      result.identityProviderId shouldBe None
    }
  }
}
