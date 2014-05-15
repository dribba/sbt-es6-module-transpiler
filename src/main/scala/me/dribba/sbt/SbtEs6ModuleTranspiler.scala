package me.dribba.sbt

import sbt._
import sbt.Keys._
import com.typesafe.sbt.web._
import com.typesafe.sbt.jse.SbtJsTask
import spray.json._

object Import {

  object Es6ModulesKeys {

    sealed abstract class ModuleType(val name: String)

    case object AMD extends ModuleType("AMD")

    case object CJS extends ModuleType("CJS")

    case object YUI extends ModuleType("YUI")


    val es6modules = TaskKey[Seq[File]]("es6modules", "Compiles ES6 Modules import/export.")

    val moduleType = SettingKey[ModuleType]("module-type", "Type of module structure to use when compiling")
  }

}

object SbtEs6ModuleTranspiler extends AutoPlugin {

  override def requires = SbtJsTask

  override def trigger = AllRequirements

  val autoImport = Import

  import SbtWeb.autoImport._
  import WebKeys._
  import SbtJsTask.autoImport.JsTaskKeys._
  import autoImport.Es6ModulesKeys._

  val es6moduleUnscopedSettings = Seq(

    includeFilter := GlobFilter("*.js"),

    jsOptions := JsObject(
      "moduleType" -> JsString(moduleType.value.name)
    ).toString()

  )

  override def projectSettings = Seq(

  ) ++ inTask(es6modules)(
    SbtJsTask.jsTaskSpecificUnscopedSettings ++
      inConfig(Assets)(es6moduleUnscopedSettings) ++
      inConfig(TestAssets)(es6moduleUnscopedSettings) ++
      Seq(
        moduleName := "es6modules",
        shellFile := getClass.getClassLoader.getResource("transpiler.js"),

        taskMessage in Assets := "Transpiling",
        taskMessage in TestAssets := "Transpiling"
      )
  ) ++ SbtJsTask.addJsSourceFileTasks(es6modules) ++ Seq(
    es6modules in Assets := (es6modules in Assets).dependsOn(webModules in Assets).value,
    es6modules in TestAssets := (es6modules in TestAssets).dependsOn(webModules in TestAssets).value
  )

}