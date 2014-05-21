package me.dribba.sbt

import sbt._
import sbt.Keys._
import com.typesafe.sbt.web._
import com.typesafe.sbt.jse.{SbtJsEngine, SbtJsTask}
import com.typesafe.sbt.web.pipeline.Pipeline
import com.typesafe.sbt.web.PathMapping
import java.nio.charset.Charset
import com.typesafe.sbt.web.Import.WebKeys
import spray.json.{JsObject, JsString, JsArray}
import scala.collection.immutable
import java.io.FileInputStream
import sbt.Task

object Import {

  object Es6ModulesKeys {

    sealed abstract class ModuleType(val name: String)

    case object AMD extends ModuleType("AMD")

    case object CJS extends ModuleType("CJS")

    case object YUI extends ModuleType("YUI")


    val es6modules = TaskKey[Pipeline.Stage]("es6modules", "Compiles ES6 Modules import/export.")

    val moduleType = SettingKey[ModuleType]("module-type", "Type of module structure to use when compiling")

  }

}

object SbtEs6ModuleTranspiler extends AutoPlugin {

  override def requires = SbtJsTask

  override def trigger = AllRequirements

  val autoImport = Import

  import autoImport.Es6ModulesKeys._
  import WebKeys._
  import SbtWeb.autoImport._
  import SbtJsEngine.autoImport.JsEngineKeys._
  import SbtJsTask.autoImport.JsTaskKeys._

  val es6moduleUnscopedSettings = Seq(

    includeFilter in es6modules := GlobFilter("*.js"),
    excludeFilter in es6modules := HiddenFileFilter,

    es6modules := runOptimizer.dependsOn(webModules in Assets).value
  )

  override def projectSettings = Seq(

    moduleType in es6modules := AMD,

    resourceManaged in es6modules in Assets := webTarget.value / es6modules.key.label / "main",
    resourceManaged in es6modules in TestAssets := webTarget.value / es6modules.key.label / "test"

  ) ++ es6moduleUnscopedSettings


  val Utf8 = Charset.forName("UTF-8")

  private def runOptimizer: Def.Initialize[Task[Pipeline.Stage]] = Def.task {
    mappings: Seq[PathMapping] =>

      val include = (includeFilter in es6modules).value
      val exclude = (excludeFilter in es6modules).value
      val targetBase = (resourceManaged in es6modules in Assets).value

      val jsOptions = JsObject(
        "moduleType" -> JsString((moduleType in es6modules).value.name)
      ).toString()

      val webjarPattern = ".*webjars/lib.*".r

      val transpilerMappings = mappings.filter(f => webjarPattern.findFirstMatchIn(f._1.getAbsolutePath).isEmpty).filter(f => {
        !f._1.isDirectory && include.accept(f._1) && !exclude.accept(f._1)
      })

      val fileMap = transpilerMappings.map(f => f._1.getAbsolutePath -> f._2).toMap
      //      println(transpilerMappings)


      val transpiler = SbtWeb.copyResourceTo(
        (target in Plugin).value / es6modules.key.label,
        getClass.getClassLoader.getResource("transpiler.js"),
        streams.value.cacheDirectory / "copy-resource"
      )

      val cacheDirectory = streams.value.cacheDirectory / es6modules.key.label
      val runUpdate = FileFunction.cached(cacheDirectory, FilesInfo.hash) {
        files =>

          val filesToCompile = files.map(file => file -> fileMap(file.getAbsolutePath))
          val jsonFilesToCompile = filesToCompile.map(x => JsArray(JsString(x._1.getCanonicalPath), JsString(x._2)))

          //          println(filesToCompile)

          val args = immutable.Seq(
            JsArray(jsonFilesToCompile.toList).toString(),
            targetBase.getAbsolutePath,
            jsOptions
          )

          SbtJsTask.executeJs(
            state.value,
            (engineType in es6modules).value,
            (command in es6modules).value,
            (nodeModules in Plugin).value.map(_.getCanonicalPath),
            transpiler,
            args,
            (timeoutPerSource in es6modules).value
          )

          files
      }

      runUpdate(transpilerMappings.map(_._1).toSet)

      val strBase = targetBase.getAbsolutePath

      // Got it from sbt-rjs
      val compiled = targetBase.***.get.filter(f => f.isFile && f.getAbsolutePath.startsWith(strBase)).pair(relativeTo(targetBase))

      (mappings.toSet -- transpilerMappings.toSet ++ compiled).toSeq
  }


}