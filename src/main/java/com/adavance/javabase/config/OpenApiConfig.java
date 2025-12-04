package com.adavance.javabase.config;

import com.adavance.javabase.util.EntityDiscovery;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.servers.Server;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration for OpenAPI/Swagger documentation.
 * Automatically generates schemas for all @Entity classes.
 */
@Configuration
@RequiredArgsConstructor
public class OpenApiConfig {

    private final EntityDiscovery entityDiscovery;

    @Bean
    public OpenAPI customOpenAPI() {
        OpenAPI openAPI = new OpenAPI()
                .info(new Info()
                        .title("Javabase API")
                        .version("1.0.0")
                        .description("API documentation for Javabase application. " +
                                "This API automatically discovers and documents all entity models."))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local development server")
                ));

        // Add fake entity schema
        Schema<?> fakeEntitySchema = new Schema<>()
                .type("object")
                .addProperty("id", new Schema<>().type("integer").format("int64").description("Entity ID"))
                .addProperty("uuid", new Schema<>().type("string").format("uuid").description("Unique identifier"))
                .addProperty("name", new Schema<>().type("string").description("Entity name"))
                .addProperty("description", new Schema<>().type("string").description("Entity description"))
                .addProperty("createdAt", new Schema<>().type("string").format("date-time").description("Creation timestamp"))
                .addProperty("updatedAt", new Schema<>().type("string").format("date-time").description("Last update timestamp"));

        if (openAPI.getComponents() == null) {
            openAPI.setComponents(new io.swagger.v3.oas.models.Components());
        }

        // Add fake entity schema (keeping for backward compatibility)
        openAPI.getComponents().addSchemas("FakeEntity", fakeEntitySchema);

        // Add fake entity paths (keeping for backward compatibility)
        PathItem fakeEntityPath = new PathItem();

        // GET /rest/fake-entity - List all fake entities
        Operation getOperation = new Operation()
                .summary("Get all fake entities")
                .description("Retrieves a list of all fake entities")
                .operationId("getAllFakeEntities")
                .responses(new ApiResponses()
                        .addApiResponse("200", new ApiResponse()
                                .description("Successful response")
                                .content(new Content()
                                        .addMediaType("application/json", new MediaType()
                                                .schema(new Schema<>()
                                                        .type("array")
                                                        .items(new Schema<>().$ref("#/components/schemas/FakeEntity"))))))
                        .addApiResponse("404", new ApiResponse().description("Not found"))
                        .addApiResponse("500", new ApiResponse().description("Internal server error")));
        fakeEntityPath.setGet(getOperation);

        // POST /rest/fake-entity - Create a new fake entity
        Schema<?> createFakeEntitySchema = new Schema<>()
                .type("object")
                .addProperty("name", new Schema<>().type("string").description("Entity name").required(List.of("name")))
                .addProperty("description", new Schema<>().type("string").description("Entity description"))
                .required(List.of("name"));

        Operation postOperation = new Operation()
                .summary("Create a new fake entity")
                .description("Creates a new fake entity with the provided data")
                .operationId("createFakeEntity")
                .requestBody(new RequestBody()
                        .description("Fake entity data")
                        .required(true)
                        .content(new Content()
                                .addMediaType("application/json", new MediaType()
                                        .schema(createFakeEntitySchema))))
                .responses(new ApiResponses()
                        .addApiResponse("201", new ApiResponse()
                                .description("Entity created successfully")
                                .content(new Content()
                                        .addMediaType("application/json", new MediaType()
                                                .schema(new Schema<>().$ref("#/components/schemas/FakeEntity")))))
                        .addApiResponse("400", new ApiResponse().description("Bad request"))
                        .addApiResponse("404", new ApiResponse().description("Not found"))
                        .addApiResponse("500", new ApiResponse().description("Internal server error")));
        fakeEntityPath.setPost(postOperation);

        openAPI.path("/rest/fake-entity", fakeEntityPath);

        // Dynamically generate OpenAPI paths for all discovered entities
        generateEntityPaths(openAPI);

        return openAPI;
    }

    /**
     * Generates OpenAPI paths for all discovered entities.
     * Creates GET (list), GET (by ID), and POST operations for each entity.
     */
    private void generateEntityPaths(OpenAPI openAPI) {
        for (Class<?> entityClass : entityDiscovery.getAllEntityClasses()) {
            String entityName = entityDiscovery.getEntityName(entityClass)
                    .orElseGet(() -> toKebabCase(entityClass.getSimpleName()));
            String schemaName = entityClass.getSimpleName();
            String path = "/rest/" + entityName;

            PathItem pathItem = new PathItem();

            // GET /rest/{entityName} - List all entities
            Operation listOperation = new Operation()
                    .summary("Get all " + entityName + " entities")
                    .description("Retrieves a list of all " + entityName + " entities")
                    .operationId("getAll" + schemaName + "Entities")
                    .responses(new ApiResponses()
                            .addApiResponse("200", new ApiResponse()
                                    .description("Successful response")
                                    .content(new Content()
                                            .addMediaType("application/json", new MediaType()
                                                    .schema(new Schema<>()
                                                            .type("array")
                                                            .items(new Schema<>().$ref("#/components/schemas/" + schemaName))))))
                            .addApiResponse("404", new ApiResponse().description("Entity type not found"))
                            .addApiResponse("500", new ApiResponse().description("Internal server error")));
            pathItem.setGet(listOperation);

            // GET /rest/{entityName}/{id} - Get entity by ID (separate path)
            Operation getByIdOperation = new Operation()
                    .summary("Get " + entityName + " by ID")
                    .description("Retrieves a specific " + entityName + " entity by its ID")
                    .operationId("get" + schemaName + "ById")
                    .addParametersItem(new Parameter()
                            .name("id")
                            .in("path")
                            .required(true)
                            .description("Entity ID")
                            .schema(new Schema<>().type("integer").format("int64")))
                    .responses(new ApiResponses()
                            .addApiResponse("200", new ApiResponse()
                                    .description("Successful response")
                                    .content(new Content()
                                            .addMediaType("application/json", new MediaType()
                                                    .schema(new Schema<>().$ref("#/components/schemas/" + schemaName)))))
                            .addApiResponse("404", new ApiResponse().description("Entity not found"))
                            .addApiResponse("500", new ApiResponse().description("Internal server error")));

            // POST /rest/{entityName} - Create new entity
            Operation createOperation = new Operation()
                    .summary("Create a new " + entityName + " entity")
                    .description("Creates a new " + entityName + " entity with the provided data")
                    .operationId("create" + schemaName + "Entity")
                    .requestBody(new RequestBody()
                            .description(entityName + " entity data")
                            .required(true)
                            .content(new Content()
                                    .addMediaType("application/json", new MediaType()
                                            .schema(new Schema<>().$ref("#/components/schemas/" + schemaName)))))
                    .responses(new ApiResponses()
                            .addApiResponse("201", new ApiResponse()
                                    .description("Entity created successfully")
                                    .content(new Content()
                                            .addMediaType("application/json", new MediaType()
                                                    .schema(new Schema<>().$ref("#/components/schemas/" + schemaName)))))
                            .addApiResponse("400", new ApiResponse().description("Bad request"))
                            .addApiResponse("404", new ApiResponse().description("Entity type not found"))
                            .addApiResponse("500", new ApiResponse().description("Internal server error")));
            pathItem.setPost(createOperation);

            // Add the path with GET (list) and POST operations
            openAPI.path(path, pathItem);

            // Add the GET by ID path separately
            String pathWithId = path + "/{id}";
            PathItem pathItemWithId = new PathItem();
            pathItemWithId.setGet(getByIdOperation);
            openAPI.path(pathWithId, pathItemWithId);
        }
    }

    /**
     * Converts a class name to kebab-case.
     * Example: AddOnLevel -> add-on-level
     */
    private String toKebabCase(String className) {
        return className
                .replaceAll("([a-z])([A-Z])", "$1-$2")
                .toLowerCase();
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .pathsToMatch("/**")
                // Note: /rest/** paths are documented via OpenApiOperationsCustomizer
                // The GenericRestController is hidden with @Hidden annotation
                .build();
    }
}
