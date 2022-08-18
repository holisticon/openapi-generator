package de.holisticon

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.kotest.core.test.TestScope
import io.kotest.core.test.parents
import io.swagger.parser.OpenAPIParser
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.parser.core.models.ParseOptions
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import org.openapitools.codegen.ClientOptInput
import org.openapitools.codegen.DefaultGenerator
import org.openapitools.codegen.languages.KotlinAzureFunctionAppServerCodegen
import org.openapitools.codegen.utils.ModelUtils
import java.io.ByteArrayOutputStream
import java.io.File

object CompilationTestHelper {
  val File.recursiveFiles: List<File>
    get() = this.listFiles()?.flatMap { file ->
      if (file.isDirectory)
        file.recursiveFiles
      else
        listOf(file)
    } ?: emptyList()

  val File.recursiveKtFiles: List<File>
    get() = this.recursiveFiles.filter { "kt" == it.extension }

  private fun parseSpec(specFilePath: String?): OpenAPI {
    val openAPI = OpenAPIParser().readLocation(specFilePath, null, ParseOptions()).openAPI
    ModelUtils.getOpenApiVersion(openAPI, specFilePath, null)
    return openAPI
  }

  fun generateOpenApi(openapiFile: String, extensionModelInputFile: String? = null, to: String?) {
    val options = ClientOptInput().apply {
      openAPI(parseSpec(openapiFile))
      config(
        KotlinAzureFunctionAppServerCodegen().also { cfg ->
          to?.let { cfg.outputDir = it }
          extensionModelInputFile?.let { cfg.additionalProperties()[KotlinAzureFunctionAppServerCodegen.EXTENSION_MODEL_PROPERTY_KEY] = it }
          cfg.additionalProperties()[KotlinAzureFunctionAppServerCodegen.MUSTACHE_DEBUG_PROPERTY_KEY] = true
          // output.absolutePath
          // val projectId = "org.openapitools."
          // codegen.setApiPackage(projectId + "api")
          // codegen.setModelPackage(projectId + "api.model")
          // codegen.additionalProperties()[CodegenConstants.SOURCE_FOLDER] = "src/gen/kotlin"
          // codegen.additionalProperties().put(CodegenConstants.MODEL_PACKAGE, baseModelPackage + ".yyyy.model.xxxx");
        }
      )
    }
    DefaultGenerator().opts(options).generate()
  }


  fun compile(path: List<File>, vararg codeSnippets: String): KotlinCompilation.Result =
    KotlinCompilation()
      .apply {
        sources =
          path.map { SourceFile.fromPath(it) } + codeSnippets.mapIndexed { i, snippet -> SourceFile.kotlin("Snippet${i}.kt", snippet) }
        inheritClassPath = true
        verbose = false
        messageOutputStream = ByteArrayOutputStream()
      }.compile()

  val String.pathReady: String
    get() = this.let { "\\s".toRegex().replace(it, "_") }
  val TestScope.parentPath: String
    get() = this.testCase.parents().let { parents ->
      parents.map { it.name.testName.pathReady }.joinToString(separator = File.separator) + (parents.ifNotEmpty { File.separator } ?: "")
    }
  val TestScope.testOut: String
    get() = "target" / "compile-test-generated-sources" / "${this.parentPath}${this.testCase.name.testName.pathReady}"

  operator fun String.div(other: String) = this + File.separator + other
}
