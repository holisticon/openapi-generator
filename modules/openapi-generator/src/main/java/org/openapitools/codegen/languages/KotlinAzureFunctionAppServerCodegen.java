package org.openapitools.codegen.languages;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import java.io.IOException;
import java.io.Writer;
import org.apache.commons.io.FilenameUtils;
import org.openapitools.codegen.*;

import java.io.File;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KotlinAzureFunctionAppServerCodegen extends AbstractKotlinCodegen {


  protected Optional<String> azureExtensionsFile = Optional.empty();
  public static final String EXTENSION_MODEL_PROPERTY_KEY = "azureExtensionsFile";

  public static final String PROJECT_NAME = "projectName";
  public static final String AZURE_EXTENSIONS_KEY = "x-azure-additional-properties";

  private final Logger logger = LoggerFactory.getLogger(KotlinAzureFunctionAppServerCodegen.class);
  public static final String INTERFACE_TEMPLATE_NAME = "apiInterface.mustache";


  public CodegenType getTag() {
    return CodegenType.SERVER;
  }

  public String getName() {
    return "kotlin-azure-function-app";
  }

  public String getHelp() {
    return "Generates a kotlin-azure-function-app server.";
  }

  @Override
  public void processOpts() {
    super.processOpts();
    addCustomMustacheLambdas();
    processExtensionModel();
  }

  private void addCustomMustacheLambdas(){
    final Mustache.Lambda upperFirstLetter = (fragment, writer) -> {
      String res = fragment.execute();
      writer.write(String.valueOf(res.charAt(0)).toUpperCase(Locale.ROOT) + res.substring(1));
    };
    final Mustache.Lambda removeApiSuffix =
      (fragment, writer) -> writer.write(fragment.execute().replaceAll(apiSuffix, ""));
    final Mustache.Lambda formatPath =
      (fragment, writer) -> writer.write(fragment.execute().replaceFirst("^/", ""));
    final Mustache.Lambda removeEmptyLines =
      (fragment, writer) -> writer.write(fragment.execute().replaceAll("(?m)^[ \t]*\r?\n", ""));


    additionalProperties.put("removeApiSuffix", removeApiSuffix);
    additionalProperties.put("formatPath", formatPath);
    additionalProperties.put("upperFirstLetter", upperFirstLetter);
    additionalProperties.put("removeEmptyLines", removeEmptyLines);
  }

  private void processExtensionModel() {
    if (additionalProperties.containsKey(EXTENSION_MODEL_PROPERTY_KEY)) {
      this.setAzureExtensionsFile((String) additionalProperties.get(EXTENSION_MODEL_PROPERTY_KEY));
    }

    final Optional<String> extensionModelProperty = Optional.ofNullable(this.additionalProperties.get(
      EXTENSION_MODEL_PROPERTY_KEY)).map(Object::toString);

    extensionModelProperty.ifPresent(azureAdditionalPropertiesFilePath -> {
      try {
        final ObjectMapper mapper;
        if (FilenameUtils.isExtension(azureAdditionalPropertiesFilePath.toLowerCase(Locale.ROOT), "yml", "yaml")) {
          mapper = Yaml.mapper().copy();
        } else {
          mapper = Json.mapper().copy();
        }
        mapper.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);


        File azureAdditionalPropertiesConfigFile = new File(azureAdditionalPropertiesFilePath);
        final List<AzureOperationExtension> azureOperationExtensions =
          mapper.readValue(azureAdditionalPropertiesConfigFile, new TypeReference<List<AzureOperationExtension>>() {
          });

        azureOperationExtensions.forEach(this::mutateExtensionModel);
        processFileExtension(azureOperationExtensions);
      } catch (Exception e) {
        logger.error("Error while processing azure additional properties config file ", e);
        throw new RuntimeException(e);
      }
    });
  }

  // wrap strings in ""
  private AzureOperationExtension mutateExtensionModel(AzureOperationExtension methodExtensions) {
    methodExtensions.extensions.forEach(azureParameterExtension ->
      azureParameterExtension.annotation.parameters.forEach((key, value) -> {
        //use string values as string
        if (value instanceof String) {
          azureParameterExtension.annotation.parameters.put(key, "\"" + value + "\"");
        }
      })
    );
    return methodExtensions;
  }

  private void processFileExtension(List<AzureOperationExtension> ext) {
    final Paths paths = this.openAPI.getPaths();
    for (AzureOperationExtension c : ext) {
      if (!paths.containsKey(c.path)) {
        logger.warn("===> Path " + c.path + " not found");
//        throw new IllegalStateException("Missing path in openAPI: " + c.path);
        continue;
      }
      final PathItem p = paths.get(c.path);
      final Operation op;
      switch (c.method) {
        case POST:
          op = p.getPost();
          break;
        case GET:
          op = p.getGet();
          break;
        case PUT:
          op = p.getPut();
          break;
        case PATCH:
          op = p.getPatch();
          break;
        case DELETE:
          op = p.getDelete();
          break;
        case HEAD:
          op = p.getHead();
          break;
        case OPTIONS:
          op = p.getOptions();
          break;
        default:
          throw new IllegalStateException("non exhaustive");
      }
      if (op == null) {
        logger.warn("Path " + c.path + " found, but no route for " + c.method);
        continue;
      }
      for (AzureParameterExtension cp : c.extensions) {
        op.addExtension(AZURE_EXTENSIONS_KEY, cp);
      }
    }
  }

  public KotlinAzureFunctionAppServerCodegen() {
    super();

    outputFolder = "generated-code" + File.separator + "kotlin-azure-function-app";
    modelTemplateFiles.put("model.mustache", ".kt");
    apiTemplateFiles.put("api.mustache", ".kt");
    apiTemplateFiles.put(INTERFACE_TEMPLATE_NAME, ".kt");
    embeddedTemplateDir = templateDir = "kotlin-azure-function-app";
    apiPackage = "apis";
    modelPackage = "models";
    apiSuffix = "AzureFunction";

    cliOptions.add(new CliOption(EXTENSION_MODEL_PROPERTY_KEY, "path to file with azure functions extensions"));

  }

  @Override
  public Mustache.Compiler processCompiler(Mustache.Compiler compiler) {
    return super.processCompiler(compiler).escapeHTML(false);
  }

  @Override
  public String apiFilename(String templateName, String tag) {
    if (templateName.equals(INTERFACE_TEMPLATE_NAME)) {
      String suffix = apiTemplateFiles().get(templateName);
      return apiFileFolder() + File.separator + toApiFilename(tag) + "Interface" + suffix;
    }
    return super.apiFilename(templateName, tag);
  }

  public void setAzureExtensionsFile(String azureExtensionsFile) {
      this.azureExtensionsFile = Optional.ofNullable(azureExtensionsFile);
  }

  @Override
  @SuppressWarnings("static-method")
  public void postProcess() {
    System.out.println("################################################################################");
    System.out.println("# lala                                          #");
    System.out.println("# Please consider donation to help us maintain this project \uD83D\uDE4F                 #");
    System.out.println("# https://opencollective.com/openapi_generator/donate                          #");
    System.out.println("################################################################################");
  }
}
