package models

import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.wordspec.AnyWordSpec
import play.api.data.FormError
import util.Generators
class InputFormsSpec extends AnyWordSpec with Matchers with Generators with TableDrivenPropertyChecks {
  implicit val arbChar: Arbitrary[Char] = Arbitrary(Gen.alphaNumChar)
  val allRequestFieldsPresent = Map(
    "action"      -> InputForms.addEntryActionBlock,
    "nino"        -> ninoGen.sample.get.nino,
    "sautr"       -> sautrGen.sample.get.utr,
    "credId"      -> nonEmptyStringGen.sample.get,
    "group"       -> nonEmptyStringGen.sample.get,
    "idProvider"  -> nonEmptyStringGen.sample.get,
    "addedByTeam" -> nonEmptyStringGen.sample.get
  )

  val missingNinoAndSautr = allRequestFieldsPresent.updated("sautr", "").updated("nino", "")
  val actionLockNoCredId = allRequestFieldsPresent.updated("action", InputForms.addEntryActionLock).updated("credId", "")
  val table = Table(
    ("Scenario", "Request fields", "Expected errors"),
    ("Nino regex fail when present and incorrect", allRequestFieldsPresent.updated("nino", "bad_nino"), Seq(FormError("nino", "form.nino.regex"))),
    ("No Nino regex failure when not entered", allRequestFieldsPresent.updated("nino", ""), Seq()),
    ("sautur regex fails when present and incorrect", allRequestFieldsPresent.updated("sautr", "bad_sautr"), Seq(FormError("sautr", "form.sautr.regex"))),
    ("No sautr regex failure when not entered", allRequestFieldsPresent.updated("sautr", ""), Seq()),
    ("Nino or sautr is required when neither are present", missingNinoAndSautr, Seq(FormError("", "form.nino.sautr.required"))),
    ("credId must be present when action is LOCK", actionLockNoCredId, Seq(FormError("credId", "error.required")))
  )

  "EntryForm" should {
    "nino is optional when sautr is present" in {
      forAll(table) { (_, request, expectedErrors) =>
        val form = InputForms.entryForm
        val result = form.bind(request)
        result.errors should contain theSameElementsAs expectedErrors
      }
    }
  }
}
