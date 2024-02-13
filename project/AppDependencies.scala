import play.core.PlayVersion.current
import play.sbt.PlayImport._
import sbt._

object AppDependencies {

  private val bootstrapVersion = "8.4.0"
  private val playVersion      = "30"
  private var mongoVersion     = "1.7.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"       %% s"bootstrap-frontend-play-$playVersion" % bootstrapVersion,
    "uk.gov.hmrc"       %% "play-frontend-hmrc"                    % "7.29.0-play-28",
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-play-$playVersion"         % mongoVersion,
    "uk.gov.hmrc"       %% "play-conditional-form-mapping"         % "1.12.0-play-28"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% s"bootstrap-test-play-$playVersion"  % bootstrapVersion % Test,
    "uk.gov.hmrc.mongo"      %% s"hmrc-mongo-test-play-$playVersion" % mongoVersion     % Test,
    "org.scalatest"          %% "scalatest"                          % "3.2.15"         % Test,
    "org.mockito"            %% "mockito-scala"                      % "1.17.12"        % Test,
    "org.jsoup"               % "jsoup"                              % "1.15.4"         % Test,
    "org.playframework"      %% "play-test"                          % current          % Test,
    "org.scalatestplus"      %% "scalatestplus-mockito"              % "1.0.0-M2"       % Test,
    "com.vladsch.flexmark"    % "flexmark-all"                       % "0.64.6"         % "test, it",
    "org.scalatestplus.play" %% "scalatestplus-play"                 % "5.1.0"          % "test, it",
    "org.mockito"            %% "mockito-scala-scalatest"            % "1.17.14"        % Test
  )

}
