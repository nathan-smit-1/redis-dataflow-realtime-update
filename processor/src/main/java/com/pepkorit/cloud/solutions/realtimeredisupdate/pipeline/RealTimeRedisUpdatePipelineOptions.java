/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pepkorit.cloud.solutions.realtimeredisupdate.pipeline;

import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.Validation;

/**
 * A Streaming pipeline option for the Metrics calculation pipeline.
 */
public interface RealTimeRedisUpdatePipelineOptions extends PipelineOptions {

  @Description("The Cloud Pub/Sub topic to read from.")
  @Validation.Required
  String getInputTopic();

  void setInputTopic(String value);

  @Description("The project holding the BigQuery data for side input")
  @Validation.Required
  String getBigQueryInputProject();

  void setBigQueryInputProject(String value);

  @Description("The Dataset in BigQuery containing the side input table")
  @Validation.Required
  String getBigQueryInputDataset();

  void setBigQueryInputDataset(String value);

  @Description("The BigQuery table for use as side input")
  @Validation.Required
  String getBigQueryInputTable();

  void setBigQueryInputTable(String value);

  @Description("Redis Host")
  @Validation.Required
  String getRedisHost();

  void setRedisHost(String redisHost);

  @Description("Redis Port")
  @Validation.Required
  Integer getRedisPort();

  void setRedisPort(Integer redisPort);
}
