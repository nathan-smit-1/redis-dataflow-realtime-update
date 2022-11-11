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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.flogger.FluentLogger;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.apache.beam.sdk.transforms.DoFn;

/**
 * Pipeline operation to parse the Pub/Sub message as JSON into a POJO.
 */
public final class ParseMessageAsLogElement extends DoFn<String, BranchCompanySkuTransactionValue> {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  @ProcessElement
  public void parseStringToLogElement(ProcessContext context) {
    try {
      DebeziumLogEvent debeziumLogEvent = buildReader().readValue(context.element());
      BranchCompanySkuTransactionValue branchCompanySkuTransactionValue =
              new BranchCompanySkuTransactionValue(debeziumLogEvent.after.get("branch_id_no").toString(),
                                        "",
                                        debeziumLogEvent.after.get("sku_no").toString(),
                                        debeziumLogEvent.after.get("transaction_id").toString(),
                                        debeziumLogEvent.source.get("table").toString(),
                                        debeziumLogEvent.after.get("value").toString());
      context.output(branchCompanySkuTransactionValue);
    } catch (IOException ioexp) {
      logger.atWarning().atMostEvery(10, TimeUnit.SECONDS).withCause(ioexp).log();
    }
  }

  private ObjectReader buildReader() {
    return new ObjectMapper()
        .readerFor(DebeziumLogEvent.class);
  }
}
