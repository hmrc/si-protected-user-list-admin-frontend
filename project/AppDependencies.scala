import sbt.*

object AppDependencies {
  private val bootstrapVersion = "9.13.0"
  private val scalaTestPlusVersion = "3.2.19.0"

  private val compile = Seq(
    "uk.gov.hmrc" %% "bootstrap-frontend-play-30"            % bootstrapVersion,
    "uk.gov.hmrc" %% "play-frontend-hmrc-play-30"            % "12.6.0",
    "uk.gov.hmrc" %% "play-conditional-form-mapping-play-30" % "3.3.0"
  )

  private val test = Seq(
    "uk.gov.hmrc"                  %% "bootstrap-test-play-30"          % bootstrapVersion,
    "uk.gov.hmrc"                  %% "domain-play-30"                  % "10.0.0",
    "org.scalatestplus"            %% "scalacheck-1-18"                 % scalaTestPlusVersion
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test
}
