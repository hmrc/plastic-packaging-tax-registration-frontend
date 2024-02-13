resolvers += "HMRC-open-artefacts-maven" at "https://open.artefacts.tax.service.gov.uk/maven2"
resolvers += Resolver.url("HMRC-open-artefacts-ivy", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(Resolver.ivyStylePatterns)

resolvers += Resolver.typesafeRepo("releases")

// To resolve a bug with version 2.x.x of the scoverage plugin - https://github.com/sbt/sbt/issues/6997
// Try to remove when sbt 1.8.0+ and scoverage is 2.0.7+
ThisBuild / libraryDependencySchemes ++= Seq("org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always)

addSbtPlugin("uk.gov.hmrc" % "sbt-auto-build" % "3.20.0")

addSbtPlugin("uk.gov.hmrc" % "sbt-distributables" % "2.2.0")

addSbtPlugin("org.playframework" % "sbt-plugin" % "3.0.1")

addSbtPlugin("io.github.irundaia" % "sbt-sassify" % "1.5.2")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.0.9")

addSbtPlugin("uk.gov.hmrc" % "sbt-accessibility-linter" % "0.33.0")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.0")
