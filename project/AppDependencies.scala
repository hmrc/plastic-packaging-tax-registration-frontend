import play.core.PlayVersion.current
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  val compile = Seq(
    ws,
    "uk.gov.hmrc"             %% "bootstrap-frontend-play-27" % "2.23.0",
    "uk.gov.hmrc"             %% "govuk-template"             % "5.61.0-play-27",
    "uk.gov.hmrc"             %% "play-ui"                    % "8.20.0-play-27",
    "uk.gov.hmrc"             %% "play-frontend-govuk"        % "0.57.0-play-27",
    "uk.gov.hmrc"             %% "play-frontend-hmrc"         % "0.35.0-play-27",
    "org.webjars.npm"         %  "govuk-frontend"             % "3.10.2",
    "org.webjars.npm"         %  "hmrc-frontend"              % "1.23.0"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-27"   % "2.23.0"                % Test,
    "org.scalatest"           %% "scalatest"                % "3.1.2"                 % Test,
    "org.jsoup"               %  "jsoup"                    % "1.10.2"                % Test,
    "com.typesafe.play"       %% "play-test"                % current                 % Test,
    "org.mockito"             %  "mockito-core"             % "3.5.7"                 % Test,
    "org.scalatestplus"       %% "scalatestplus-mockito"    % "1.0.0-M2"              % Test,
    "com.vladsch.flexmark"    %  "flexmark-all"             % "0.35.10"               % "test, it",
    "org.scalatestplus.play"  %% "scalatestplus-play"       % "4.0.3"                 % "test, it"
  )
}
