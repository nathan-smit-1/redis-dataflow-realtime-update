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

import com.google.common.flogger.FluentLogger;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.io.redis.RedisIO;
import org.apache.beam.sdk.io.redis.RedisIO.Write.Method;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.*;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import java.util.Arrays;
import java.util.Map;
import org.apache.beam.sdk.io.GenerateSequence;
import org.apache.beam.sdk.transforms.Partition.PartitionFn;
import org.apache.beam.sdk.transforms.windowing.AfterProcessingTime;
import org.apache.beam.sdk.transforms.windowing.GlobalWindows;
import org.apache.beam.sdk.transforms.windowing.Repeatedly;
import org.apache.beam.sdk.transforms.windowing.Window;
import org.apache.beam.sdk.values.PCollectionList;
import org.apache.beam.sdk.values.PCollectionView;
import org.joda.time.Duration;

/**
 * Realtime Dataflow pipeline to extract experiment metrics from Log Events
 * published on Pub/Sub.
 */
public final class RealTimeRedisUpdatePipeline {

    private static final FluentLogger logger = FluentLogger.forEnclosingClass();

    /**
     * Parses the command line arguments and runs the pipeline.
     */
    public static void main(String[] args) {
        RealTimeRedisUpdatePipelineOptions options = extractPipelineOptions(args);
        Pipeline pipeline = Pipeline.create(options);

        String project = options.getBigQueryInputProject();
        String dataset = options.getBigQueryInputDataset();
        String table = options.getBigQueryInputTable();

        String query = String.format("SELECT branch_id, cpy_id FROM `%s.%s.%s`;",
                project, dataset, table);

        final PCollectionView<Map<String, String>> branches = pipeline
                // Emitted long data trigger this batch read BigQuery client job.
                .apply(String.format("Updating every %s hours", 10),
                        GenerateSequence.from(0).withRate(1, Duration.standardHours(10)))
                .apply(
                        Window.<Long>into(new GlobalWindows())
                                .triggering(Repeatedly.forever(AfterProcessingTime.pastFirstElementInPane()))
                                .discardingFiredPanes()) // Caching results as Map.

                .apply(new ReadSlowChangingBigQueryTable("Read BigQuery Table", query, "branch_id", "cpy_id"))
                // Caching results as Map.
                .apply("View As Map", View.asMap());

        PCollection<BranchCompanySkuTransactionValue> allDebeziumEvents
                = pipeline
                .apply("Read PubSub Events",
                        PubsubIO.readStrings().fromTopic(options.getInputTopic()))
                .apply("Parse Message JSON",
                        ParDo.of(new ParseMessageAsLogElement()));

        PCollection<BranchCompanySkuTransactionValue> filteredDebeziumEvents = allDebeziumEvents
                .apply("Filtering out messages not related to transactions",Filter.by(new SerializableFunction<BranchCompanySkuTransactionValue, Boolean>() {
                    @Override
                    public Boolean apply(BranchCompanySkuTransactionValue input) {
                        return input.getTable().equals("TRANSACTION_DETAIL");
                    }
                }));

        PCollection<BranchCompanySkuTransactionValue> dedupedEvents = filteredDebeziumEvents
                .apply("Dedupe incoming messages using transaction id",
                        Deduplicate.withRepresentativeValueFn(BranchCompanySkuTransactionValue::getTransactionId)
                                .withDuration(Duration.standardHours(10))
                );

        PCollection<BranchCompanySkuTransactionValue> output = dedupedEvents.apply(
                ParDo
                        .of(new AddCompanyDataToMessage(branches))
                        // the side input is provided here and above
                        // now the city map is available for the transform to use
                        .withSideInput("branches", branches)
        );

        //create partitions based on company
        PCollectionList<BranchCompanySkuTransactionValue> msgs
                = output.apply(Partition.of(2, new PartitionFn<BranchCompanySkuTransactionValue>() {
            public int partitionFor(BranchCompanySkuTransactionValue branchCompanySkuValue, int numPartitions) {
                if (Arrays.asList("1", "2", "3").contains(branchCompanySkuValue.getCompany())) {
                    return 0;
                } else {
                    return 1;
                }

            }
        }));

        PCollection<BranchCompanySkuTransactionValue> partition1 = msgs.get(0);

        RedisIO.Write redisWriter = RedisIO.write().withEndpoint(options.getRedisHost(),
                options.getRedisPort());

        //stock movement updater
        partition1
                .apply("Get stock movement from message for partition 1", ParDo.of(
                        new DoFn<BranchCompanySkuTransactionValue, KV<String, String>>() {
                            @ProcessElement
                            public void getStockMovement(ProcessContext context) {
                                BranchCompanySkuTransactionValue updateValue = context.element();
                                context.output(
                                        KV.of(updateValue.getBranch()
                                                        + "|" + updateValue.getCompany()
                                                        + "|" + updateValue.getSku(),
                                                updateValue.getValue()));
                            }
                        }
                ))
                .apply("Update stock movement counter", redisWriter.withMethod(Method.INCRBY));

        pipeline.run();
    }

    /**
     * Parse Pipeline options from command line arguments.
     */
    private static RealTimeRedisUpdatePipelineOptions extractPipelineOptions(String[] args) {
        RealTimeRedisUpdatePipelineOptions options = PipelineOptionsFactory
                .fromArgs(args)
                .withValidation()
                .as(RealTimeRedisUpdatePipelineOptions.class);

        return options;
    }
}