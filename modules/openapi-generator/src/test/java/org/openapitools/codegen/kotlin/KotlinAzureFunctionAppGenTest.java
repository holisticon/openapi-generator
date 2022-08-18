package org.openapitools.codegen.kotlin;

import static org.openapitools.codegen.CodegenConstants.ENUM_PROPERTY_NAMING_TYPE.*;
import static org.openapitools.codegen.languages.KotlinAzureFunctionAppServerCodegen.EXTENSION_MODEL_PROPERTY_KEY;

import io.swagger.v3.oas.models.OpenAPI;
import java.io.File;

import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.CodegenConstants;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.TestUtils;
import org.openapitools.codegen.languages.AbstractKotlinCodegen;
import org.openapitools.codegen.languages.KotlinAzureFunctionAppServerCodegen;
import org.openapitools.codegen.languages.KotlinSpringServerCodegen;
import org.testng.annotations.Test;

public class KotlinAzureFunctionAppGenTest {

  private String extensionModelInputFile;

  @Test(description = "test embedded enum array")
  public void embeddedEnumArrayTest() throws Exception {
    String openapiFile =
//      "/3_0/3248-regression-dates.yaml";
      "/3_0/petstore-id-as-string.yaml";
//      "/3_0/oneOf.yaml";

    // paths
    String testProject = "../../samples/server/petstore/kotlin-azure-function-app/";
    extensionModelInputFile = new File(testProject + "model/kotlinAzureExtensionModel.yaml").getPath();
    OpenAPI openAPI =
      TestUtils.parseFlattenSpec(openapiFile/*this.getClass().getResource(openapiFile).getPath()*/);
    File output = new File(testProject + "target/generated-sources/openapi/");

    AbstractKotlinCodegen codegen = new
      KotlinAzureFunctionAppServerCodegen();
    codegen.setOutputDir(output.getAbsolutePath());
    String projectId = "org.openapitools.";
    codegen.setApiPackage(projectId + "api");
    codegen.setModelPackage(projectId + "api.model");
    codegen.additionalProperties().put(CodegenConstants.SOURCE_FOLDER, "src/gen/kotlin");

//    codegen.additionalProperties().put(CodegenConstants.MODEL_PACKAGE, baseModelPackage + ".yyyy.model.xxxx");
    codegen.additionalProperties().put(EXTENSION_MODEL_PROPERTY_KEY, extensionModelInputFile);





    ClientOptInput input = new ClientOptInput();
    input.openAPI(openAPI);
    input.config(codegen);
    DefaultGenerator generator = new DefaultGenerator();
    generator.opts(input).generate();

  }
}
