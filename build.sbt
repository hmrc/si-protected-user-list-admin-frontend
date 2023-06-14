import uk.gov.hmrc.DefaultBuildSettings.integrationTestSettings

lazy val microservice = Project("si-protected-user-list-admin-frontend", file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .settings(integrationTestSettings() *)
  .settings(CodeCoverageSettings.settings *)
  .settings(
    majorVersion := 0,
    scalaVersion := "2.13.8",
    libraryDependencies ++= AppDependencies(),
    // https://www.scala-lang.org/2021/01/12/configuring-and-suppressing-warnings.html
    // suppress warnings in generated routes files
    scalacOptions += "-Wconf:src=routes/.*:s",
    scalacOptions += "-Wconf:cat=unused-imports&src=html/.*:s",
    pipelineStages := Seq(gzip),
    IntegrationTest / dependencyClasspath ++= (Test / exportedProducts).value,
    resolvers += Resolver.jcenterRepo,
    scalafmtOnCompile := true,
    PlayKeys.playDefaultPort := 8508,
    TwirlKeys.templateImports ++= Seq(
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.helpers._"
    )
  )
  .configs(IntegrationTest)
