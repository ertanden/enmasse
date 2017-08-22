/*
 * Copyright 2016 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.enmasse.config;

/**
 * Labels that are applied to a destination cluster.
 */
public interface LabelKeys {
    String TYPE = "type";

    String UUID = "uuid";
    String ADDRESS = "address";
    String APP = "app";
    String CAPABILITY = "capability";
    java.lang.String CERT_SECRET_NAME = "io.enmasse.cert-secret-name";
}
