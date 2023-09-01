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

import models.backend.ProtectedUserRecord
import org.scalacheck.Arbitrary

final case class Delete200Scenario(
  strideUserPID: String,
  record:        ProtectedUserRecord
) extends AbstractScenario(Seq(record))

object Delete200Scenario extends models.Generators {
  import Arbitrary.arbitrary

  implicit val arb: Arbitrary[Delete200Scenario] = Arbitrary(
    for {
      strideUserPID <- randomNonEmptyAlphaNumStrings
      record        <- arbitrary[ProtectedUserRecord]
    } yield apply(strideUserPID, record)
  )
}
