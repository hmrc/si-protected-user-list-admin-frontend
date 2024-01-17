/*
 * Copyright 2024 HM Revenue & Customs
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

package util

import models.request.{addEntryActionBlock, addEntryActionLock, groupMaxLength}
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor3}
import play.api.data.FormError

trait ScenarioTables extends TableDrivenPropertyChecks with Generators {
  protected val allRequestFieldsPresentEntryForm: Map[String, String] = Map(
    "action"             -> addEntryActionBlock,
    "nino"               -> ninoGen.sample.get.nino,
    "sautr"              -> sautrGen.sample.get.utr,
    "identityProvider"   -> nonEmptyStringGen.sample.get,
    "identityProviderId" -> nonEmptyStringGen.sample.get,
    "group"              -> nonEmptyStringOfGen(groupMaxLength).sample.get,
    "team"               -> nonEmptyStringGen.sample.get
  )

  private val missingNinoAndSautr = allRequestFieldsPresentEntryForm.updated("sautr", "").updated("nino", "")
  private val actionLockNoCredId = allRequestFieldsPresentEntryForm.updated("action", addEntryActionLock).updated("identityProviderId", "")
  private val groupIsLongerThanAllowed = allRequestFieldsPresentEntryForm.updated("group", nonEmptyStringOfGen(groupMaxLength + 1).sample.get)
  private val missingTeam = allRequestFieldsPresentEntryForm.updated("team", "")

  private val invalidTaxIdScenarios = Array(
    ("Nino regex fail when present and incorrect", allRequestFieldsPresentEntryForm.updated("nino", "bad_nino"), FormError("nino", "form.nino.regex")),
    ("sautr regex fails when present and incorrect", allRequestFieldsPresentEntryForm.updated("sautr", "bad_sautr"), FormError("sautr", "form.sautr.regex")),
    ("Nino or sautr is required when neither are present", missingNinoAndSautr, FormError("", "form.nino.sautr.required"))
  )

  private val invalidOtherScenarios = Array(
    ("Group is longer than allowed", groupIsLongerThanAllowed, FormError("group", "error.maxLength", Seq(groupMaxLength))),
    ("Team is missing", missingTeam, FormError("team", "form.team.required")),
    ("identityProviderId must be present when action is LOCK", actionLockNoCredId, FormError("identityProviderId", "form.identityProviderId.required"))
  )

  protected val invalidAddEntryScenarios: TableFor3[String, Map[String, String], FormError] = Table(
    ("Scenario", "Request fields", "Expected error"),
    invalidTaxIdScenarios ++ invalidOtherScenarios: _*
  )

  protected val invalidEditEntryScenarios: TableFor3[String, Map[String, String], FormError] = Table(
    ("Scenario", "Request fields", "Expected error"),
    invalidOtherScenarios: _*
  )
}
