name := """keendly"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

resolvers += "Local Maven Repository" at Path.userHome.asFile.toURI.toURL + ".m2/repository"

libraryDependencies ++= Seq(
  javaJpa,
  "org.hibernate" % "hibernate-entitymanager" % "4.3.7.Final",
  "org.powermock" % "powermock-api-mockito" % "1.6.2",
  "org.powermock" % "powermock-module-junit4" % "1.6.2",
//  "de.jetwick" % "snacktory" % "1.2.1-keendly-SNAPSHOT",
//  "com.jindle" % "jindle" % "0.0.1-SNAPSHOT",
  "com.sendgrid" % "sendgrid-java" % "2.2.1"
)

libraryDependencies ++= Seq(
  javaWs
)

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator

// needed to allow remote debugging
fork in run := false
