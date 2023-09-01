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
import models.forms.Insert

final case class Insert201Scenario(strideUserPID: String, formModel: Insert) extends AbstractScenario(Seq.empty)
object Insert201Scenario extends Generators {
  import org.scalacheck.Arbitrary

  implicit val arb: Arbitrary[Insert201Scenario] = Arbitrary(
    for {
      stridePID   <- randomNonEmptyAlphaNumStrings
      insertModel <- validInsertModels
    } yield apply(stridePID, insertModel)
  )
}
