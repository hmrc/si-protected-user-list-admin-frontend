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
import models.*
import org.scalacheck.Gen
import uk.gov.hmrc.auth.core.Enrolment
import uk.gov.hmrc.auth.core.retrieve.{Name, ~}
import uk.gov.hmrc.domain.{Generator, Nino, SaUtr, SaUtrGenerator}

trait Generators {
  val nonEmptyStringGen: Gen[String] = for {
    length <- Gen.chooseNum(1, 50)
    str    <- Gen.listOfN(length, Gen.alphaChar).map(_.mkString)
  } yield str

  val nonEmptyPrintableStringGen: Gen[String] = for {
    length <- Gen.chooseNum(1, 64)
    str    <- Gen.listOfN(length, Gen.asciiPrintableChar).map(_.filterNot(ch => InputForms.disallowedCharacters.contains(ch)).mkString)
  } yield str

  val asciiUnprintableChars: Gen[Char] =
    Gen.oneOf((1 to 31) ++ (127 to 255)).map(_.toChar)

  val nonEmptyNonPrintableStringGen: Gen[String] = for {
    length <- Gen.chooseNum(1, 64)
    str    <- Gen.listOfN(length, asciiUnprintableChars).map(_.mkString)
  } yield str

  val disallowedCharStringGen: Gen[String] = for {
    str <- Gen.atLeastOne(InputForms.disallowedCharacters).map(_.mkString)
  } yield str

  def nonEmptyStringOfGen(length: Int): Gen[String] = Gen.listOfN(length, Gen.alphaChar).map(_.mkString)

  val ninoGen: Gen[Nino] = Gen.const(new Generator().nextNino)

  val sautrGen: Gen[SaUtr] = Gen.const(new SaUtrGenerator().nextSaUtr)

  val entryGen: Gen[Entry] = for {
    action             <- Gen.oneOf(InputForms.addEntryActionBlock, InputForms.addEntryActionLock)
    nino               <- Gen.some(ninoGen.map(_.nino))
    sautr              <- Gen.some(sautrGen.map(_.utr))
    identityProvider   <- Gen.some(nonEmptyStringGen)
    identityProviderId <- Gen.some(nonEmptyStringGen)
    group              <- Gen.some(nonEmptyStringOfGen(groupMaxLength))
    addedByTeam        <- nonEmptyStringGen
    updatedByTeam      <- Gen.some(nonEmptyStringGen)
    updatedByUser      <- Gen.some(nonEmptyStringGen)
    addedByUser        <- Gen.some(nonEmptyStringGen)
  } yield Entry(
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

  val validRequestEntryGen: Gen[Entry] = entryGen.map(_.copy(addedByUser = None, updatedByUser = None, action = InputForms.addEntryActionLock))
  val validEditEntryGen: Gen[Entry] = entryGen.map(_.copy(addedByUser = None, updatedByUser = None, action = InputForms.addEntryActionLock))

  val siProtectedUserConfigGen: Gen[SiProtectedUserConfig] = for {
    num               <- Gen.chooseNum(1, 10)
    addedByTeams      <- Gen.listOfN(num, nonEmptyStringGen)
    identityProviders <- Gen.listOfN(num, nonEmptyStringGen)
  } yield SiProtectedUserConfig(
    dashboardUrl      = "http://gov.uk",
    identityProviders = identityProviders,
    addedByTeams      = addedByTeams
  )

  val authStrideEnrolmentsConfigGen: Gen[StrideConfig] = for {
    strideLoginBaseUrl <- nonEmptyStringGen
    strideEnrolments   <- Gen.const(Set.empty[Enrolment])
  } yield StrideConfig(strideLoginBaseUrl = strideLoginBaseUrl, strideEnrolments = strideEnrolments)

  val taxIdTypeGen: Gen[TaxIdentifierType] = Gen.oneOf(TaxIdentifierType.values)

  val taxIdProviderIdGen: Gen[IdentityProviderId] = for {
    authProviderIdType  <- Gen oneOf Seq("GovernmentGateway", "OLfG")
    authProviderIdValue <- Gen.alphaNumStr
  } yield IdentityProviderId(authProviderIdType, authProviderIdValue)

  val taxIdGen: Gen[TaxIdentifier] = for {
    typeName <- Gen oneOf TaxIdentifierType.values
    value    <- nonEmptyStringGen
  } yield TaxIdentifier(typeName, value)

  val protectedUserGen: Gen[ProtectedUser] = for {
    taxIdType          <- taxIdTypeGen
    taxIdValue         <- nonEmptyStringGen
    identityProviderId <- Gen.some(taxIdProviderIdGen)
    group              <- nonEmptyStringGen
    addedByUser        <- Gen.some(nonEmptyStringGen)
    addedByTeam        <- Gen.some(nonEmptyStringGen)
    updatedByUser      <- Gen.some(nonEmptyStringGen)
    updatedByTeam      <- Gen.some(nonEmptyStringGen)
  } yield ProtectedUser(
    taxId              = TaxIdentifier(taxIdType, taxIdValue),
    identityProviderId = identityProviderId,
    group              = group,
    addedByUser        = addedByUser,
    addedByTeam        = addedByTeam,
    updatedByUser      = updatedByUser,
    updatedByTeam      = updatedByTeam
  )

  val protectedUserRecords: Gen[ProtectedUserRecord] =
    for {
      entryId      <- nonEmptyStringGen
      firstCreated <- Gen.posNum[Long]
      lastUpdated  <- Gen.option(Gen.posNum[Long])
      body         <- protectedUserGen
    } yield ProtectedUserRecord(
      entryId      = entryId,
      firstCreated = firstCreated,
      lastUpdated  = lastUpdated,
      body         = body
    )

  val optionalNonEmptyStringGen: Gen[Option[String]] = Gen.option(nonEmptyStringGen)

  def stridePidGen(forceStridePid: Boolean): Gen[Option[String]] = {
    if (forceStridePid) nonEmptyStringGen.map(Some(_))
    else optionalNonEmptyStringGen
  }

  val nameGen: Gen[Option[Name]] = {
    Gen.option(
      for {
        firstName <- optionalNonEmptyStringGen
        lastName  <- optionalNonEmptyStringGen
      } yield Name(firstName, lastName)
    )
  }

  def retrievalResultGen(forceStridePid: Boolean): Gen[Option[String] ~ Option[Name]] = {
    for {
      stridePidOpt <- stridePidGen(forceStridePid)
      nameOpt      <- nameGen
    } yield new ~(stridePidOpt, nameOpt)
  }

}
