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
  .configs(A11yTest)
  .settings(resolvers += Resolver.jcenterRepo)
  .settings(headerSettings(A11yTest): _*)
  .settings(automateHeaderSettings(A11yTest))
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
    // These are being excluded since all logic has been pushed to base class and they are tested by their non-post-registration associates
    ".*.controllers.amendment.group.AddGroupMember(OrganisationDetailsType|Grs|ContactDetailsName|ContactDetailsEmailAddress|ContactDetailsTelephoneNumber|ContactDetailsConfirmAddress)Controller",
    ".*.controllers.amendment.partner.AddPartner(Name|OrganisationDetailsType|Grs|ContactDetailsEmailAddress|ContactDetailsName|ContactDetailsTelephoneNumber)Controller",
  ).mkString(";"),
  coverageMinimum := 85, // TODO reduced whilst new-liability in progress
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
