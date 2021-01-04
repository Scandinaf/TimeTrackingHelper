import sbt._

object Dependencies {

  object UUID {
    private val version = "0.3.1"
    val scalaUUID = "io.jvm.uuid" %% "scala-uuid" % version
  }

  object Cats {
    private val version = "2.3.1"
    val core = "org.typelevel" %% "cats-core" % version
    val effect = "org.typelevel" %% "cats-effect" % version
  }

  object FS2 {
    private val version = "2.5.0"
    val core = "co.fs2" %% "fs2-core" % version
  }

  object Http4s {
    private val version = "0.21.15"
    val server = "org.http4s" %% "http4s-blaze-server" % version
    val dsl = "org.http4s" %% "http4s-dsl" % version
    val client = "org.http4s" %% "http4s-blaze-client" % version
    val circe = "org.http4s" %% "http4s-circe" % version
  }

  object Outlook {
    private val version = "2.0"
    val ewsJavaApi = "com.microsoft.ews-java-api" % "ews-java-api" % version
  }

  object HtmlParser {
    private val version = "2.2.0"
    val scraper = "net.ruippeixotog" %% "scala-scraper" % version
  }

  object Sttp {
    private val version = "2.2.9"
    val sttpClient = "com.softwaremill.sttp.client" %% "core" % version
  }

  object Config {
    private val typeSafeVersion = "1.4.1"
    val typeSafe = "com.typesafe" % "config" % typeSafeVersion
    private val pureConfigVersion = "0.14.0"
    val pureConfig = "com.github.pureconfig" %% "pureconfig" % pureConfigVersion
  }

  object Logging {
    private val scalaLoggingVersion = "3.9.2"
    val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingVersion
    private val logbackClassicVersion = "1.2.3"
    val logbackClassic = "ch.qos.logback" % "logback-classic" % logbackClassicVersion
  }

  object Javax {
    private val version = "2.3.1"
    val jaxwsApi = "javax.xml.ws" % "jaxws-api" % version
  }

  object Test {
    private val version = "3.2.3"
    val scalatest = "org.scalatest" %% "scalatest" % version % "test"
    private val mockitoVersion = "1.16.2"
    val mockito = "org.mockito" %% "mockito-scala" % mockitoVersion % "test"
  }

}
