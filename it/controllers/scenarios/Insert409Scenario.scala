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

package controllers.scenarios

import models.Generators
import models.backend.ProtectedUserRecord
import models.backend.TaxIdentifierType.{NINO, SAUTR}
import models.forms.Insert

final case class Insert409Scenario(
  initialRecord: ProtectedUserRecord,
  strideUserPID: String,
  group:         String,
  team:          String
) extends AbstractScenario(Seq(initialRecord)) {
  def formModel: Insert = {
    val taxID = initialRecord.body.taxId
    Insert(
      if (taxID.name == NINO) Some(taxID.value) else None,
      if (taxID.name == SAUTR) Some(taxID.value) else None,
      initialRecord.body.identityProviderId,
      group,
      team
    )
  }
}
object Insert409Scenario extends Generators {
  import org.scalacheck.Arbitrary
  import Arbitrary.arbitrary

  implicit val arb: Arbitrary[Insert409Scenario] = Arbitrary(
    for {
      initialRecord <- arbitrary[ProtectedUserRecord]
      stridePID     <- randomNonEmptyAlphaNumStrings
      group         <- genValidGroup
      team          <- randomNonEmptyAlphaNumStrings
    } yield apply(initialRecord, stridePID, group, team)
  )
}
