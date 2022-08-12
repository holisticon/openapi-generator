package de.holisticon

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.test.TestCase
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.comparables.shouldBeEqualComparingTo

import io.kotest.matchers.compilation.shouldCompile
import io.kotest.matchers.compilation.shouldNotCompile
import io.kotest.matchers.shouldBe
import io.swagger.parser.OpenAPIParser
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.parser.core.models.ParseOptions
import org.openapitools.codegen.ClientOptInput
import org.openapitools.codegen.DefaultGenerator
import org.openapitools.codegen.InlineModelResolver
import org.openapitools.codegen.languages.KotlinAzureFunctionAppServerCodegen
import org.openapitools.codegen.utils.ModelUtils
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files

class CompilationTest : StringSpec() {
  fun parseFlattenSpec(specFilePath: String): OpenAPI {
    val openAPI = parseSpec(specFilePath)
    return openAPI
  }

  fun parseSpec(specFilePath: String?): OpenAPI {
    val openAPI = OpenAPIParser().readLocation(specFilePath, null, ParseOptions()).openAPI
    ModelUtils.getOpenApiVersion(openAPI, specFilePath, null)
    return openAPI
  }

  fun gen(openapiFile: String, extensionModelInputFile: String?, output: String?) {
    // paths
//    val testProject = "../../samples/server/petstore/kotlin-azure-function-app/"
//    val extensionModelInputFile = File(testProject + "model/kotlinAzureExtensionModel.yaml").path
//    val openAPI = TestUtils.parseFlattenSpec(this.javaClass.getResource(openapiFile).path)
    val openAPI = parseFlattenSpec(openapiFile)
//    val output = File(testProject + "target/generated-sources/openapi/")
    val codegen = KotlinAzureFunctionAppServerCodegen()

    output?.let {
      codegen.outputDir = it // output.absolutePath
    }
//    val projectId = "org.openapitools."
//    codegen.setApiPackage(projectId + "api")
//    codegen.setModelPackage(projectId + "api.model")
//    codegen.additionalProperties()[CodegenConstants.SOURCE_FOLDER] = "src/gen/kotlin"

//    codegen.additionalProperties().put(CodegenConstants.MODEL_PACKAGE, baseModelPackage + ".yyyy.model.xxxx");

    extensionModelInputFile?.let {
      codegen.additionalProperties()[KotlinAzureFunctionAppServerCodegen.EXTENSION_MODEL_PROPERTY_KEY] = it
    }

    val input = ClientOptInput()
    input.openAPI(openAPI)
    input.config(codegen)
    val generator = DefaultGenerator()
    generator.opts(input).generate()
  }

  fun cs(codeSnippet: String, path: List<File>): KotlinCompilation.Result {
    val kotlinCompilation = KotlinCompilation()
      .apply {
        sources = path.map { SourceFile.fromPath(it) } + SourceFile.kotlin("KClass.kt", codeSnippet)
        inheritClassPath = true
        verbose = false
        messageOutputStream = ByteArrayOutputStream()
      }
    val compilationResult = kotlinCompilation.compile()
    kotlinCompilation.workingDir.deleteRecursively()

    return compilationResult
  }

  init {
    "shouldCompile test" {
      val codeSnippet = """ class CompilationABC {}  """.trimMargin()

      codeSnippet.shouldCompile()
//      File("SourceFile.kt").shouldCompile()
    }

    "shouldNotCompile test" {
      val codeSnippet = """ val aInteger: Int = "A invalid assignment" """.trimMargin()

      codeSnippet.shouldNotCompile()
//      File("SourceFile.kt").shouldNotCompile()
    }

    "lala" {
      val output = "target/generated-test-sources/${testCase.name.testName}"
      gen(
        "../openapi-generator/src/test/resources/3_0/3248-regression-dates.yaml",
        null,
        output
      )

      val code1 = """
        package apis
        import apis.DefaultAzureFunctionInterface
        class DefaultApi : DefaultAzureFunctionInterface {}"""

      val code2 = """
        package apis
        class DefaultXApi {}"""

      val aout = File(output).absoluteFile

      val res = cs(code1,
        aout.listFilesRecursively().filter { "kt" == it.extension }
      )
      withClue(res.messages) { res.exitCode shouldBeEqualComparingTo KotlinCompilation.ExitCode.OK }

    }
  }

  fun File.listFilesRecursively(): List<File> {
    return listFiles()?.flatMap { file ->
      if(file.isDirectory)
        file.listFilesRecursively()
      else
        listOf(file)
    } ?: emptyList()
  }

}
