import uk.gov.hmrc.DefaultBuildSettings
val appName = "plastic-packaging-tax-registration-frontend"

PlayKeys.devSettings := Seq("play.server.http.port" -> "8503")

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "3.3.7"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin, SbtWeb)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    TwirlKeys.templateImports ++= Seq(
      "uk.gov.hmrc.hmrcfrontend.views.html.components._",
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "views.html.components._",
      "views.components.Styles._"
    ),
    scalacOptions ++= Seq(
      "-Wconf:msg=unused import&src=html/.*:s",
      "-Wconf:msg=Flag.*repeatedly:s",
      "-Wconf:src=routes/.*:s",
    ),
  )
  .settings(scoverageSettings)
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)

lazy val scoverageSettings: Seq[Setting[_]] = Seq(
  coverageExcludedPackages := List(
    "<empty>",
    "Reverse.*",
    "metrics\\..*",
    "features\\..*",
    "test\\..*",
    ".*(BuildInfo|Routes|Options).*",
    "logger.*\\(.*\\)",
    // These are being excluded since all logic has been pushed to base class and they are tested by their non-post-registration associates
    ".*.controllers.amendment.group.AddGroupMember(OrganisationDetailsType|Grs|ContactDetailsName|ContactDetailsEmailAddress|ContactDetailsTelephoneNumber|ContactDetailsConfirmAddress)Controller",
    ".*.controllers.amendment.partner.AddPartner(Name|OrganisationDetailsType|Grs|ContactDetailsEmailAddress|ContactDetailsName|ContactDetailsTelephoneNumber)Controller"
  ).mkString(";"),
  coverageMinimumStmtTotal := 85,
  coverageFailOnMinimum    := true,
  coverageHighlighting     := true,
  Test / parallelExecution := false
)

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(DefaultBuildSettings.itSettings())
  .settings(libraryDependencies ++= AppDependencies.test)

lazy val all = taskKey[Unit]("Runs units, its, and ally tests")
all := Def.sequential(Test / test).value
