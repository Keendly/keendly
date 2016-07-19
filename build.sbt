import play.routes.compiler.InjectedRoutesGenerator
import play.sbt.PlayJava

name := """keendly"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

resolvers += "Local Maven Repository" at Path.userHome.asFile.toURI.toURL + ".m2/repository"

libraryDependencies ++= Seq(
  javaJpa,
  javaWs,
  "org.hibernate" % "hibernate-entitymanager" % "4.3.7.Final",
  "org.powermock" % "powermock-api-mockito" % "1.6.2" % "test",
  "org.powermock" % "powermock-module-junit4" % "1.6.2" % "test",
  "postgresql" % "postgresql" % "9.1-901.jdbc4",
  "io.jsonwebtoken" % "jjwt" % "0.6.0",
  "com.google.protobuf" % "protobuf-java" % "2.6.1",
  "com.amazonaws" % "aws-java-sdk-s3" % "1.10.52",
  "com.amazonaws" % "aws-java-sdk-simpleworkflow" % "1.10.77",
  "com.github.tomakehurst" % "wiremock" % "1.58" % "test",
  "org.projectlombok" % "lombok" % "1.16.8" % "test"
)

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator

// needed to allow remote debugging
fork in run := false
PlayKeys.externalizeResources := false
