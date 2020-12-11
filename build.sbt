// 2.2.0-RC1 good
// 2.2.0-RC2 good
// 2.2.0-RC3 good
// 2.2.0+ bad
val catsEffectVer = "2.2.0"
//val catsEffectVer = "2.2.0"
//val catsEffectVer = "2.3.0"

lazy val root = Project("root", file("."))
  .settings(commonSettings)
  .settings(
    name := "Scala Starter",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "2.3.0",
      "org.typelevel" %% "cats-effect" % catsEffectVer,
//      "co.fs2" %% "fs2-core" % "2.4.6",
      "org.typelevel" %% "cats-effect-laws" % catsEffectVer % Test,
      "org.scalatest" %% "scalatest" % "3.2.3" % Test,
    ),
  )

lazy val commonSettings = Seq(
  version := "0.1.0",
  scalaVersion := "2.13.4",
  scalacOptions --= {
    if (sys.env.get("CI").isDefined) {
      Seq.empty
    } else {
      Seq("-Xfatal-warnings")
    }
  },
  addCompilerPlugin(
    "org.typelevel" %% "kind-projector" % "0.11.2" cross CrossVersion.full,
  ),
  addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
)
