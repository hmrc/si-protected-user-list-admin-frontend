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

package util

import config.SiProtectedUserConfig
import org.scalacheck.Gen

trait Generators {
  val nonEmptyStringGen = for {
    length <- Gen.chooseNum(1, 50)
    str    <- Gen.listOfN(length, Gen.alphaChar).map(_.mkString)
  } yield str

  val siProtectedUserConfigGen: Gen[SiProtectedUserConfig] = for {
    bulkUploadScreenEnabled  <- Gen.const(true)
    bulkUploadRowLimit       <- Gen.chooseNum(1, 4000)
    bulkUploadBatchSize      <- Gen.chooseNum(1, 100)
    bulkUploadBatchDelaySecs <- Gen.chooseNum(1, 100)
    showAllEnabled           <- Gen.const(true)
    shutterService           <- Gen.const(false)
    listScreenRowLimit       <- Gen.chooseNum(1, 1500)
  } yield SiProtectedUserConfig(
    bulkUploadScreenEnabled = bulkUploadScreenEnabled,
    bulkUploadRowLimit = bulkUploadRowLimit,
    bulkUploadBatchSize = bulkUploadBatchSize,
    bulkUploadBatchDelaySecs = bulkUploadBatchDelaySecs,
    showAllEnabled = showAllEnabled,
    shutterService = shutterService,
    listScreenRowLimit = listScreenRowLimit
  )
}
