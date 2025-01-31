/*
 * Copyright 2012-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.config;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.core.env.PropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link EnvConfigDataLoader}.
 *
 * @author Moritz Halbritter
 */
class EnvConfigDataLoaderTests {

	private ConfigDataLoaderContext context;

	private Map<String, String> envVariables;

	private EnvConfigDataLoader loader;

	@BeforeEach
	void setUp() {
		this.context = mock(ConfigDataLoaderContext.class);
		this.envVariables = new HashMap<>();
		this.loader = new EnvConfigDataLoader(this.envVariables::get);
	}

	@Test
	void shouldLoadFromVariable() throws IOException {
		this.envVariables.put("VAR1", "key1=value1");
		ConfigData data = this.loader.load(this.context, createResource("VAR1"));
		assertThat(data.getPropertySources()).hasSize(1);
		PropertySource<?> propertySource = data.getPropertySources().get(0);
		assertThat(propertySource.getProperty("key1")).isEqualTo("value1");
	}

	@Test
	void shouldFailIfVariableIsNotSet() {
		assertThatExceptionOfType(ConfigDataResourceNotFoundException.class)
			.isThrownBy(() -> this.loader.load(this.context, createResource("VAR1")))
			.withMessage("Config data resource 'env variable [VAR1]' cannot be found");
	}

	private static EnvConfigDataResource createResource(String variableName) {
		return new EnvConfigDataResource(ConfigDataLocation.of("env:" + variableName), variableName,
				new PropertiesPropertySourceLoader());
	}

}
