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

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.core.io.support.SpringFactoriesLoader;

/**
 * {@link ConfigDataLocationResolver} to resolve {@code env:} locations.
 *
 * @author Moritz Halbritter
 */
class EnvConfigDataLocationResolver implements ConfigDataLocationResolver<EnvConfigDataResource> {

	private static final String PREFIX = "env:";

	private static final Pattern EXTENSION_HINT_PATTERN = Pattern.compile("^(.*)\\[(\\.\\w+)](?!\\[)$");

	private static final String DEFAULT_EXTENSION = ".properties";

	private final List<PropertySourceLoader> loaders;

	private final Function<String, String> readEnvVariable;

	EnvConfigDataLocationResolver() {
		this.loaders = SpringFactoriesLoader.loadFactories(PropertySourceLoader.class, getClass().getClassLoader());
		this.readEnvVariable = System::getenv;
	}

	EnvConfigDataLocationResolver(List<PropertySourceLoader> loaders, Function<String, String> readEnvVariable) {
		this.loaders = loaders;
		this.readEnvVariable = readEnvVariable;
	}

	@Override
	public boolean isResolvable(ConfigDataLocationResolverContext context, ConfigDataLocation location) {
		return location.hasPrefix(PREFIX);
	}

	@Override
	public List<EnvConfigDataResource> resolve(ConfigDataLocationResolverContext context, ConfigDataLocation location)
			throws ConfigDataLocationNotFoundException, ConfigDataResourceNotFoundException {
		String value = location.getNonPrefixedValue(PREFIX);
		Matcher matcher = EXTENSION_HINT_PATTERN.matcher(value);
		String extension = getExtension(matcher);
		String variableName = getVariableName(matcher, value);
		PropertySourceLoader loader = getLoader(extension);
		if (hasEnvVariable(variableName)) {
			return List.of(new EnvConfigDataResource(location, variableName, loader));
		}
		if (location.isOptional()) {
			return Collections.emptyList();
		}
		throw new ConfigDataLocationNotFoundException(location,
				"Environment variable '%s' is not set".formatted(variableName), null);
	}

	private PropertySourceLoader getLoader(String extension) {
		if (extension == null) {
			extension = DEFAULT_EXTENSION;
		}
		if (extension.startsWith(".")) {
			extension = extension.substring(1);
		}
		for (PropertySourceLoader loader : this.loaders) {
			for (String supportedExtension : loader.getFileExtensions()) {
				if (supportedExtension.equalsIgnoreCase(extension)) {
					return loader;
				}
			}
		}
		throw new IllegalStateException(
				"File extension '%s' is not known to any PropertySourceLoader".formatted(extension));
	}

	private boolean hasEnvVariable(String variableName) {
		return this.readEnvVariable.apply(variableName) != null;
	}

	private String getVariableName(Matcher matcher, String value) {
		if (matcher.matches()) {
			return matcher.group(1);
		}
		return value;
	}

	private String getExtension(Matcher matcher) {
		if (matcher.matches()) {
			return matcher.group(2);
		}
		return null;
	}

}
