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

import config.{SiProtectedUserConfig, StrideConfig}
import models.InputForms.groupMaxLength
import models._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}
import uk.gov.hmrc.auth.core.Enrolment
import uk.gov.hmrc.domain.{Generator, Nino, SaUtr, SaUtrGenerator}

trait Generators {
  val randomNonEmptyAlphaNumStrings: Gen[String] = Gen.alphaNumStr.filter(_.nonEmpty)

  def nonEmptyStringOfGen(length: Int): Gen[String] = Gen.listOfN(length, Gen.alphaChar).map(_.mkString)

  val ninoGen: Gen[Nino] = Gen.const(new Generator().nextNino)

  val sautrGen: Gen[SaUtr] = Gen.const(new SaUtrGenerator().nextSaUtr)

  val entryGen: Gen[Entry] = for {
    entryId            <- Gen.some(randomNonEmptyAlphaNumStrings)
    action             <- Gen.oneOf(InputForms.addEntryActionBlock, InputForms.addEntryActionLock)
    nino               <- Gen.some(ninoGen.map(_.nino))
    sautr              <- Gen.some(sautrGen.map(_.utr))
    identityProvider   <- Gen.some(randomNonEmptyAlphaNumStrings)
    identityProviderId <- Gen.some(randomNonEmptyAlphaNumStrings)
    group              <- Gen.some(nonEmptyStringOfGen(groupMaxLength))
    addedByTeam        <- Gen.some(randomNonEmptyAlphaNumStrings)
    updatedByTeam      <- Gen.some(randomNonEmptyAlphaNumStrings)
    updatedByUser      <- Gen.some(randomNonEmptyAlphaNumStrings)
    addedByUser        <- Gen.some(randomNonEmptyAlphaNumStrings)
  } yield Entry(
    entryId            = entryId,
    action             = action,
    nino               = nino,
    sautr              = sautr,
    identityProvider   = identityProvider,
    identityProviderId = identityProviderId,
    group              = group,
    addedByTeam        = addedByTeam,
    updatedByTeam      = updatedByTeam,
    updatedByUser      = updatedByUser,
    addedByUser        = addedByUser
  )

  val validRequestEntryGen: Gen[Entry] = entryGen.map(_.copy(entryId = None, addedByUser = None, updatedByUser = None, action = InputForms.addEntryActionLock))
  val validEditEntryGen = entryGen.map(_.copy(addedByUser = None, updatedByUser = None, action = InputForms.addEntryActionLock))

  val siProtectedUserConfigGen: Gen[SiProtectedUserConfig] = for {
    num               <- Gen.chooseNum(1, 10)
    addedByTeams      <- Gen.listOfN(num, randomNonEmptyAlphaNumStrings)
    identityProviders <- Gen.listOfN(num, randomNonEmptyAlphaNumStrings)
  } yield SiProtectedUserConfig(
    dashboardUrl      = "http://gov.uk",
    identityProviders = identityProviders,
    addedByTeams      = addedByTeams
  )

  val authStrideEnrolmentsConfigGen: Gen[StrideConfig] = for {
    strideLoginBaseUrl <- randomNonEmptyAlphaNumStrings
    strideSuccessUrl   <- randomNonEmptyAlphaNumStrings
    strideEnrolments   <- Gen.const(Set.empty[Enrolment])
  } yield StrideConfig(strideLoginBaseUrl = strideLoginBaseUrl, strideSuccessUrl = strideSuccessUrl, strideEnrolments = strideEnrolments)

  val taxIdTypeGen: Gen[TaxIdentifierType] = Gen.oneOf(TaxIdentifierType.values)

  val taxIdProviderIdGen: Gen[IdentityProviderId] = for {
    authProviderIdType  <- Gen oneOf Seq("GovernmentGateway", "OLfG")
    authProviderIdValue <- Gen.alphaNumStr
  } yield IdentityProviderId(authProviderIdType, authProviderIdValue)

  val taxIdGen: Gen[TaxIdentifier] = for {
    typeName <- Gen oneOf TaxIdentifierType.values
    value    <- randomNonEmptyAlphaNumStrings
  } yield TaxIdentifier(typeName, value)

  val protectedUserGen: Gen[ProtectedUser] = for {
    taxIdType          <- taxIdTypeGen
    taxIdValue         <- randomNonEmptyAlphaNumStrings
    identityProviderId <- Gen.some(taxIdProviderIdGen)
    group              <- randomNonEmptyAlphaNumStrings
    addedByTeam        <- Gen.some(randomNonEmptyAlphaNumStrings)
  } yield ProtectedUser(
    taxId              = TaxIdentifier(taxIdType, taxIdValue),
    identityProviderId = identityProviderId,
    team               = addedByTeam getOrElse "",
    group              = group
  )

  implicit val protectedUserRecordArb: Arbitrary[ProtectedUserRecord] = Arbitrary(
    for {
      entryId <- randomNonEmptyAlphaNumStrings
      created <- arbitrary[Modified]
      updated <- arbitrary[Option[Modified]]
      body    <- arbitrary[ProtectedUser]
    } yield ProtectedUserRecord(entryId, created, updated, body)
  )

  implicit val arbModified: Arbitrary[Modified] = Arbitrary(
    for {
      calendar      <- Gen.calendar
      strideUserPid <- randomNonEmptyAlphaNumStrings
    } yield Modified(calendar.toInstant, strideUserPid)
  )

  implicit val arbProtectedUser: Arbitrary[ProtectedUser] = Arbitrary(
    for {
      taxId    <- arbitrary[TaxIdentifier]
      optIdpID <- arbitrary[Option[IdentityProviderId]]
      team     <- Gen.alphaNumStr if team.nonEmpty
      group    <- Gen.asciiPrintableStr
    } yield ProtectedUser(taxId, optIdpID, team, group)
  )

  implicit val arbAuthProviderId: Arbitrary[IdentityProviderId] =
    Arbitrary(
      for {
        authProviderIdType  <- Gen oneOf Seq("GovernmentGateway", "OLfG")
        authProviderIdValue <- Gen.uuid.map(_.toString)
      } yield IdentityProviderId(authProviderIdType, authProviderIdValue)
    )

  implicit val arbTaxId: Arbitrary[TaxIdentifier] =
    Arbitrary(
      for {
        typeName <- Gen oneOf TaxIdentifierType.values
        value    <- Gen.uuid.map(_.toString)
      } yield TaxIdentifier(typeName, value)
    )
}
