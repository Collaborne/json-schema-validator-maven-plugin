/**
 * Copyright (C) 2015 Collaborne B.V. (opensource@collaborne.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.collaborne.jsonschema.validator.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Before;
import org.junit.Test;

import com.collaborne.jsonschema.validator.plugin.ValidateMojo.DirectoryURIMapping;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jackson.JacksonUtils;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.load.URIManager;
import com.github.fge.jsonschema.core.load.uri.URITranslator;
import com.github.fge.jsonschema.core.load.uri.URITranslatorConfiguration;
import com.github.fge.jsonschema.core.load.uri.URITranslatorConfigurationBuilder;
import com.github.fge.jsonschema.core.report.ListProcessingReport;
import com.github.fge.jsonschema.core.report.LogLevel;
import com.google.common.collect.Iterators;

public class ValidateMojoTest {
	private final ObjectMapper objectMapper = JacksonUtils.newMapper();
	private final URITranslator uriTranslator = new URITranslator(URITranslatorConfiguration.byDefault());
	private final URIManager uriManager = new URIManager();

	private ValidateMojo validateMojo;

	@Before
	public void setUp() {
		validateMojo = new ValidateMojo();
	}

	@Test
	public void validateNoSchemaNoRequiredReturnsReportWithWarning() throws ProcessingException {
		validateMojo.setRequireSchema(false);
		ListProcessingReport report = validateMojo.validate(objectMapper.createObjectNode(), uriTranslator, uriManager, null);
		assertTrue(report.isSuccess());
		assertEquals(LogLevel.WARNING, Iterators.getOnlyElement(report.iterator()).getLogLevel());
	}

	@Test
	public void validateNoSchemaRequiredReturnsReportWithError() throws ProcessingException {
		validateMojo.setRequireSchema(true);
		ListProcessingReport report = validateMojo.validate(objectMapper.createObjectNode(), uriTranslator, uriManager, null);
		assertFalse(report.isSuccess());
		assertEquals(LogLevel.ERROR, Iterators.getOnlyElement(report.iterator()).getLogLevel());
	}

	@Test
	public void validateInvalidDollarSchemaUriReturnsReportWithError() throws ProcessingException {
		ObjectNode node = objectMapper.createObjectNode();
		node.put("$schema", ":");

		ListProcessingReport report = validateMojo.validate(node, uriTranslator, uriManager, null);
		assertFalse(report.isSuccess());
		assertEquals(LogLevel.ERROR, Iterators.getOnlyElement(report.iterator()).getLogLevel());
	}

	@Test
	public void addSchemaMappingsAddsMissingSlashInUri() throws MojoExecutionException, IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		DirectoryURIMapping mapping = new DirectoryURIMapping(URI.create("http://example.com"), new File("."));
		validateMojo.setSchemaMappings(new DirectoryURIMapping[] { mapping });

		URITranslatorConfigurationBuilder builder = URITranslatorConfiguration.newBuilder();
		validateMojo.addSchemaMappings(builder);
		URITranslatorConfiguration cfg = builder.freeze();

		// FIXME: should be able to query the configuration
		Field pathRedirectsField = URITranslatorConfiguration.class.getDeclaredField("pathRedirects");
		pathRedirectsField.setAccessible(true);
		Map<URI, URI> pathRedirects = (Map<URI, URI>) pathRedirectsField.get(cfg);
		assertTrue(pathRedirects.containsKey(URI.create("http://example.com/")));
	}
}
