package org.openapitools.codegen.kotlin;

import static org.openapitools.codegen.CodegenConstants.ENUM_PROPERTY_NAMING_TYPE.UPPERCASE;
import static org.openapitools.codegen.languages.KotlinAzureFunctionAppServerCodegen.EXTENSION_MODEL_PROPERTY_KEY;

import io.swagger.v3.oas.models.OpenAPI;
import java.io.File;

import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.CodegenConstants;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.TestUtils;
import org.openapitools.codegen.languages.KotlinAzureFunctionAppServerCodegen;
import org.testng.annotations.Test;

public class KotlinAzureFunctionAppGenTest {

  private String extensionModelInputFile;

  @Test(description = "test embedded enum array")
  public void embeddedEnumArrayTest() throws Exception {
    // paths
    String testProject = "../../samples/server/petstore/kotlin-azure-function-app/";
    extensionModelInputFile = new File(testProject + "model/kotlinAzureExtensionModel.yaml").getPath();
    OpenAPI openAPI =
      TestUtils.parseFlattenSpec(this.getClass().getResource("/3_0/petstore.yaml").getPath());
    File output = new File(testProject + "target/generated-sources/openapi/");

    KotlinAzureFunctionAppServerCodegen codegen = new KotlinAzureFunctionAppServerCodegen();
    codegen.setEnumPropertyNaming(UPPERCASE.name());
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
