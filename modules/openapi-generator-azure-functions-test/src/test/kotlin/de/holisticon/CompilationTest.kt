package de.holisticon

import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.OK
import de.holisticon.CompilationTestHelper.absPath
import de.holisticon.CompilationTestHelper.compile
import de.holisticon.CompilationTestHelper.div
import de.holisticon.CompilationTestHelper.generateOpenApi
import de.holisticon.CompilationTestHelper.recursiveKtFiles
import de.holisticon.CompilationTestHelper.testOut
import io.kotest.assertions.withClue
import io.kotest.core.names.TestName
import io.kotest.core.spec.style.FreeSpec
import io.kotest.core.spec.style.scopes.*
import io.kotest.core.test.TestScope
import io.kotest.datatest.withData
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import org.openapitools.codegen.CodegenConfig
import org.openapitools.codegen.CodegenConstants
import org.openapitools.codegen.config.GlobalSettings
import org.openapitools.codegen.languages.KotlinSpringServerCodegen
import java.io.File

class CompilationTest : FreeSpec() {
  fun springGen(to: String?) = KotlinSpringServerCodegen().also { cfg ->
    to?.let { cfg.outputDir = it }
  }

  private val openApiFiles = ".." / "openapi-generator" / "src" / "test" / "resources"
  private val openApiFiles30 = openApiFiles / "3_0"
  private val openApiFiles31 = openApiFiles / "3_1"

  init {
    "focus".config(enabled = true) {
      val openapiFile = openApiFiles30 / "unsigned-test.yaml"
//      val openapiFile = openApiFiles30 / "unusedSchemas.yaml"
      println("From: ${openapiFile.absPath}")
      GlobalSettings.setProperty(CodegenConstants.SKIP_FORM_MODEL, "false")
      generateOpenApi(
        openapiFile = openapiFile, to = "target" / "generated-sources",
        generator = CompilationTestHelper.kotlinAzureServerCodegen(null, "target" / "generated-sources"),
        codeGenConfigMod = { modCodegenConfig(it, openapiFile) }
      )

      val res = compile(File(testOut).absoluteFile.recursiveKtFiles)

      withClue("From ${openapiFile.absPath}\n${res.messages}") { res.exitCode shouldBeEqualComparingTo OK }

    }


    // generate spring for code. just used for debugging
    "regression spring".config(false) {
      val openapiFile = openApiFiles30 / "form-multipart-binary-array.yaml"
      generateOpenApi(openapiFile = openapiFile, to = testOut, generator = springGen(testOut))

      val res = compile(File(testOut).absoluteFile.recursiveKtFiles)

      withClue("From ${openapiFile.absPath}\n${res.messages}") { res.exitCode shouldBeEqualComparingTo OK }
    }


    "regression 3.0" - {
      testTraverse(File(openApiFiles30)) { file ->
        generateOpenApi(openapiFile = file.absolutePath, to = testOut, codeGenConfigMod = { modCodegenConfig(it, file.absolutePath) })
        val res = compile(File(testOut).absoluteFile.recursiveKtFiles)
        withClue("From ${file.absoluteFile}\n${res.messages}") { res.exitCode shouldBeEqualComparingTo OK }
      }
    }
    "regression 3.1" - {
      testTraverse(File(openApiFiles31)) { file ->
        generateOpenApi(openapiFile = file.absolutePath, to = testOut, codeGenConfigMod = { modCodegenConfig(it, file.absolutePath) })
        val res = compile(File(testOut).absoluteFile.recursiveKtFiles)
        withClue("From ${file.absoluteFile}\n${res.messages}") { res.exitCode shouldBeEqualComparingTo OK }
      }
    }
  }

  suspend fun ContainerScope.testTraverse(
    input: File,
    extensionFilter: Set<String> = setOf("yaml", "yml"),
    testFun: TestScope.(File) -> Unit
  ) {
    val content = input.listFiles()!!.toList().filterNotNull()
    val (dirs, files) = content
      .filter { it.isDirectory || (it.isFile && extensionFilter.contains(it.extension)) }
      .partition { f -> f.isDirectory }
    dirs.sortedBy { d -> d.name }.forEach { d ->
      registerContainer(TestName(d.name), false, null) {
        FreeSpecContainerScope(this).testTraverse(d, extensionFilter, testFun)
      }
    }
    files.sortedBy { it.name }.map { it.absoluteFile }.forEach { f ->
      registerTest(TestName(f.name), disabled = disableTestFilter(f.absolutePath), null) {
        if (!disableTestFilter(f.absolutePath)) {
          this.testFun(f)
        }
      }
    }
  }

  private fun CodegenConfig.removeFile(): CodegenConfig =
    this.also { importMapping().remove("File") }// remove File -> java.io.File mapping for this test

  private fun CodegenConfig.removeDate(): CodegenConfig =
    this.also { importMapping().remove("Date") }

  private fun CodegenConfig.addUnsigneds(): CodegenConfig = run {
    this.importMapping().put("UnsignedInteger", "java.lang.Long") // map to Long
    this.importMapping().put("UnsignedLong", "java.lang.Long")
    this
  }

  val disabledTests = setOf(
    // "UNKNOWN_BASE_TYPE" not fixed in AzureCodegen
    openApiFiles30 / "python" / "petstore-with-fake-endpoints-models-for-testing-with-http-signature.yaml",
    openApiFiles30 / "issue_10330.yaml",

    // generates @BindingName("pn0") pn0: java.math.BigDecimal = 10.0. We have to revisit that to handle default values of params.
    openApiFiles30 / "issue_10865_default_values.yaml",

    // SpecialCharacterEnum
    openApiFiles30 / "python" / "petstore-with-fake-endpoints-models-for-testing.yaml",

    // oneOf (in ComposedOneOfNumberWithValidations)
    openApiFiles30 / "python-prior" / "petstore-with-fake-endpoints-models-for-testing-with-http-signature.yaml",

    // 'String' instead of 'string' as type is not supported
    openApiFiles30 / "rust-server" / "openapi-v3.yaml",

    // ambiguous enum names in inlined and $ref enums
    openApiFiles30 / "typescript-fetch" / "enum.yaml",

    // allOf in HashMap not supported: AdditionalpropertiesShouldNotLookInApplicators
    openApiFiles30 / "unit_test_spec" / "3_0_3_unit_test_spec.yaml",

    // default values for enum arrays should be handled by another Default/Abstract*Codegen.
    // somewhere in updateCodegenPropertyEnum / updateDataTypeWithEnumForArray / toDefaultValue
    // the enum literals should be prefixed with enum type.
    openApiFiles30 / "echo_api.yaml",

    // kotlin correctly doesn't handle  listOfNulls: kotlin.Array<kotlin.Nothing>?
    openApiFiles30 / "issue_7651.yaml",

    // it's invalid on purpose and throws an exception
    openApiFiles30 / "inline_model_resolver.yaml",

    // classname Deprecated overlaps with @Deprecated annotation
    openApiFiles30 / "model-deprecated.yaml",
  ).map { it.absPath }.toSet()

  private fun disableTestFilter(filePath: String): Boolean = disabledTests.contains(filePath)


  private fun modCodegenConfig(c: CodegenConfig, filePath: String) {
    GlobalSettings.reset();
    val absFilePath = filePath.absPath
    val toRemoveFile = setOf(
      openApiFiles30 / "go" / "petstore-with-fake-endpoints-models-for-testing-with-http-signature.yaml",
      openApiFiles30 / "csharp-netcore" / "petstore-with-fake-endpoints-models-for-testing-with-http-signature.yaml",
      openApiFiles30 / "java" / "petstore-with-fake-endpoints-models-for-testing-with-http-signature-okhttp-gson.yaml",
      openApiFiles30 / "java" / "petstore-with-fake-endpoints-models-for-testing-with-http-signature.yaml",
      openApiFiles30 / "javascript" / "petstore-with-fake-endpoints-models-for-testing.yaml",
      openApiFiles30 / "java" / "native" / "petstore-with-fake-endpoints-models-for-testing-with-http-signature.yaml",
      openApiFiles30 / "powershell" / "petstore-with-fake-endpoints-models-for-testing-with-http-signature.yaml",
      openApiFiles30 / "python" / "petstore-with-fake-endpoints-models-for-testing-with-http-signature.yaml",
      openApiFiles30 / "python" / "petstore-with-fake-endpoints-models-for-testing.yaml",
      openApiFiles30 / "python-prior" / "petstore-with-fake-endpoints-models-for-testing-with-http-signature.yaml",
      openApiFiles30 / "spring" / "petstore-with-fake-endpoints-models-for-testing-with-spring-pageable.yaml",
      openApiFiles30 / "spring" / "petstore-with-fake-endpoints-models-for-testing.yaml",
      openApiFiles30 / "petstore-with-fake-endpoints-models-for-testing-with-http-signature.yaml",
      openApiFiles30 / "petstore-with-fake-endpoints-models-for-testing.yaml",
    ).map { it.absPath }.toSet()

    if (toRemoveFile.contains(absFilePath)) {
      c.removeFile()
    }

    when (absFilePath) {
      (openApiFiles30 / "csharp" / "petstore-with-fake-endpoints-models-for-testing-with-http-signature.yaml").absPath -> {
        c.removeFile().addUnsigneds()
      }

      (openApiFiles30 / "unsigned-test.yaml").absPath -> {
        c.addUnsigneds()
      }

      (openApiFiles30 / "r" / "petstore.yaml").absPath -> {
        c.removeFile().removeDate()
      }

      (openApiFiles30 / "form-multipart-binary-array.yaml").absPath -> {
        GlobalSettings.setProperty(CodegenConstants.SKIP_FORM_MODEL, "false")
      }

      (openApiFiles30 / "petstore-with-object-as-parameter.yaml").absPath -> {
        GlobalSettings.setProperty(CodegenConstants.SKIP_FORM_MODEL, "false")
      }
    }
  }
}
