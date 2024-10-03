import sbt.*

object AppDependencies {
  private val bootstrapVersion = "9.4.0"
  private val scalaTestPlusVersion = "3.2.18.0"

  private val compile = Seq(
    "uk.gov.hmrc" %% "bootstrap-frontend-play-30"            % bootstrapVersion,
    "uk.gov.hmrc" %% "play-frontend-hmrc-play-30"            % "10.9.0",
    "uk.gov.hmrc" %% "play-conditional-form-mapping-play-30" % "3.2.0"
  )

  private val test = Seq(
    "uk.gov.hmrc"                  %% "bootstrap-test-play-30"          % bootstrapVersion,
    "uk.gov.hmrc"                  %% "government-gateway-test-play-30" % "7.0.0",
    "uk.gov.hmrc"                  %% "domain-play-30"                  % "10.0.0",
    "org.scalatestplus"            %% "scalacheck-1-17"                 % scalaTestPlusVersion,
    "org.scalatestplus"            %% "mockito-4-11"                    % scalaTestPlusVersion,
    "com.vladsch.flexmark"         %  "flexmark-all"                    % "0.64.8",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"            % "2.17.0"
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test
}
