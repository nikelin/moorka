import sbt._
import sbt.Keys._
import bintray.Keys._

val currentScalaVersion = "2.11.5"
crossScalaVersions  := Seq("2.11.5", "2.10.4")
val moorkaVersion = "0.4.0-SNAPSHOT"

scalaVersion := currentScalaVersion

val dontPublish = Seq(
  publish := { }
)

val commonSettings = Seq(
  version := moorkaVersion,
  organization := "com.tenderowls.opensource",
  licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html")),
  homepage := Some(url("http://github.com/tenderowls/moorka")),
  scalacOptions ++= Seq("-deprecation", "-feature")
)

val utestSetting = Seq(
  scalaJSStage in Test := FastOptStage,
  persistLauncher in Test := false,
  testFrameworks += new TestFramework("utest.runner.Framework"),
  libraryDependencies += "com.lihaoyi" %%% "utest" % "0.3.0" % "test"
)

val publishSettings = moorkaVersion.endsWith("SNAPSHOT") match {
  case true => Seq(
    publishTo := Some("Flexis Thirdparty Snapshots" at "https://nexus.flexis.ru/content/repositories/thirdparty-snapshots"),
    credentials += {
      val ivyHome = sys.props.get("sbt.ivy.home") match {
        case Some(path) ⇒ file(path)
        case None ⇒ Path.userHome / ".ivy2"
      }
      Credentials(ivyHome / ".credentials")
    }
  )
  case false => bintraySettings ++ bintrayPublishSettings ++ Seq(
    repository in bintray := "moorka",
    bintrayOrganization in bintray := Some("tenderowls"),
    publishMavenStyle := false
  )
}

lazy val `moorka-resources-plugin` = (project in file("moorka-resources-plugin"))
  .settings(publishSettings:_*)
  .settings(commonSettings:_*)
  .settings(
    scalaVersion := "2.10.4",
    sbtPlugin := true
  )

lazy val `moorka-core` = (project in file("moorka-core"))
  .enablePlugins(ScalaJSPlugin)
  .settings(publishSettings:_*)
  .settings(commonSettings:_*)
  .settings(utestSetting:_*)
  .settings(
    scalaVersion := currentScalaVersion,
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.7.0",
      "org.scala-lang" % "scala-reflect" % currentScalaVersion
    )
  )

lazy val `moorka-ui` = (project in file("moorka-ui"))
  .enablePlugins(ScalaJSPlugin)
  .settings(publishSettings:_*)
  .settings(commonSettings:_*)
  .settings(
    scalaVersion := currentScalaVersion
  )
  .dependsOn(`moorka-core`)

lazy val root = (project in file("."))
  .settings(dontPublish:_*)
  .settings(
    scalaVersion := currentScalaVersion
  )
  .aggregate(
    `moorka-resources-plugin`,
    `moorka-ui`,
    `moorka-core`
  )

