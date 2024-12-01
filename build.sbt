// Project Metadata
ThisBuild / scalaVersion := "2.13.8"
ThisBuild / version := "0.1.0-SNAPSHOT"

// Enable Protobuf Plugin
enablePlugins(ProtocPlugin)

lazy val root = (project in file("."))
  .settings(
    name := "Bedrock-gRPC-Client",
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-core" % "1.3.0-alpha10",
      "ch.qos.logback" % "logback-classic" % "1.3.0-alpha10",
      "org.scalatest" %% "scalatest" % "3.2.15" % Test,
      "com.typesafe.akka" %% "akka-http-testkit" % "10.2.10" % Test,
      "org.scalaj" %% "scalaj-http" % "2.4.2", // HTTP Client
      "com.typesafe.akka" %% "akka-actor-typed" % "2.6.19",
      "com.typesafe.akka" %% "akka-http" % "10.2.10",
      "com.typesafe.akka" %% "akka-stream" % "2.6.19",
      "io.spray" %% "spray-json" % "1.3.6",
      "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion, // Protobuf + gRPC
      "com.typesafe" % "config" % "1.4.1", // Config File Support
      "io.grpc" % "grpc-netty" % "1.44.1",
      "io.grpc" % "grpc-netty-shaded" % "1.56.0",
      // Ensure this version or newer
      "io.grpc" % "grpc-protobuf" % "1.56.0",
      "org.mockito" %% "mockito-scala" % "1.17.12" % Test,
      "io.grpc" % "grpc-stub" % "1.56.0",
      "io.github.ollama4j" % "ollama4j" % "1.0.79",
    ),
    mainClass in Compile := Some("example.grpc.main.Main")
,Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.Flat,
      Compile / PB.targets := Seq(
      scalapb.gen() -> (Compile / sourceManaged).value
    ),
    Compile / PB.protoSources ++= Seq(
      file("src/main/proto") // Explicitly specify the location of proto files
    ),
      // Assembly settings
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", "services", xs@_*) => MergeStrategy.concat
      case PathList("reference.conf") => MergeStrategy.concat
      case PathList("META-INF", xs@_*) => MergeStrategy.discard
      case _ => MergeStrategy.first
    }

  )
