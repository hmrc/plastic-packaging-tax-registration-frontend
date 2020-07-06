import play.core.PlayVersion.current
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-frontend-play-27" % "2.23.0",
    "uk.gov.hmrc"             %% "govuk-template"             % "5.55.0-play-27",
    "uk.gov.hmrc"             %% "play-ui"                    % "8.11.0-play-27"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-27"   % "2.23.0" % Test,
    "org.scalatest"           %% "scalatest"                % "3.1.2"                 % Test,
    "org.jsoup"               %  "jsoup"                    % "1.10.2"                % Test,
    "com.typesafe.play"       %% "play-test"                % current                 % Test,
    "com.vladsch.flexmark"    %  "flexmark-all"             % "0.35.10"               % "test, it",
    "org.scalatestplus.play"  %% "scalatestplus-play"       % "4.0.3"                 % "test, it"
  )
}
