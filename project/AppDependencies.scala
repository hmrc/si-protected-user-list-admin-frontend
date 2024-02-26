import sbt.*

object AppDependencies {
  private val bootstrapVersion = "7.21.0"

  private val compile = Seq(
    "uk.gov.hmrc" %% "bootstrap-frontend-play-28"    % bootstrapVersion,
    "uk.gov.hmrc" %% "play-frontend-hmrc-play-28"    % "8.5.0",
    "uk.gov.hmrc" %% "play-conditional-form-mapping" % "1.13.0-play-28"
  )

  private val test = Seq(
    "uk.gov.hmrc"                  %% "bootstrap-test-play-28"  % bootstrapVersion,
    "uk.gov.hmrc"                  %% "government-gateway-test" % "5.2.0",
    "uk.gov.hmrc"                  %% "domain"                  % "8.3.0-play-28",
    "org.scalatestplus"            %% "scalacheck-1-17"         % "3.2.16.0",
    "com.vladsch.flexmark"          % "flexmark-all"            % "0.64.6",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"    % "2.14.2"
  ).map(_ % "test,it")

  def apply(): Seq[ModuleID] = compile ++ test
}
