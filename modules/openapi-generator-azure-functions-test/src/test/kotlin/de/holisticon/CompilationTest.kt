package de.holisticon

import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.OK
import de.holisticon.CompilationTestHelper.compile
import de.holisticon.CompilationTestHelper.div
import de.holisticon.CompilationTestHelper.generateOpenApi
import de.holisticon.CompilationTestHelper.recursiveKtFiles
import de.holisticon.CompilationTestHelper.testOut
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import org.openapitools.codegen.languages.KotlinAzureFunctionAppServerCodegen
import org.openapitools.codegen.languages.KotlinSpringServerCodegen
import java.io.File

class CompilationTest : FreeSpec() {
  fun springGen(to: String?) = KotlinSpringServerCodegen().also { cfg ->
    to?.let { cfg.outputDir = it }
  }

  init {
    val openApiFiles = ".." / "openapi-generator" / "src" / "test" / "resources" / "3_0"

    "regression spring" - {
      withData(
        listOf(
          "3134-regression",
          "3248-regression",
          "3248-regression-ref-lvl0",
          "3248-regression-ref-lvl1",
          "3248-regression-dates"
        )

      ) { filename ->
        val openapiFile = openApiFiles / "$filename.yaml"
        generateOpenApi(openapiFile = openapiFile, to = testOut, generator = springGen(testOut))

        val res = compile(File(testOut).absoluteFile.recursiveKtFiles)

        withClue("From ${File(openapiFile).absoluteFile}\n${res.messages}") { res.exitCode shouldBeEqualComparingTo OK }
      }
    }


    "f: regression" - {
      withData(
        listOf(
//          "3134-regression",
          "3248-regression",
//          "3248-regression-ref-lvl0",
//          "3248-regression-ref-lvl1",
//          "3248-regression-dates"
        )

      ) { filename ->
        val openapiFile = openApiFiles / "$filename.yaml"
        generateOpenApi(openapiFile = openapiFile, to = testOut)

        val res = compile(File(testOut).absoluteFile.recursiveKtFiles)

        withClue("From ${File(openapiFile).absoluteFile}\n${res.messages}") { res.exitCode shouldBeEqualComparingTo OK }
      }
    }
  }

  fun defaultApi(): String = """
        package apis
        import apis.DefaultAzureFunctionInterface
        class DefaultApi : DefaultAzureFunctionInterface {}"""
}
