## Realtime pipeline using Dataflow and Cloud Memorystore (Redis)

This solution was created for the "store fulfillment project".  Code contains a Dataflow pipeline that
ingests data from pub/sub, applies a side input and updates a key/value pair in a Redis instance

### Setup Environment
1. Clone this repository
    ```shell script
    git clone https://gitlab.pepkorit.com/NATHANS/redis-dataflow-realtime-update.git
    cd redis-dataflow-realtime-update
    ```
2. Update and activate all environment variables in `set_variables.sh`
    ```shell script
    source set_variables.sh
    ```
3. Enable required Cloud products
    ```shell script
    gcloud services enable \
    compute.googleapis.com \
    pubsub.googleapis.com \
    redis.googleapis.com \
    dataflow.googleapis.com \
    storage-component.googleapis.com
    ```
### Create Pub/Sub Topic
Pub/Sub is a global message bus enabling easy message consumption in a decoupled fashion.
Create a Pub/Sub topic to receive application instrumentation messages
```shell script
gcloud pubsub topics create $APP_EVENTS_TOPIC --project $PROJECT_ID
```
### Create VPC network
Protecting the Redis instance is important as it  does not provide any protections from external entities.

1. Creating a sepate VPC network with external ingress blocked by a firewall provides basic security for the instance.
    ```shell script
    gcloud compute networks create $VPC_NETWORK_NAME \
    --subnet-mode=auto \
    --bgp-routing-mode=regional
    ```
2. Create Firewall rule to enable SSH
    ```shell script
    gcloud compute firewall-rules create allow-internal-ssh \
    --network $VPC_NETWORK_NAME \
    --allow tcp:22,icmp
    ```

### Configure Cloud Memorystore
[Cloud Memorystore](https://cloud.google.com/memorystore) provides a fully managed [Redis](https://redis.io/) database.
Redis is a NoSQL In-Memory database, which offers comprehensive in-built functions for
[SETs](https://redis.io/commands#set) operations,
including efficient HLL operations for cardinality measurement.

1. Create Redis instance in Memorystore.
    ```shell script
    gcloud redis instances create $REDIS_NAME \
    --size=1 \
    --region=$REGION_ID \
    --zone="$ZONE_ID" \
    --network=$VPC_NETWORK_NAME \
    --tier=standard
    ```
   
2. Capture instance's IP to configure Dataflow
    ```shell script
    export REDIS_IP="$(gcloud redis instances describe $REDIS_NAME --region=$REGION_ID \
   | grep host \
   | sed 's/host: //')"
    ```

### Start Pipeline

1. Create Cloud Storage bucket for temporary and staging area for the pipeline
    ```shell script
    gsutil mb -l $REGION_ID -p $PROJECT_ID gs://$TEMP_GCS_BUCKET
    ```
2. Launch the pipeline using [Maven](https://apache.org/maven)
    ```shell script
    cd processor
    ``` 
    ```shell script
    mvn clean compile exec:java \
      -Dexec.mainClass=com.google.cloud.solutions.realtimedash.pipeline.MetricsCalculationPipeline \
      -Dexec.cleanupDaemonThreads=false \
      -Dmaven.test.skip=true \
      -Dexec.args=" \
    --streaming \
    --project=$PROJECT_ID \
    --runner=DataflowRunner \
    --stagingLocation=gs://$TEMP_GCS_BUCKET/stage/ \
    --tempLocation=gs://$TEMP_GCS_BUCKET/temp/ \
    --inputTopic=projects/$PROJECT_ID/topics/$APP_EVENTS_TOPIC \
    --bigQueryInputProject=$BIG_QUERY_PROJECT \
    --bigQueryInputDataset=$BIG_QUERY_DATASET \
    --BigQueryInputTable=$BIG_QUERY_TABLE \
    --workerMachineType=n1-standard-1 \
    --region=$REGION_ID \
    --subnetwork=regions/$REGION_ID/subnetworks/$VPC_NETWORK_NAME \
    --redisHost=$REDIS_IP \
    --redisPort=6379"
    ```

### Send a dummy message to test
1. Create and initialize a new python3 virtual environment (you need to have `pyhton3-venv` package)
    ```json message
   {
   "before": null,
   "after": {
   "branch_id_no": 6373,
   "sku_no": 867840,
   "transaction_id":12345,
   "value": -5
   },
   "source": {
   "version": "1.8.0.Final",
   "connector": "oracle",
   "name": "glodc_schema",
   "ts_ms": 1648200702000,
   "snapshot": "false",
   "db": "GLODCP",
   "sequence": null,
   "schema": "DC",
   "table": "TRANSACTION_DETAIL",
   "txId": "06004200eff83900",
   "scn": "774200482967",
   "commit_scn": "774200597370",
   "lcr_position": null
   },
   "op": "c",
   "ts_ms": 1648193561170,
   "transaction": null
   }
    ```
2. Create a test server to connect to redis
    ```shell script
    gcloud compute instances create redis-connect-server \
    --zone $ZONE_ID \
    --image-family debian-10 \
    --image-project debian-cloud \
    --network $VPC_NETWORK_NAME
    ```

3.  Connect to test VM to allow connection to redis
     SSH to created compute engine and execute below commands
    ```shell script
    sudo apt-get install telnet
    telnet instance-ip-address 6379
    GET BRANCH|COMPANY|SKU
    ```