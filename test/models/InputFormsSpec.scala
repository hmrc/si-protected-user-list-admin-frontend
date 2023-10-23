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

import models.InputForms.{addEntryActionBlock, addEntryActionLock, groupMaxLength, searchQueryMaxLength}
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.data.FormError
import util.Generators

class InputFormsSpec extends AnyWordSpec with Matchers with Generators with TableDrivenPropertyChecks with GuiceOneAppPerSuite {

  val inputForm = app.injector.instanceOf[InputForms]

  val allRequestFieldsPresentEntryForm = Map(
    "action"             -> addEntryActionBlock,
    "nino"               -> ninoGen.sample.get.nino,
    "sautr"              -> sautrGen.sample.get.utr,
    "identityProvider"   -> nonEmptyStringGen.sample.get,
    "identityProviderId" -> nonEmptyStringGen.sample.get,
    "group"              -> nonEmptyStringOfGen(groupMaxLength).sample.get,
    "addedByTeam"        -> nonEmptyStringGen.sample.get
  )

  val allRequestFieldsPresentSearchQuery = Map(
    "filterByTeam" -> "All",
    "searchQuery"  -> nonEmptyPrintableStringGen.sample.get
  )

  val onlyFilterByTeamFieldPresentInSearchQuery = Map(
    "filterByTeam" -> "All"
  )

  val missingNinoAndSautr = allRequestFieldsPresentEntryForm.updated("sautr", "").updated("nino", "")
  val actionLockNoCredId = allRequestFieldsPresentEntryForm.updated("action", addEntryActionLock).updated("identityProviderId", "")
  val groupIsLongerThanAllowed = allRequestFieldsPresentEntryForm.updated("group", nonEmptyStringOfGen(groupMaxLength + 1).sample.get)
  val missingAddedByTeam = allRequestFieldsPresentEntryForm.updated("addedByTeam", "")

  val tableEntryForm = Table(
    ("Scenario", "Request fields", "Expected errors"),
    ("Nino regex fail when present and incorrect", allRequestFieldsPresentEntryForm.updated("nino", "bad_nino"), Seq(FormError("nino", "form.nino.regex"))),
    ("No Nino regex failure when not entered", allRequestFieldsPresentEntryForm.updated("nino", ""), Seq()),
    ("sautr regex fails when present and incorrect", allRequestFieldsPresentEntryForm.updated("sautr", "bad_sautr"), Seq(FormError("sautr", "form.sautr.regex"))),
    ("No sautr regex failure when not entered", allRequestFieldsPresentEntryForm.updated("sautr", ""), Seq()),
    ("Group is longer than allowed", groupIsLongerThanAllowed, Seq(FormError("group", "error.maxLength", Seq(groupMaxLength)))),
    ("Nino or sautr is required when neither are present", missingNinoAndSautr, Seq(FormError("", "form.nino.sautr.required"))),
    ("AddedByTeam is missing", missingAddedByTeam, Seq(FormError("addedByTeam", "form.addedByTeam.required"))),
    ("identityProviderId must be present when action is LOCK", actionLockNoCredId, Seq(FormError("identityProviderId", "form.identityProviderId.required")))
  )

  val tableSearchQueryForm = Table(
    ("Scenario", "Request fields", "Expected errors"),
    ("SearchQuery is valid no errors", allRequestFieldsPresentSearchQuery, Seq()),
    ("SearchQuery isn't specified", onlyFilterByTeamFieldPresentInSearchQuery, Seq(FormError("searchQuery", "form.searchQuery.minLength"))),
    ("Length is past 64",
     allRequestFieldsPresentSearchQuery.updated("searchQuery", nonEmptyStringOfGen(searchQueryMaxLength + 1).sample.get),
     Seq(FormError("searchQuery", "form.searchQuery.maxLength"))
    ),
    ("Character is not in the ascii range of 32 to 126",
     allRequestFieldsPresentSearchQuery.updated("searchQuery", nonEmptyNonPrintableStringGen.sample.get),
     Seq(FormError("searchQuery", "form.searchQuery.regex"))
    ),
    ("Character is in the list of disallowed characters",
     allRequestFieldsPresentSearchQuery.updated("searchQuery", disallowedCharStringGen.sample.get),
     Seq(FormError("searchQuery", "form.searchQuery.regex"))
    )
  )

  "EntryForm" should {
    "handle validation scenarios for table" in {
      forAll(tableEntryForm) { (_, request, expectedErrors) =>
        val form = inputForm.entryForm
        val result = form.bind(request)
        result.errors should contain theSameElementsAs expectedErrors
      }
    }

    "Identity provider should be None when request field is empty string" in {
      val noIdpFields = allRequestFieldsPresentEntryForm.updated("identityProvider", "").updated("identityProviderId", "")
      val result = inputForm.entryForm.bind(noIdpFields).get
      result.identityProvider   shouldBe None
      result.identityProviderId shouldBe None
    }
  }
  "searchForm" should {
    "handle validation scenarios for table" in {
      forAll(tableSearchQueryForm) { (_, request, expectedErrors) =>
        val form = inputForm.searchForm
        val result = form.bind(request)
        result.errors should contain theSameElementsAs expectedErrors
      }
    }
  }
}
