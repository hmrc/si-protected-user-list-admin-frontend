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

import config.{AuthStrideEnrolmentsConfig, SiProtectedUserConfig}
import models._
import org.scalacheck.Gen
import uk.gov.hmrc.auth.core.Enrolment
import uk.gov.hmrc.domain.{Generator, Nino, SaUtr, SaUtrGenerator}

trait Generators {
  val nonEmptyStringGen = for {
    length <- Gen.chooseNum(1, 50)
    str    <- Gen.listOfN(length, Gen.alphaChar).map(_.mkString)
  } yield str

  val ninoGen: Gen[Nino] = Gen.const(new Generator().nextNino)

  val sautrGen: Gen[SaUtr] = Gen.const(new SaUtrGenerator().nextSaUtr)

  val entryGen: Gen[Entry] = for {
    entryId            <- Gen.some(nonEmptyStringGen)
    action             <- Gen.oneOf(InputForms.addEntryActionBlock, InputForms.addEntryActionLock)
    nino               <- Gen.some(ninoGen.map(_.nino))
    sautr              <- Gen.some(sautrGen.map(_.utr))
    identityProvider   <- Gen.some(nonEmptyStringGen)
    identityProviderId <- Gen.some(nonEmptyStringGen)
    group              <- Gen.some(nonEmptyStringGen)
    addedByTeam        <- Gen.some(nonEmptyStringGen)
    updatedByTeam      <- Gen.some(nonEmptyStringGen)
    updatedByUser      <- Gen.some(nonEmptyStringGen)
    addedByUser        <- Gen.some(nonEmptyStringGen)
  } yield Entry(
    entryId = entryId,
    action = action,
    nino = nino,
    sautr = sautr,
    identityProvider = identityProvider,
    identityProviderId = identityProviderId,
    group = group,
    addedByTeam = addedByTeam,
    updatedByTeam = updatedByTeam,
    updatedByUser = updatedByUser,
    addedByUser = addedByUser
  )

  val validRequestEntryGen = entryGen.map(_.copy(entryId = None, addedByUser = None, updatedByUser = None, action = InputForms.addEntryActionLock))

  val siProtectedUserConfigGen: Gen[SiProtectedUserConfig] = for {
    bulkUploadScreenEnabled  <- Gen.const(true)
    bulkUploadRowLimit       <- Gen.chooseNum(1, 4000)
    bulkUploadBatchSize      <- Gen.chooseNum(1, 100)
    bulkUploadBatchDelaySecs <- Gen.chooseNum(1, 100)
    showAllEnabled           <- Gen.const(true)
    shutterService           <- Gen.const(false)
    listScreenRowLimit       <- Gen.chooseNum(1, 1500)
    num                      <- Gen.chooseNum(1, 10)
    addedByTeams             <- Gen.listOfN(num, nonEmptyStringGen)
    identityProviders        <- Gen.listOfN(num, nonEmptyStringGen)
  } yield SiProtectedUserConfig(
    bulkUploadScreenEnabled = bulkUploadScreenEnabled,
    bulkUploadRowLimit = bulkUploadRowLimit,
    bulkUploadBatchSize = bulkUploadBatchSize,
    bulkUploadBatchDelaySecs = bulkUploadBatchDelaySecs,
    showAllEnabled = showAllEnabled,
    shutterService = shutterService,
    listScreenRowLimit = listScreenRowLimit,
    identityProviders = identityProviders,
    addedByTeams = addedByTeams
  )

  val authStrideEnrolmentsConfigGen: Gen[AuthStrideEnrolmentsConfig] = for {
    strideLoginBaseUrl <- nonEmptyStringGen
    strideSuccessUrl   <- nonEmptyStringGen
    strideEnrolments   <- Gen.const(Set.empty[Enrolment])
  } yield AuthStrideEnrolmentsConfig(strideLoginBaseUrl = strideLoginBaseUrl, strideSuccessUrl = strideSuccessUrl, strideEnrolments = strideEnrolments)

  val taxIdTypeGen: Gen[TaxIdentifierType] = Gen.oneOf(TaxIdentifierType.values)

  val taxIdProviderIdGen: Gen[IdentityProviderId] = for {
    authProviderIdType  <- Gen oneOf Seq("GovernmentGateway", "OLfG")
    authProviderIdValue <- Gen.alphaNumStr
  } yield IdentityProviderId(authProviderIdType, authProviderIdValue)

  val taxIdGen: Gen[TaxIdentifier] = for {
    typeName <- Gen oneOf TaxIdentifierType.values
    value    <- Gen.alphaNumStr
  } yield TaxIdentifier(typeName, value)

  val protectedUserGen: Gen[ProtectedUser] = for {
    taxIdType          <- taxIdTypeGen
    taxIdValue         <- Gen.alphaNumStr
    identityProviderId <- Gen.some(taxIdProviderIdGen)
    group              <- nonEmptyStringGen
    addedByUser        <- Gen.some(nonEmptyStringGen)
    addedByTeam        <- Gen.some(nonEmptyStringGen)
    updatedByUser      <- Gen.some(nonEmptyStringGen)
    updatedByTeam      <- Gen.some(nonEmptyStringGen)
  } yield ProtectedUser(
    taxId = TaxIdentifier(taxIdType, taxIdValue),
    identityProviderId = identityProviderId,
    group = group,
    addedByUser = addedByUser,
    addedByTeam = addedByTeam,
    updatedByUser = updatedByUser,
    updatedByTeam = updatedByTeam
  )

  val protectedUserRecordGen: Gen[ProtectedUserRecord] = for {
    entryId      <- nonEmptyStringGen
    firstCreated <- Gen.posNum[Long]
    lastUpdated  <- Gen.option(Gen.posNum[Long])
    body         <- protectedUserGen

  } yield ProtectedUserRecord(
    entryId = entryId,
    firstCreated = firstCreated,
    lastUpdated = lastUpdated,
    body = body
  )
}
