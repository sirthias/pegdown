name := "pegdown"

version := "1.5.8"

homepage := Some(new URL("http://pegdown.org"))

organization := "org.pegdown"

organizationHomepage := Some(new URL("http://pegdown.org"))

description := "A Java 1.6+ library providing a clean and lightweight markdown processor"

startYear := Some(2009)

licenses := Seq("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt"))

javacOptions ++= Seq(
  "-g", // required for byte-code rewriting in parboiled-java!
  "-deprecation",
  "-target", "1.6",
  "-source", "1.6",
  "-encoding", "utf8",
  "-Xlint:unchecked"
)

javacOptions in doc := Seq(
  "-source", "1.6",
  "-encoding", "utf8"
)

scalaVersion := "2.11.5"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies   ++= Seq(
  "org.parboiled" % "parboiled-java" % "1.1.7",
  "net.sf.jtidy" % "jtidy" % "r938" % "test",
  "org.specs2" %% "specs2-core" % "2.4.16" % "test"
)

// publishing

crossPaths := false

autoScalaLibrary := false

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

useGpg := true

pgpSigningKey := Some(-2321133875171851978L)

publishTo <<= version { v: String =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT")) Some("snapshots" at nexus + "content/repositories/snapshots")
  else                             Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

pomExtra :=
  <scm>
    <url>git@github.com:sirthias/pegdown.git</url>
    <connection>scm:git:git@github.com:sirthias/pegdown.git</connection>
  </scm>
  <developers>
    <developer>
      <id>sirthias</id>
      <name>Mathias Doenitz</name>
    </developer>
  </developers>
