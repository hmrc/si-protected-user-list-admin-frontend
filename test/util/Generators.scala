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
import models.TaxIdentifierType.{NINO, SAUTR}
import models._
import org.scalacheck.Gen
import uk.gov.hmrc.auth.core.Enrolment
import uk.gov.hmrc.domain.{Generator, Nino, SaUtr, SaUtrGenerator}

trait Generators {
  def alphaNumStringsOfLength(from: Int, to: Int): Gen[String] = for {
    length <- Gen.chooseNum(from, to)
    chars  <- Gen.listOfN(length, Gen.alphaNumChar)
  } yield chars.mkString

  val nonEmptyStringGen: Gen[String] = alphaNumStringsOfLength(1, 50)

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

  val siProtectedUserConfigGen: Gen[SiProtectedUserConfig] = for {
    num               <- Gen.chooseNum(1, 10)
    addedByTeams      <- Gen.listOfN(num, nonEmptyStringGen)
    identityProviders <- Gen.listOfN(num, nonEmptyStringGen)
  } yield SiProtectedUserConfig(
    dashboardUrl = "http://gov.uk",
    identityProviders = identityProviders,
    addedByTeams = addedByTeams
  )

  val authStrideEnrolmentsConfigGen: Gen[StrideConfig] = for {
    strideLoginBaseUrl <- nonEmptyStringGen
    strideEnrolments   <- Gen.const(Set.empty[Enrolment])
  } yield StrideConfig(strideLoginBaseUrl = strideLoginBaseUrl, strideEnrolments = strideEnrolments)

  val taxIdTypeGen: Gen[TaxIdentifierType] = Gen.oneOf(TaxIdentifierType.values)

  val identityProviderIds: Gen[IdentityProviderId] = for {
    name  <- Gen.alphaNumStr if name.nonEmpty
    value <- Gen.alphaNumStr if value.nonEmpty
  } yield IdentityProviderId(name, value)

  val taxIdentifiers: Gen[TaxIdentifier] = Gen.oneOf(
    ninoGen.map(nino => TaxIdentifier(NINO, nino.nino)),
    sautrGen.map(sautr => TaxIdentifier(SAUTR, sautr.utr))
  )

  val protectedUserGen: Gen[ProtectedUser] = for {
    taxId              <- taxIdentifiers
    identityProviderId <- Gen option identityProviderIds
    group              <- alphaNumStringsOfLength(1, groupMaxLength)
    addedByUser        <- Gen.some(nonEmptyStringGen)
    addedByTeam        <- Gen.some(nonEmptyStringGen)
    updatedByUser      <- Gen.some(nonEmptyStringGen)
    updatedByTeam      <- Gen.some(nonEmptyStringGen)
  } yield ProtectedUser(
    taxId = taxId,
    identityProviderId = identityProviderId,
    group = group,
    addedByUser = addedByUser,
    addedByTeam = addedByTeam,
    updatedByUser = updatedByUser,
    updatedByTeam = updatedByTeam
  )

  val protectedUserRecords: Gen[ProtectedUserRecord] =
    for {
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
