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
import org.scalacheck.{Arbitrary, Gen}

final case class View404Scenario(
  entryID:       String,
  strideUserPID: String
) extends AbstractScenario(initRecords = Seq.empty)

object View404Scenario extends Generators {
  implicit val arb: Arbitrary[View404Scenario] = Arbitrary(
    for {
      entryID <- Gen.alphaNumStr if entryID.nonEmpty
      pid     <- Gen.alphaNumStr if pid.nonEmpty
    } yield apply(entryID, pid)
  )
}
