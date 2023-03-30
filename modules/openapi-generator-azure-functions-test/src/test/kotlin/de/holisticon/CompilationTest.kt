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
    "focus" {
      val openapiFile = openApiFiles30 / "helidon" / "petstore-for-testing.yaml"
      println("From: ${openapiFile.absPath}")
      generateOpenApi(
        openapiFile = openapiFile, to = "target" / "generated-sources",
        generator = CompilationTestHelper.kotlinAzureServerCodegen(null, "target" / "generated-sources"),
        codeGenConfigMod = { modCodegenConfig(it, openapiFile) }
      )

      val res = compile(File(testOut).absoluteFile.recursiveKtFiles)

      withClue("From ${openapiFile.absPath}\n${res.messages}") { res.exitCode shouldBeEqualComparingTo OK }

    }


    // generate spring for code. just used for debugging
    "regression spring".config(false) - {
      withData(
        listOf(
          "issue_3248"
//          "3248-regression",
//          "3248-regression-ref-lvl0",
//          "3248-regression-ref-lvl1",
//          "3248-regression-dates"
        )
      ) { filename ->
        val openapiFile = openApiFiles30 / "$filename.yaml"
        generateOpenApi(openapiFile = openapiFile, to = testOut, generator = springGen(testOut))

        val res = compile(File(testOut).absoluteFile.recursiveKtFiles)

        withClue("From ${openapiFile.absPath}\n${res.messages}") { res.exitCode shouldBeEqualComparingTo OK }
      }
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

    // used to manually call individual tests
    "regression".config(false) - {
      withData(
        listOf(
          "issue_3248",
//          "3134-regression",
//          "3248-regression",
//          "3248-regression-required",
//          "3248-regression-required-no-default",
//          "3248-regression-ref-lvl0",
//          "3248-regression-ref-lvl1",
//          "3248-regression-dates"
        )

      ) { filename ->
        val openapiFile = openApiFiles30 / "$filename.yaml"
        generateOpenApi(openapiFile = openapiFile, to = testOut)

        val res = compile(File(testOut).absoluteFile.recursiveKtFiles)

        withClue("From ${File(openapiFile).absoluteFile}\n${res.messages}") { res.exitCode shouldBeEqualComparingTo OK }
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

  private fun CodegenConfig.addUnsigneds(): CodegenConfig = run {
    this.importMapping().put("UnsignedInteger", "java.lang.Long") // map to Long
    this.importMapping().put("UnsignedLong", "java.lang.Long")
    this
  }

  private fun disableTestFilter(filePath: String): Boolean = run {
    false
  }


  private fun modCodegenConfig(c: CodegenConfig, filePath: String) {
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
      openApiFiles30 / "r" / "petstore.yaml",
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
    }
  }
}
