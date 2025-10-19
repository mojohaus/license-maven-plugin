/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

def licensesXml = new File(basedir, 'target/generated-resources/licenses.xml')
assert licensesXml.exists() : "licenses.xml should exist"

def libDir = new File(basedir, 'target/lib')
assert libDir.exists() : "lib directory should exist"
assert libDir.list().length > 0 : "lib directory should contain JAR files"

// The build should succeed because licenses.xml matches the JARs in lib/
def buildLog = new File(basedir, 'build.log')
assert buildLog.exists() : "build.log should exist"
def buildLogContent = buildLog.text
assert buildLogContent.contains('Check Passed') || !buildLogContent.contains('completeness check failed') : "Check should pass"

println "Integration test passed: licenses.xml is complete and minimal"
