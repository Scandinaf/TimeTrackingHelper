import Dependencies.{Test, _}

name := "Time Tracking Helper"
version := "0.1"
scalaVersion := "2.12.10"

lazy val root = project.in(file("."))
  .settings(
    mainClass in(Compile, run) := Some("com.eg.timeTrackingHelper.Main"),
    scalacOptions ++= Seq("-feature", "-deprecation", "-unchecked", "-Ypartial-unification", "-language:postfixOps"),
    libraryDependencies ++=
      Seq(
        Cats.core,
        Cats.effect,
        FS2.core,
        Http4s.dsl,
        Http4s.server,
        Http4s.circe,
        Http4s.client,
        UUID.scalaUUID,
        Outlook.ewsJavaApi,
        Sttp.sttpClient,
        HtmlParser.scraper,
        Config.typeSafe,
        Config.pureConfig,
        Javax.jaxwsApi,
        Logging.logbackClassic,
        Logging.scalaLogging,
        Test.scalatest,
        Test.mockito,
      )
  ).dependsOn(jira4s)

lazy val jira4s = RootProject(uri("https://github.com/Scandinaf/jira4s.git"))