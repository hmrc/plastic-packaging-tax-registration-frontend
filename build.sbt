import uk.gov.hmrc.DefaultBuildSettings.integrationTestSettings
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

val appName = "plastic-packaging-tax-registration-frontend"

PlayKeys.devSettings := Seq("play.server.http.port" -> "8503")

val silencerVersion = "1.7.0"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtDistributablesPlugin, SbtWeb)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(
    majorVersion                     := 0,
    scalaVersion                     := "2.12.11",
    libraryDependencies              ++= AppDependencies.compile ++ AppDependencies.test,
    TwirlKeys.templateImports        ++= Seq(
      "uk.gov.hmrc.hmrcfrontend.views.html.components._",
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "uk.gov.hmrc.plasticpackagingtax.registration.views.html.components._",
      "uk.gov.hmrc.plasticpackagingtax.registration.views.components.Styles._"),

    // ***************
    // Use the silencer plugin to suppress warnings
    // You may turn it on for `views` too to suppress warnings from unused imports in compiled twirl templates, but this will hide other warnings.
    scalacOptions += "-P:silencer:pathFilters=routes",
    libraryDependencies ++= Seq(
      compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
      "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full
    )
    // ***************
  )
  .settings(publishingSettings: _*)
  .configs(IntegrationTest)
  .settings(integrationTestSettings(): _*)
  .settings(resolvers += Resolver.jcenterRepo)
  .settings(scoverageSettings)
  .settings(silencerSettings)

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
    "uk.gov.hmrc.plasticpackagingtax.registration.controllers.partnership.PartnershipOtherPartnerCheckAnswersController",
    "uk.gov.hmrc.plasticpackagingtax.registration.controllers.partnership.PartnershipPartnersListController",
    "uk.gov.hmrc.plasticpackagingtax.registration.views.html.partnerships.other_partners_page",
    "uk.gov.hmrc.plasticpackagingtax.registration.views.html.partnerships.check_other_partner_answers_page"
  ).mkString(";"),
  coverageMinimum := 95.2,
  coverageFailOnMinimum := true,
  coverageHighlighting := true,
  parallelExecution in Test := false
)

lazy val silencerSettings: Seq[Setting[_]] = {
  val silencerVersion = "1.7.0"
  Seq(
    libraryDependencies ++= Seq(compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full)),
    // silence all warnings on autogenerated files
    scalacOptions += "-P:silencer:pathFilters=target/.*",
    // Make sure you only exclude warnings for the project directories, i.e. make builds reproducible
    scalacOptions += s"-P:silencer:sourceRoots=${baseDirectory.value.getCanonicalPath}"
  )
}
