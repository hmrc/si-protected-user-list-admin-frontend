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

package models

import config.AppConfig.{SiProtectedUserConfig, StrideConfig}
import models.backend.TaxIdentifierType.{NINO, SAUTR}
import models.backend._
import models.forms.{Insert, Update, groupMaxLength}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}
import uk.gov.hmrc.auth.core.Enrolment
import uk.gov.hmrc.domain.{Generator, Nino, SaUtr, SaUtrGenerator}

trait Generators {
  val randomNonEmptyAlphaNumStrings: Gen[String] = Gen.alphaNumStr.filter(_.nonEmpty)

  def nonEmptyStringOfGen(length: Int): Gen[String] = Gen.listOfN(length, Gen.alphaChar).map(_.mkString)

  val ninoGen: Gen[Nino] = Gen.const(new Generator().nextNino)

  val sautrGen: Gen[SaUtr] = Gen.const(new SaUtrGenerator().nextSaUtr)

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
    strideEnrolments   <- Gen.const(Set.empty[Enrolment])
  } yield StrideConfig(strideLoginBaseUrl, strideEnrolments)

  val genNINO: Gen[String] = for {
    char1  <- Gen const 'A'
    char2  <- Gen const 'A'
    digits <- Gen.listOfN(6, Gen.numChar)
    char3  <- Gen.oneOf('A' to 'D')
  } yield s"$char1$char2${digits.mkString}$char3"

  val genSAUTR: Gen[String] = Gen.listOfN(10, Gen.numChar).map(_.mkString)

  val genTaxId: Gen[TaxIdentifier] = Gen.oneOf(
    genNINO.map(TaxIdentifier(NINO, _)),
    genSAUTR.map(TaxIdentifier(SAUTR, _))
  )

  val protectedUserGen: Gen[ProtectedUser] = for {
    taxId              <- genTaxId
    identityProviderId <- arbitrary[Option[IdentityProviderId]]
    group              <- Gen.asciiPrintableStr
    team               <- randomNonEmptyAlphaNumStrings
  } yield ProtectedUser(taxId, identityProviderId, team, group)

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

  implicit val genGroup: Gen[String] = for {
    length <- Gen.chooseNum(1, groupMaxLength)
    chars  <- Gen.listOfN(length, Gen.asciiPrintableChar)
  } yield chars.mkString

  implicit val arbProtectedUser: Arbitrary[ProtectedUser] = Arbitrary(
    for {
      taxId    <- genTaxId
      optIdpID <- arbitrary[Option[IdentityProviderId]]
      team     <- Gen.alphaNumStr if team.nonEmpty
      group    <- genGroup
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

  val idpNames: Gen[String] = Gen.oneOf("GG", "OL")

  val idpValues: Gen[String] = Gen.alphaNumStr.filter(_.nonEmpty)

  val genIdpID: Gen[IdentityProviderId] = for {
    name  <- idpNames
    value <- idpValues
  } yield IdentityProviderId(name, value)

  val validInsertModels: Gen[Insert] = for {
    taxID <- genTaxId
    optNINO = if (taxID.name == NINO) Some(taxID.value) else None
    optSAUTR = if (taxID.name == SAUTR) Some(taxID.value) else None
    optIdpID <- arbitrary[Option[IdentityProviderId]]
    group    <- genGroup
    team     <- Gen.alphaStr if team.nonEmpty
  } yield Insert(optNINO, optSAUTR, optIdpID, group, team)

  val validUpdateModels: Gen[Update] = for {
    optIdpID <- arbitrary[Option[IdentityProviderId]]
    group    <- genGroup
    team     <- Gen.alphaStr if team.nonEmpty
  } yield Update(optIdpID, group, team)
}
