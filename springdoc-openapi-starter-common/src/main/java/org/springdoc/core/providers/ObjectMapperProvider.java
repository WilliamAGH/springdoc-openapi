/*
 *
 *  *
 *  *  *
 *  *  *  *
 *  *  *  *  *
 *  *  *  *  *  * Copyright 2019-2025 the original author or authors.
 *  *  *  *  *  *
 *  *  *  *  *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  *  *  *  *  * you may not use this file except in compliance with the License.
 *  *  *  *  *  * You may obtain a copy of the License at
 *  *  *  *  *  *
 *  *  *  *  *  *      https://www.apache.org/licenses/LICENSE-2.0
 *  *  *  *  *  *
 *  *  *  *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  *  *  *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  *  *  *  *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  *  *  *  * See the License for the specific language governing permissions and
 *  *  *  *  *  * limitations under the License.
 *  *  *  *  *
 *  *  *  *
 *  *  *
 *  *
 *
 */

package org.springdoc.core.providers;

import java.io.IOException;
import java.util.List;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Json31;
import io.swagger.v3.core.util.ObjectMapperFactory;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.core.util.Yaml31;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.servers.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import org.springdoc.core.mixins.SortedOpenAPIMixin;
import org.springdoc.core.mixins.SortedOpenAPIMixin31;
import org.springdoc.core.mixins.SortedSchemaMixin;
import org.springdoc.core.mixins.SortedSchemaMixin31;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.properties.SpringDocConfigProperties.ApiDocs.OpenApiVersion;

/**
 * The type Spring doc object mapper provider.
 * Provides Jackson version-agnostic methods for JSON operations.
 */
public class ObjectMapperProvider extends ObjectMapperFactory {

	/**
	 * The constant LOGGER.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(ObjectMapperProvider.class);

	/**
	 * The Json mapper.
	 */
	private final ObjectMapper jsonMapper;

	/**
	 * The Yaml mapper.
	 */
	private final ObjectMapper yamlMapper;

	/**
	 * The Spring doc config properties.
	 */
	private final SpringDocConfigProperties springDocConfigProperties;

	/**
	 * Instantiates a new Spring doc object mapper.
	 *
	 * @param springDocConfigProperties the spring doc config properties
	 */
	public ObjectMapperProvider(SpringDocConfigProperties springDocConfigProperties) {
		this.springDocConfigProperties = springDocConfigProperties;
		OpenApiVersion openApiVersion = springDocConfigProperties.getApiDocs().getVersion();
		if (openApiVersion == OpenApiVersion.OPENAPI_3_1) {
			jsonMapper = Json31.mapper();
			yamlMapper = Yaml31.mapper();
			if (springDocConfigProperties.isUseArbitrarySchemas()) {
				System.setProperty(Schema.USE_ARBITRARY_SCHEMA_PROPERTY, "true");
			}
			if (springDocConfigProperties.isExplicitObjectSchema()) {
				System.setProperty(Schema.EXPLICIT_OBJECT_SCHEMA_PROPERTY, "true");
			}
		}
		else {
			jsonMapper = Json.mapper();
			yamlMapper = Yaml.mapper();
		}
	}

	/**
	 * Create json object mapper.
	 *
	 * @param springDocConfigProperties the spring doc config properties
	 * @return the object mapper
	 */
	public static ObjectMapper createJson(SpringDocConfigProperties springDocConfigProperties) {
		OpenApiVersion openApiVersion = springDocConfigProperties.getApiDocs().getVersion();
		ObjectMapper objectMapper;
		if (openApiVersion == OpenApiVersion.OPENAPI_3_1)
			objectMapper = ObjectMapperFactory.createJson31();
		else
			objectMapper = ObjectMapperFactory.createJson();

		if (springDocConfigProperties.isWriterWithOrderByKeys())
			sortOutput(objectMapper, springDocConfigProperties);

		return objectMapper;
	}

	/**
	 * Sort output.
	 *
	 * @param objectMapper              the object mapper
	 * @param springDocConfigProperties the spring doc config properties
	 */
	public static void sortOutput(ObjectMapper objectMapper, SpringDocConfigProperties springDocConfigProperties) {
		objectMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
		objectMapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
		if (OpenApiVersion.OPENAPI_3_1 == springDocConfigProperties.getApiDocs().getVersion()) {
			objectMapper.addMixIn(OpenAPI.class, SortedOpenAPIMixin31.class);
			objectMapper.addMixIn(Schema.class, SortedSchemaMixin31.class);
		}
		else {
			objectMapper.addMixIn(OpenAPI.class, SortedOpenAPIMixin.class);
			objectMapper.addMixIn(Schema.class, SortedSchemaMixin.class);
		}
	}

	/**
	 * Mapper object mapper.
	 *
	 * @return the object mapper
	 */
	public ObjectMapper jsonMapper() {
		return jsonMapper;
	}

	/**
	 * Yaml mapper object mapper.
	 *
	 * @return the object mapper
	 */
	public ObjectMapper yamlMapper() {
		return yamlMapper;
	}

	/**
	 * Is openapi 31 boolean.
	 *
	 * @return the boolean
	 */
	public boolean isOpenapi31() {
		return springDocConfigProperties.isOpenapi31();
	}

	/**
	 * Clone a Schema via JSON serialization/deserialization.
	 * This method abstracts Jackson version-specific TypeReference usage.
	 *
	 * @param source the source schema to clone
	 * @return the cloned schema, or the source if cloning fails
	 */
	public Schema<?> cloneSchema(Schema<?> source) {
		if (source == null) return null;
		try {
			JavaType schemaType = jsonMapper.constructType(Schema.class);
			return jsonMapper.readValue(jsonMapper.writeValueAsBytes(source), schemaType);
		}
		catch (IOException e) {
			LOGGER.warn("Json Processing Exception occurred while cloning Schema: {}", e.getMessage());
			return source;
		}
	}

	/**
	 * Clone a List of Servers via JSON serialization/deserialization.
	 * This method abstracts Jackson version-specific TypeReference usage.
	 *
	 * @param source the source list to clone
	 * @return the cloned list, or the source if cloning fails
	 */
	public List<Server> cloneServers(List<Server> source) {
		if (source == null) return null;
		try {
			JavaType listType = jsonMapper.getTypeFactory().constructCollectionType(List.class, Server.class);
			return jsonMapper.readValue(jsonMapper.writeValueAsBytes(source), listType);
		}
		catch (IOException e) {
			LOGGER.warn("Json Processing Exception occurred while cloning Servers: {}", e.getMessage());
			return source;
		}
	}
}
