[![Build Status](https://travis-ci.org/Collaborne/json-schema-validator-maven-plugin.svg?branch=master)](https://travis-ci.org/Collaborne/json-schema-validator-maven-plugin)

JSON Schema Validator Maven Plugin
==================================

An Apache Maven (3.2.1+) plugin that validates a set of JSON files in the project against
JSON schema.


Use Cases
---------

Validating your JSON against a schema can help finding issues early, and possibly prevent
regressions. There are two main use cases for doing that validation inside your build:

1. Validate test data against existing schemas
2. Validate schemas themselves against the JSON Schema schema


Installation
------------

The plugin is available in Maven Central, so you should be able to just add

    <plugins>
      <plugin>
        <groupId>com.collaborne.maven</groupId>
        <artifactId>json-schema-validator-maven-plugin</artifactId>
        <version>1.0-beta-1</version>
        <executions>
          <execution>
            <id>validate-json</id>
            <phase>process-resources</phase>
            <goals>
              <goal>validate</goal>
            </goals>
            <configuration>
              <sourceDirectory>src/main/resources</sourceDirectory>
              <includes>
                <include>*.json</include>
              </includes>
              <requireSchema>false</requireSchema>
              <deepCheck>true</deepCheck>
              <schemaMappings>
                ...
              </schemaMappings>
            </configuration>
          </execution>
        </executions>
      </plugin>
      ...
    </plugins>


Goals
-----

There is only one available goal, `validate`. `validate` processes the included files in the
given `sourceDirectory`.


Configuration
-------------

Setting          | Default Value | Description
-----------------|---------------|-------------
`sourceDirectory`| (required)    | The directory to scan for JSON files to process
`includes`       | `*.json`      | A list of include patterns to process
`requireSchema`  | `false`       | If `true` then files without a `$schema` reference will lead to validation failures, otherwise those files are ignored
`deepCheck`      | `true`        | If `false` then validation stops early, if `true` children are validated even if  the container (array, object) is invalid
`schemaMappings` | (none)        | Definitions for mapping directories to URIs, see below

`schemaMappings` can be used to map directories on the file system to URIs, for example the following
mapping will tell the validator that schemas for `http://example.com/schema/` are available in the directory `src/main/schema/`:

    <configuration>
      ...
      <schemaMappings>
        <schemaMapping>
          <directory>src/main/schema</directory>
          <uri>http://example.com/schema/</uri>
        </schemaMapping>
      </schemaMappings>
    </configuration>

With this definition a schema `definitions/TYPE` inside the file `src/main/schema/data.json` can be referenced
in other files using `{ "$schema": "http://example.com/schema/data.json#/definitions/TYPE" }`


License
-------

> Copyright 2015 Collaborne B.V.
>
> Licensed under the Apache License, Version 2.0 (the "License");
> you may not use this file except in compliance with the License.
> You may obtain a copy of the License at [apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)
>
> Unless required by applicable law or agreed to in writing, software
> distributed under the License is distributed on an "AS IS" BASIS,
> WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
> See the License for the specific language governing permissions and
> limitations under the License.

