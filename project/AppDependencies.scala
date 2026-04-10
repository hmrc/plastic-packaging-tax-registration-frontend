import play.core.PlayVersion.current
import play.sbt.PlayImport._
import sbt._

object AppDependencies {

  private val bootstrapVersion = "10.7.0"
  private val playVersion      = "play-30"
  private val mongoVersion     = "2.12.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"       %% s"bootstrap-frontend-$playVersion"            % bootstrapVersion,
    "uk.gov.hmrc"       %% s"play-frontend-hmrc-$playVersion"            % "12.32.0",
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-$playVersion"                    % mongoVersion,
    "uk.gov.hmrc"       %% s"play-conditional-form-mapping-$playVersion" % "3.5.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% s"bootstrap-test-$playVersion"  % bootstrapVersion,
    "uk.gov.hmrc.mongo"      %% s"hmrc-mongo-test-$playVersion" % mongoVersion,
    "org.scalatestplus.play" %% "scalatestplus-play"            % "7.0.2",
    "org.scalatestplus"      %% "mockito-5-21"                  % "3.2.19.0"
  ).map(_ % "test")

}
