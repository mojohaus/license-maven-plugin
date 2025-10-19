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

def buildLog = new File(basedir, 'build.log')
assert buildLog.exists() : "build.log should exist"
def buildLogContent = buildLog.text

// Check that the build failed due to completeness check
assert buildLogContent.contains('Completeness Check Failed') : "Build should report completeness check failure"
assert buildLogContent.contains('Missing license entry for JAR') : "Build should report missing license entry"
assert buildLogContent.contains('extra-library') : "Build should identify the missing extra-library JAR"

println "Integration test passed: check-licenses-xml correctly detected incomplete licenses.xml"
