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

import com.google.auto.value.AutoValue;
import java.io.Serializable;
import java.util.Map;

/**
 * Data model represents the message sent from Debezium to Pub/Sub.
 */
@AutoValue
public class DebeziumLogEvent implements Serializable {

  public Object before;
  public Map<String, Object> after;
  public Map<String, Object> source;
  public String op;
  public long ts_ms;
  public Object transaction;

 }
