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

import models.InputForms.searchQueryMaxLength
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.data.FormError
import util.ScenarioTables

class InputFormsSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite with ScenarioTables {

  val inputForm = app.injector.instanceOf[InputForms]

  val allRequestFieldsPresentSearchQuery = Map(
    "filterByTeam" -> "All",
    "searchQuery"  -> nonEmptyPrintableStringGen.sample.get
  )

  val onlyFilterByTeamFieldPresentInSearchQuery = Map(
    "filterByTeam" -> "All"
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
