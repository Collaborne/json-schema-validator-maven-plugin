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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.DirectoryScanner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.JacksonUtils;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.load.URIManager;
import com.github.fge.jsonschema.core.load.configuration.LoadingConfiguration;
import com.github.fge.jsonschema.core.load.uri.URITranslatorConfiguration;
import com.github.fge.jsonschema.core.load.uri.URITranslatorConfigurationBuilder;
import com.github.fge.jsonschema.core.report.ListProcessingReport;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.processors.syntax.SyntaxValidator;
import com.google.common.annotations.VisibleForTesting;

/**
 * Validate a set of files against a set of schemas
 *
 */
@Mojo(name="validate", defaultPhase=LifecyclePhase.PROCESS_RESOURCES, threadSafe=true)
public class ValidateMojo extends AbstractMojo {
	public static class DirectoryURIMapping {
		public File directory;
		public URI uri;

		public DirectoryURIMapping() {
			// Nothing
		}

		@VisibleForTesting
		protected DirectoryURIMapping(URI uri, File directory) {
			this.directory = directory;
			this.uri = uri;
		}

		@Override
		public String toString() {
			return "{ " + directory + " -> " + uri + " }";
		}
	}

	private final ObjectMapper mapper = JacksonUtils.newMapper();
	private final Map<Object, JsonSchema> loadedSchemas = new HashMap<>();

	/**
	 * Optional source directories and URIs for schemas
	 */
	@Parameter
	private DirectoryURIMapping[] schemaMappings;

	@Parameter(required=true)
	private File sourceDirectory;
	@Parameter(defaultValue="*.json")
	private String[] includes;

	@Parameter(defaultValue="true")
	private boolean deepCheck;

	@Parameter(defaultValue="false")
	private boolean requireSchema;

	@Override
	public void execute() throws MojoExecutionException {
		URITranslatorConfigurationBuilder builder = URITranslatorConfiguration.newBuilder()
				.setNamespace(sourceDirectory.toURI());
		if (schemaMappings != null) {
			addSchemaMappings(builder);
		}

		LoadingConfiguration cfg = LoadingConfiguration.newBuilder()
				.setURITranslatorConfiguration(builder.freeze())
				.freeze();
		JsonSchemaFactory factory = JsonSchemaFactory.newBuilder().setLoadingConfiguration(cfg).freeze();
		URIManager uriManager = new URIManager(cfg);

		DirectoryScanner scanner = new DirectoryScanner();
		scanner.setBasedir(sourceDirectory);
		scanner.setIncludes(includes);
		scanner.scan();

		boolean success = true;
		for (String includedFileName : scanner.getIncludedFiles()) {
			File file = new File(sourceDirectory, includedFileName);
			try {
				getLog().info("Validating " + file);
				
				JsonNode node = mapper.readTree(file);
				ListProcessingReport report = validate(node, uriManager, factory);

				for (Iterator<ProcessingMessage> it = report.iterator(); it.hasNext(); ) {
					ProcessingMessage message = it.next();
					log(includedFileName, message);
				}
				if (!report.isSuccess()) {
					success = false;
				}
			} catch (JsonProcessingException e) {
				getLog().error(e);
				success = false;
			} catch (IOException e) {
				getLog().error(e);
				success = false;
			} catch (ProcessingException e) {
				getLog().error(e);
				success = false;
			}
		}

		if (!success) {
			throw new MojoExecutionException("Validation failures");
		}
	}
	
	@VisibleForTesting
	protected void setDeepCheck(boolean deepCheck) {
		this.deepCheck = deepCheck;
	}
	
	@VisibleForTesting
	protected void setRequireSchema(boolean requireSchema) {
		this.requireSchema = requireSchema;
	}
	
	@VisibleForTesting
	protected void setSchemaMappings(DirectoryURIMapping[] schemaMappings) {
		this.schemaMappings = schemaMappings;
	}
	
	@VisibleForTesting
	protected void setSourceDirectory(File sourceDirectory) {
		this.sourceDirectory = sourceDirectory;
	}
	
	@VisibleForTesting
	protected void setIncludes(String[] includes) {
		this.includes = includes;
	}
	
	@VisibleForTesting
	protected URITranslatorConfigurationBuilder addSchemaMappings(URITranslatorConfigurationBuilder builder) throws MojoExecutionException {
		for (DirectoryURIMapping schemaMapping : schemaMappings) {
			URI uri = schemaMapping.uri;
			if (uri.getFragment() != null) {
				throw new MojoExecutionException("URI " + uri + " must not contain a fragment");
			}
			if (!uri.isAbsolute()) {
				throw new MojoExecutionException("URI " + uri + " must be absolute");
			}
			// Check the path (and fix it up for the obvious issues)
			String path = uri.getPath();
			if (path == null) {
				path = "/";
			}
			if (!path.endsWith("/")) {
				getLog().warn("URI " + uri + " does not end with '/'");
				path += "/";
				try {
					uri = new URI(uri.getScheme(), uri.getAuthority(), path, uri.getQuery(), uri.getFragment());
				} catch (URISyntaxException e) {
					// Basically impossible: it was (syntactically) valid before that.
					throw new MojoExecutionException("Cannot construct fixed URI", e);
				}
			}
			getLog().debug("Mapping " + schemaMapping.directory + " to " + uri);
			builder.addPathRedirect(uri, schemaMapping.directory.toURI());
		}
		return builder;
	}

	@VisibleForTesting
	protected ListProcessingReport validate(JsonNode node, URIManager uriManager, JsonSchemaFactory factory) throws ProcessingException {
		ListProcessingReport report = new ListProcessingReport();

		// Determine the schema to be applied to it
		if (!node.hasNonNull("$schema")) {
			ProcessingMessage processingMessage = new ProcessingMessage();
			processingMessage.setMessage("Missing $schema");
			
			if (requireSchema) {
				report.error(processingMessage);
			} else {
				report.warn(processingMessage);
			}
			return report;
		}

		// Load the schema
		String dollarSchema = node.get("$schema").textValue();
		URI schemaUri;
		try {
			schemaUri = new URI(dollarSchema);
		} catch (URISyntaxException e) {
			ProcessingMessage processingMessage = new ProcessingMessage();
			processingMessage.setMessage("Invalid $schema URI '" + dollarSchema + "': " + e.getMessage());
			report.error(processingMessage);
			return report;
		}

		JsonSchema schema = loadedSchemas.get(schemaUri);
		if (schema == null) {
			// Schema we have not seen so far, load and validate it.
			JsonNode schemaNode = uriManager.getContent(schemaUri);

			SyntaxValidator syntaxValidator = factory.getSyntaxValidator();
			syntaxValidator.validateSchema(schemaNode);

			schema = factory.getJsonSchema(schemaNode);
			loadedSchemas.put(schemaUri, schema);
		}

		ProcessingReport validationReport = schema.validate(node, deepCheck);
		report.mergeWith(validationReport);
		return report;
	}

	@VisibleForTesting
	protected void log(String filename, ProcessingMessage message) {
		// FIXME: #getMessage() is ok, but ideally we also need the other key/value pairs.
		//        Doing that would require knowing that "message" is the one holding the message
		//        itself.
		StringBuilder logMessageBuilder = new StringBuilder();
		logMessageBuilder.append(filename);
		logMessageBuilder.append(": ");
		logMessageBuilder.append(message.getMessage());
		String logMessage = logMessageBuilder.toString();
		
		Log log = getLog();

		switch (message.getLogLevel()) {
		case NONE:
			// XXX: Do nothing?
			break;
		case DEBUG:
			log.debug(logMessage);
			break;
		case INFO:
			log.info(logMessage);
			break;
		case WARNING:
			log.warn(logMessage);
			break;
		case ERROR:
		case FATAL:
			log.error(logMessage);
			break;
		}
	}
}
