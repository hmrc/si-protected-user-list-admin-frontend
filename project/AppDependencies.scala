import sbt.*

object AppDependencies {

  val compile = Seq(
    "com.auth0"                     % "java-jwt"                      % "4.0.0",
    "uk.gov.hmrc"                  %% "bootstrap-frontend-play-28"    % "7.14.0",
    "uk.gov.hmrc"                  %% "play-frontend-hmrc"            % "7.4.0-play-28",
    "uk.gov.hmrc"                  %% "play-conditional-form-mapping" % "1.13.0-play-28",
    "uk.gov.hmrc"                  %% "http-caching-client"           % "10.0.0-play-28",
    "io.github.zamblauskas"        %% "scala-csv-parser"              % "0.13.1",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"          % "2.14.2"
  )

  val test = Seq(
    "uk.gov.hmrc"          %% "government-gateway-test" % "5.0.0"         % "test,it",
    "uk.gov.hmrc"          %% "bootstrap-test-play-28"  % "7.14.0"        % "test,it",
    "uk.gov.hmrc"          %% "domain"                  % "8.3.0-play-28" % "test,it",
    "org.scalatestplus"    %% "scalacheck-1-15"         % "3.2.11.0"      % "test,it",
    "org.scalacheck"       %% "scalacheck"              % "1.17.0"        % "test,it",
    "com.vladsch.flexmark"  % "flexmark-all"            % "0.64.0"        % "test,it",
    "io.github.wolfendale" %% "scalacheck-gen-regexp"   % "1.1.0"         % "test,it"
  )
}
