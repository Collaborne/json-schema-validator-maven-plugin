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

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.JacksonUtils;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ListProcessingReport;
import com.github.fge.jsonschema.core.report.LogLevel;
import com.google.common.collect.Iterators;

public class ValidateMojoTest {
	private final ObjectMapper objectMapper = JacksonUtils.newMapper();

	@Test
	public void validateNoSchemaNoRequiredReturnsReportWithWarning() throws ProcessingException {
		ValidateMojo validateMojo = new ValidateMojo();
		validateMojo.setRequireSchema(false);
		ListProcessingReport report = validateMojo.validate(objectMapper.createObjectNode(), null);
		assertTrue(report.isSuccess());
		assertEquals(LogLevel.WARNING, Iterators.getOnlyElement(report.iterator()).getLogLevel());
	}

	@Test
	public void validateNoSchemaRequiredReturnsReportWithError() throws ProcessingException {
		ValidateMojo validateMojo = new ValidateMojo();
		validateMojo.setRequireSchema(true);
		ListProcessingReport report = validateMojo.validate(objectMapper.createObjectNode(), null);
		assertFalse(report.isSuccess());
		assertEquals(LogLevel.ERROR, Iterators.getOnlyElement(report.iterator()).getLogLevel());
	}
}
