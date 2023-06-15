import play.core.PlayVersion.current
import play.sbt.PlayImport._
import sbt._

object AppDependencies {

  val bootstrapVersion = "7.15.0"

  val compile = Seq(
    ws,
    "uk.gov.hmrc"             %% "bootstrap-frontend-play-28" % bootstrapVersion,
    "uk.gov.hmrc"             %% "play-frontend-hmrc"         % "7.9.0-play-28",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-28"         % "1.2.0",
    "uk.gov.hmrc"             %% "play-conditional-form-mapping" % "1.12.0-play-28"
  )
  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"     % bootstrapVersion        % Test,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-28"    % "1.2.0"                % Test,
    "org.scalatest"           %% "scalatest"                  % "3.2.5"                 % Test,
    "org.mockito"             %% "mockito-scala"              % "1.17.12"               % Test,
    "org.jsoup"               %  "jsoup"                      % "1.13.1"                % Test,
    "com.typesafe.play"       %% "play-test"                  % current                 % Test,
    "org.mockito"             %  "mockito-core"               % "3.9.0"                 % Test,
    "org.scalatestplus"       %% "scalatestplus-mockito"      % "1.0.0-M2"              % Test,
    "com.vladsch.flexmark"    %  "flexmark-all"               % "0.36.8"                % "test, it",
    "org.scalatestplus.play"  %% "scalatestplus-play"         % "5.1.0"                 % "test, it",
    "com.github.tomakehurst"  %  "wiremock-jre8"              % "2.26.3"                % "test, it",
    "org.pegdown"             %  "pegdown"                    % "1.6.0"                 % "test, it",
    "org.mockito"             %% "mockito-scala-scalatest"    % "1.17.14"               % Test
  )
}
