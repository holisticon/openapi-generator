package org.openapitools.codegen.languages;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.samskivert.mustache.Mustache;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.text.CaseUtils;
import org.openapitools.codegen.*;

import java.io.File;
import java.util.*;

import org.openapitools.codegen.meta.features.*;
import org.openapitools.codegen.utils.ModelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KotlinAzureFunctionAppServerCodegen extends AbstractKotlinCodegen {

 /*   @Override
    public CodegenProperty fromProperty(String name, Schema p, boolean required, boolean schemaIsFromAdditionalProperties) {
        Schema ref;
        if (p.get$ref() != null && ModelUtils.getSimpleRef(p.get$ref()) != null) {
            Schema resolvedRef = ModelUtils.getSchemas(this.openAPI).get(ModelUtils.getSimpleRef(p.get$ref()));
            if (resolvedRef == null) {
                logger.warn("could not resolve ref " + p.get$ref());
                ref = p;
            } else {
                ref = resolvedRef;
            }
        } else {
            ref = p;
        }
        return super.fromProperty(name, ref, required, schemaIsFromAdditionalProperties);
    }*/

    protected Optional<String> azureExtensionsFile = Optional.empty();
    public static final String EXTENSION_MODEL_PROPERTY_KEY = "azureExtensionsFile";
    public static final String MUSTACHE_DEBUG_PROPERTY_KEY = "mustacheDebug";

    public static final String GEN_IMPL_FOR_TESTS = "genImplForTests";

    public static final String PROJECT_NAME = "projectName";
    public static final String AZURE_EXTENSIONS_KEY = "x-azure-additional-properties";

    private final Logger logger = LoggerFactory.getLogger(KotlinAzureFunctionAppServerCodegen.class);
    public static final String INTERFACE_TEMPLATE_NAME = "apiInterface.mustache";

    public KotlinAzureFunctionAppServerCodegen() {
        super();
        // currently json deserialization of azure functions is strict about the enum name.
        CodegenConstants.ENUM_PROPERTY_NAMING_TYPE defaultEnumNaming = CodegenConstants.ENUM_PROPERTY_NAMING_TYPE.original;
        cliOptions.stream().filter(x -> CodegenConstants.ENUM_PROPERTY_NAMING.equals(x.getOpt())).findFirst().map(p -> p.defaultValue(
                defaultEnumNaming.name()
        ));
        super.setEnumPropertyNaming(defaultEnumNaming.name());

        modifyFeatureSet(features -> features
                .excludeSecurityFeatures(
                        SecurityFeature.BasicAuth,
                        SecurityFeature.ApiKey,
                        SecurityFeature.OpenIDConnect,
                        SecurityFeature.BearerToken,
                        SecurityFeature.OAuth2_Implicit,
                        SecurityFeature.OAuth2_Password,
                        SecurityFeature.OAuth2_ClientCredentials,
                        SecurityFeature.OAuth2_AuthorizationCode
                )
                .excludeGlobalFeatures(
                        GlobalFeature.XMLStructureDefinitions,
                        GlobalFeature.Callbacks,
                        GlobalFeature.LinkObjects,
                        GlobalFeature.ParameterStyling
                )
                .excludeParameterFeatures(
                        ParameterFeature.Cookie,
                        ParameterFeature.FormUnencoded,
                        ParameterFeature.FormMultipart
                )
        );

        outputFolder = "generated-code" + File.separator + "kotlin-azure-function-app";
        modelTemplateFiles.put("model.mustache", ".kt");
        apiTemplateFiles.put("api.mustache", ".kt");
        apiTemplateFiles.put(INTERFACE_TEMPLATE_NAME, ".kt");
        embeddedTemplateDir = templateDir = "kotlin-azure-function-app";
        apiPackage = "apis";
        modelPackage = "models";
        apiSuffix = "AzureFunction";

        cliOptions.add(new CliOption(EXTENSION_MODEL_PROPERTY_KEY, "path to file with azure functions extensions"));
        cliOptions.add(new CliOption(MUSTACHE_DEBUG_PROPERTY_KEY, "debug mustache templates"));
        cliOptions.add(new CliOption(GEN_IMPL_FOR_TESTS, "generate 'custom' implementation for interface. Used in test "));

    }


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

    private void addCustomMustacheLambdas() {
        final Mustache.Lambda upperFirstLetter = (fragment, writer) -> {
            String res = fragment.execute();
            writer.write(String.valueOf(res.charAt(0)).toUpperCase(Locale.ROOT) + res.substring(1));
        };
        final Mustache.Lambda removeApiSuffix =
                (fragment, writer) -> writer.write(fragment.execute().replaceAll(apiSuffix, ""));
        final Mustache.Lambda formatPath =
                (fragment, writer) -> writer.write(fragment.execute().replaceFirst("^/", ""));
        final Mustache.Lambda removeEmptyLines =
                (fragment, writer) -> writer.write(fragment.execute().replaceAll("(?m)^[ \t]*\r?\n+", "\n"));
        final Mustache.Lambda trimLambda =
                (fragment, writer) -> writer.write(fragment.execute().trim());

        class ContentTypeMapContext {
            public final String key;
            public final Object value;

            public final Integer size;

            public ContentTypeMapContext(String key, Object value, Integer size) {
                this.key = key;
                this.value = value;
                this.size = size;
            }

            public boolean hasContentType() {
                return null != key;
            }

            public boolean isUnique() {
                return size <= 1;
            }

            public String contentTypeShortName() {
                int shortNameIndex = key.lastIndexOf("/");
                String shortName = key.substring(shortNameIndex + 1);
                return CaseUtils.toCamelCase(shortName, true, '-', '_');
            }
        }
        final Mustache.Lambda contentTypeMap =
                (fragment, writer) -> {
                    if (fragment.context() instanceof Map) {
                        Map<?, ?> map = (Map<?, ?>) fragment.context();
                        map.forEach((key, value) ->
                                fragment.execute(new ContentTypeMapContext(key.toString(), value, map.size()), writer));
                        if (map.entrySet().isEmpty()) {
                            fragment.execute(new ContentTypeMapContext(null, null, 0), writer);
                        }
                    } else {
                        throw new IllegalStateException("Wrong usage of custom lambda ContentTypeMap");
                    }
                };

        final Mustache.Lambda debugLambda = (fragment, writer) -> {
            if (mustacheDebug) writer.write("/*" + fragment.execute().trim() + "*/");
        };

        final Mustache.Lambda genInterfaceImplLambda = (fragment, writer) -> {
            if (genInterfaceImpl) writer.write(fragment.execute());
        };

        final Mustache.Lambda dumpCtxLambda = (fragment, writer) -> {
            if (mustacheDebug) writer.write("/*\n CTX: \n" + fragment.context().toString() + "\n*/");
        };

        additionalProperties.put("removeApiSuffix", removeApiSuffix);
        additionalProperties.put("formatPath", formatPath);
        additionalProperties.put("upperFirstLetter", upperFirstLetter);
        additionalProperties.put("removeEmptyLines", removeEmptyLines);
        additionalProperties.put("ContentTypeMap", contentTypeMap);
        additionalProperties.put("trim", trimLambda);
        additionalProperties.put("debug", debugLambda);
        additionalProperties.put("dump", dumpCtxLambda);
        additionalProperties.put("genInterfaceImpl", genInterfaceImplLambda);
    }

    boolean mustacheDebug = false;
    boolean genInterfaceImpl = false;

    private void processExtensionModel() {
        if (additionalProperties.containsKey(EXTENSION_MODEL_PROPERTY_KEY)) {
            this.setAzureExtensionsFile((String) additionalProperties.get(EXTENSION_MODEL_PROPERTY_KEY));
        }
        if (additionalProperties.containsKey(MUSTACHE_DEBUG_PROPERTY_KEY)) {
            switch (additionalProperties.get(MUSTACHE_DEBUG_PROPERTY_KEY).toString().toLowerCase(Locale.ROOT)) {
                case "1":
                case "true":
                case "t":
                    mustacheDebug = true;
                    break;
                default:
                    logger.warn(MUSTACHE_DEBUG_PROPERTY_KEY + " set to unknown value " + additionalProperties.get(MUSTACHE_DEBUG_PROPERTY_KEY));
                    break;
            }
        }
        if (additionalProperties.containsKey(GEN_IMPL_FOR_TESTS)) {
            switch (additionalProperties.get(GEN_IMPL_FOR_TESTS).toString().toLowerCase(Locale.ROOT)) {
                case "1":
                case "true":
                case "t":
                    genInterfaceImpl = true;
                    break;
                default:
                    logger.warn(GEN_IMPL_FOR_TESTS + " set to unknown value " + additionalProperties.get(GEN_IMPL_FOR_TESTS));
                    break;
            }
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
                List<AzureParameterExtension> extensions = (List<AzureParameterExtension>) Optional.ofNullable(op.getExtensions()).orElse(Collections.emptyMap()).getOrDefault(AZURE_EXTENSIONS_KEY, Lists.newArrayList());
                extensions.add(cp);
                op.addExtension(AZURE_EXTENSIONS_KEY, extensions);
            }
        }
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
