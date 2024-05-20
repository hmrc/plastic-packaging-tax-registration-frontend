import play.core.PlayVersion.current
import play.sbt.PlayImport._
import sbt._

object AppDependencies {

  private val bootstrapVersion = "8.6.0"
  private val playVersion      = "30"
  private val mongoVersion     = "1.9.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"       %% s"bootstrap-frontend-play-$playVersion" % bootstrapVersion,
    "uk.gov.hmrc"       %% "play-frontend-hmrc-play-30"            % "9.10.0",
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-play-$playVersion"         % mongoVersion,
    "uk.gov.hmrc"       %% "play-conditional-form-mapping-play-30" % "2.0.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% s"bootstrap-test-play-$playVersion"  % bootstrapVersion,
    "uk.gov.hmrc.mongo"      %% s"hmrc-mongo-test-play-$playVersion" % mongoVersion,
    "org.scalatestplus.play" %% "scalatestplus-play"                 % "7.0.1",
    "org.mockito"            %% "mockito-scala-scalatest"            % "1.17.29"
  ).map(_ % "test")

}
