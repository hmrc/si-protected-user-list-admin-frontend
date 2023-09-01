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
import models.backend.IdentityProviderId
import models.forms.Insert
import org.scalacheck.{Arbitrary, Gen}

final case class Insert400Scenario(strideUserPID: String, formModel: Insert) extends AbstractScenario(Seq.empty)
object Insert400Scenario extends Generators {
  import Arbitrary.arbitrary

  private val genInvalidTaxID = Gen.alphaStr

  private val invalidInsertModels = for {
    optNINO  <- Gen option genInvalidTaxID
    optSAUTR <- Gen option genInvalidTaxID
    optIdpID <- arbitrary[Option[IdentityProviderId]]
    group    <- genGroup
    team     <- Gen.asciiPrintableStr
  } yield Insert(optNINO, optSAUTR, optIdpID, group, team)

  implicit val arb: Arbitrary[Insert400Scenario] = Arbitrary(
    for {
      stridePID   <- randomNonEmptyAlphaNumStrings
      insertModel <- invalidInsertModels
    } yield apply(stridePID, insertModel)
  )
}
