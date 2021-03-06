package com.iheart.sbtPlaySwagger

import com.sohoffice.doc.extract.sbt.DocExtractPlugin
import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport._
import sbt.Attributed._
import sbt.Keys._
import sbt.{ AutoPlugin, _ }
import com.typesafe.sbt.web.Import._

object SwaggerPlugin extends AutoPlugin {
  lazy val SwaggerConfig = config("play-swagger").hide
  lazy val playSwaggerVersion = com.sohoffice.playSwagger.BuildInfo.version

  object autoImport extends SwaggerKeys

  override def requires: Plugins = JavaAppPackaging && DocExtractPlugin

  override def trigger = noTrigger

  import autoImport._

  override def projectConfigurations: Seq[Configuration] = Seq(SwaggerConfig)

  override def projectSettings: Seq[Setting[_]] = Seq(
    ivyConfigurations += SwaggerConfig,
    resolvers += Resolver.jcenterRepo,
    //todo: remove hardcoded org name using BuildInfo
    libraryDependencies += "com.sohoffice" %% "descriptive-play-swagger" % playSwaggerVersion % SwaggerConfig,
    swaggerDomainNameSpaces := Seq(),
    swaggerV3 := false,
    swaggerTarget := target.value / "swagger",
    swaggerFileName := "swagger.json",
    swaggerRoutesFile := "routes",
    swaggerOutputTransformers := Seq(),
    swaggerAPIVersion := version.value,
    swaggerPrettyJson := false,
    swaggerDescriptionFile := None,
    swagger := Def.task[File] {
      (swaggerTarget.value).mkdirs()
      val file = swaggerTarget.value / swaggerFileName.value
      IO.delete(file)
      val args: Seq[String] = file.absolutePath :: swaggerRoutesFile.value ::
        swaggerDomainNameSpaces.value.mkString(",") ::
        swaggerOutputTransformers.value.mkString(",") ::
        swaggerV3.value.toString ::
        swaggerAPIVersion.value ::
        swaggerPrettyJson.value.toString ::
        swaggerDescriptionFile.value.map { f ⇒
          List(
            "--description-file", f.getAbsolutePath)
        }.getOrElse(Nil)
      val swaggerClasspath = data((fullClasspath in Runtime).value) ++ update.value.select(configurationFilter(SwaggerConfig.name))
      runner.value.run("com.iheart.playSwagger.SwaggerSpecRunner", swaggerClasspath, args, streams.value.log).failed foreach (sys error _.getMessage)
      file
    }.value,
    unmanagedResourceDirectories in Assets += swaggerTarget.value,
    mappings in (Compile, packageBin) += (swaggerTarget.value / swaggerFileName.value) → s"public/${swaggerFileName.value}", //include it in the unmanagedResourceDirectories in Assets doesn't automatically include it package
    packageBin in Universal := (packageBin in Universal).dependsOn(swagger).value,
    run := (run in Compile).dependsOn(swagger).evaluated,
    stage := stage.dependsOn(swagger).value
  ) ++ DocExtractPluginSupport.docExtractSettings
}

