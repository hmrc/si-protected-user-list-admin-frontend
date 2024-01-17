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
    "addedByTeam"        -> nonEmptyStringGen.sample.get
  )

  private val missingNinoAndSautr = allRequestFieldsPresentEntryForm.updated("sautr", "").updated("nino", "")
  private val actionLockNoCredId = allRequestFieldsPresentEntryForm.updated("action", addEntryActionLock).updated("identityProviderId", "")
  private val groupIsLongerThanAllowed = allRequestFieldsPresentEntryForm.updated("group", nonEmptyStringOfGen(groupMaxLength + 1).sample.get)
  private val missingAddedByTeam = allRequestFieldsPresentEntryForm.updated("addedByTeam", "")

  val invalidFormScenarios: TableFor3[String, Map[String, String], Seq[FormError]] = Table(
    ("Scenario", "Request fields", "Expected errors"),
    ("Nino regex fail when present and incorrect", allRequestFieldsPresentEntryForm.updated("nino", "bad_nino"), Seq(FormError("nino", "form.nino.regex"))),
    ("sautr regex fails when present and incorrect", allRequestFieldsPresentEntryForm.updated("sautr", "bad_sautr"), Seq(FormError("sautr", "form.sautr.regex"))),
    ("Group is longer than allowed", groupIsLongerThanAllowed, Seq(FormError("group", "error.maxLength", Seq(groupMaxLength)))),
    ("Nino or sautr is required when neither are present", missingNinoAndSautr, Seq(FormError("", "form.nino.sautr.required"))),
    ("AddedByTeam is missing", missingAddedByTeam, Seq(FormError("addedByTeam", "form.addedByTeam.required"))),
    ("identityProviderId must be present when action is LOCK", actionLockNoCredId, Seq(FormError("identityProviderId", "form.identityProviderId.required")))
  )
}
