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
import java.io.File

class CompilationTest : FreeSpec() {
  init {
    val openApiFiles = ".." / "openapi-generator" / "src" / "test" / "resources" / "3_0"

    "regression" - {
      withData(
        "3134-regression",
        "3248-regression",
        "3248-regression-dates"
      ) { filename ->
        val openapiFile = openApiFiles / "$filename.yaml"
        generateOpenApi(openapiFile = openapiFile, to = testOut)

        val res = compile(File(testOut).absoluteFile.recursiveKtFiles, defaultApi())

        withClue("From ${File(openapiFile).absoluteFile}\n${res.messages}") { res.exitCode shouldBeEqualComparingTo OK }
      }
    }
  }

  fun defaultApi(): String = """
        package apis
        import apis.DefaultAzureFunctionInterface
        class DefaultApi : DefaultAzureFunctionInterface {}"""
}
