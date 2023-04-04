package testutil

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.extensions.Extension
import io.kotest.extensions.junitxml.JunitXmlReporter

class KotestConfig: AbstractProjectConfig() {
  override fun extensions(): List<Extension> = listOf(
    JunitXmlReporter(
      includeContainers = false,
      useTestPathAsName = true,
      // filter failed tests: `xidel --xpath "//testcase[*]/@name" TEST-de.holisticon.CompilationTest.xml  | sort`
      outputDir = "../target/junit-xml"
    )
  )
}
