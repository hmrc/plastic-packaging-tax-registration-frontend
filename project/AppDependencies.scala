import play.core.PlayVersion.current
import play.sbt.PlayImport._
import sbt._

object AppDependencies {

  val compile = Seq(
    ws,
    "uk.gov.hmrc"             %% "bootstrap-frontend-play-28" % "5.14.0",
    "uk.gov.hmrc"             %% "play-frontend-hmrc"         % "1.11.0-play-28",
    "uk.gov.hmrc"             %% "play-conditional-form-mapping" % "1.9.0-play-28",
    "org.webjars.npm"         %  "govuk-frontend"             % "3.11.0",
    "org.webjars.npm"         %  "hmrc-frontend"              % "1.32.0"
  )
  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"     % "5.14.0"                 % Test,
    "org.scalatest"           %% "scalatest"                  % "3.2.5"                 % Test,
    "org.jsoup"               %  "jsoup"                      % "1.13.1"                % Test,
    "com.typesafe.play"       %% "play-test"                  % current                 % Test,
    "org.mockito"             %  "mockito-core"               % "3.9.0"                 % Test,
    "org.scalatestplus"       %% "scalatestplus-mockito"      % "1.0.0-M2"              % Test,
    "com.vladsch.flexmark"    %  "flexmark-all"               % "0.36.8"                % "test, it",
    "org.scalatestplus.play"  %% "scalatestplus-play"         % "5.1.0"                 % "test, it",
    "com.github.tomakehurst"  %  "wiremock-jre8"              % "2.26.3"                % "test, it",
    "org.pegdown"             %  "pegdown"                    % "1.6.0"                 % "test, it"
  )
}
