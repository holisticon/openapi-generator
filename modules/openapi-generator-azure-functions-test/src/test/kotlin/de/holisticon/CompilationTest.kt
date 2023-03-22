package de.holisticon

import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.OK
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
import org.openapitools.codegen.languages.KotlinSpringServerCodegen
import java.io.File

class CompilationTest : FreeSpec() {
  fun springGen(to: String?) = KotlinSpringServerCodegen().also { cfg ->
    to?.let { cfg.outputDir = it }
  }

  init {
    val openApiFiles = ".." / "openapi-generator" / "src" / "test" / "resources"
    val openApiFiles30 = openApiFiles / "3_0"
    val openApiFiles31 = openApiFiles / "3_1"

    "focus" {
      val openapiFile = openApiFiles30 / "aspnetcore" / "petstore.yaml"
      println("From: ${File(openapiFile).absoluteFile}")
      generateOpenApi(openapiFile = openapiFile, to = "target" / "generated-sources2")

      val res = compile(File(testOut).absoluteFile.recursiveKtFiles)

      withClue("From ${File(openapiFile).absoluteFile}\n${res.messages}") { res.exitCode shouldBeEqualComparingTo OK }

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

        withClue("From ${File(openapiFile).absoluteFile}\n${res.messages}") { res.exitCode shouldBeEqualComparingTo OK }
      }
    }


    "regression 3.0" - { // 77 F : 119 P
      testTraverse(File(openApiFiles30)) { file ->
        generateOpenApi(openapiFile = file.absolutePath, to = testOut)
        val res = compile(File(testOut).absoluteFile.recursiveKtFiles)
        withClue("From ${file.absoluteFile}\n${res.messages}") { res.exitCode shouldBeEqualComparingTo OK }
      }
    }
    "regression 3.1" - { // 2 F
      testTraverse(File(openApiFiles31)) { file ->
        generateOpenApi(openapiFile = file.absolutePath, to = testOut)
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
      registerTest(TestName(f.name), false, null) {
        this.testFun(f)
      }
    }
  }
}
