kafka-semantics-tester
============

### Introduction
Apache Kafka provides basically at least once semantics but in 0.11 release exactly-once introduced.
[KIP-98](https://cwiki.apache.org/confluence/display/KAFKA/KIP-98+-+Exactly+Once+Delivery+and+Transactional+Messaging) added the following capabilities to Apache Kafka:
* An Idempotent Producer based on producer identifiers (PIDs) to eliminate duplicates.
* Cross-partition transactions for writes and offset commits.
* Consumer support for fetching only committed messages.

Further reading about exactly once and transactions can be found [here](https://www.confluent.io/blog/exactly-once-semantics-are-possible-heres-how-apache-kafka-does-it/) and [here](https://www.confluent.io/blog/transactions-apache-kafka/).

As I'm evaluating Kafka exactly-once semantics I've written a bunch of applications to automate things.
Please be aware this test provide neither 100% coverage nor high availability in testing(for example doesn't tolerate Producer/Consumer crashes).
Use it accordingly.

### Build the app
To build, you need Scala 2.12, git and maven on the box.
Do a git clone of this repo and then run:
```
cd kafka-semantics-tester
mvn clean package
```

This generates the following applications:
* `kafka-producer`: It can connect to a Kafka broker and produce data in a topic in a transactional fashion.
  Under the hood it does the following with the generated records:
  * either simply commit
  * or abort and right after commit the same record set (this emulates a failing application in a simple way)
* `standalone-app-to-test`: It can connect to a Kafka broker and does the following things:
  * Consumes `kafka-producer` records
  * Forwards the consumed records to another topic

  This has basically 2 modes:
  * at-least-once: Reads uncommitted records as well, uses `consumer.commitSync()`. The application randomly throws exception
    right before the consumer committed.
  * exactly-once: Reads committed records only, uses transactions and sends offset with `producer.sendOffsetsToTransaction(...)`.
    Records forwarded the same way as `kafka-producer` does.
* `kafka-consumer`: It can connect to a Kafka broker and consume committed records only coming from `standalone-app-to-test`.
  Incoming data semantics evaluated and exception is thrown if semantics broken.

### Running the app
When everything runs properly the following chain will be built up:

`kafka-producer -> kafka(src-topic) -> *-app-to-test -> kafka(dst-topic) -> kafka-consumer`

Change `SERVER` variable in `config.sh`.

Make sure the related topics are deleted:
```
./delete-topics.sh
```

consumer-console
```
$ ./start-consumer.sh
>>> 19/01/29 14:35:20 INFO consumer.KafkaConsumer$: Creating config properties...
>>> 19/01/29 14:35:20 INFO consumer.KafkaConsumer$: OK
>>> 19/01/29 14:35:20 INFO consumer.KafkaConsumer$: Creating kafka consumer...
>>> 19/01/29 14:35:20 INFO consumer.ConsumerConfig: ConsumerConfig values:
	auto.commit.interval.ms = 5000
	auto.offset.reset = latest
	bootstrap.servers = [host:9092]
	check.crcs = true
	client.id =
	connections.max.idle.ms = 540000
	default.api.timeout.ms = 60000
	enable.auto.commit = false
	exclude.internal.topics = true
	fetch.max.bytes = 52428800
	fetch.max.wait.ms = 500
	fetch.min.bytes = 1
	group.id = consumer-static-group-id
	heartbeat.interval.ms = 3000
	interceptor.classes = []
	internal.leave.group.on.close = true
	isolation.level = read_committed
	key.deserializer = class org.apache.kafka.common.serialization.StringDeserializer
	max.partition.fetch.bytes = 1048576
	max.poll.interval.ms = 300000
	max.poll.records = 500
	metadata.max.age.ms = 300000
	metric.reporters = []
	metrics.num.samples = 2
	metrics.recording.level = INFO
	metrics.sample.window.ms = 30000
	partition.assignment.strategy = [class org.apache.kafka.clients.consumer.RangeAssignor]
	receive.buffer.bytes = 65536
	reconnect.backoff.max.ms = 1000
	reconnect.backoff.ms = 50
	request.timeout.ms = 30000
	retry.backoff.ms = 100
	sasl.client.callback.handler.class = null
	sasl.jaas.config = null
	sasl.kerberos.kinit.cmd = /usr/bin/kinit
	sasl.kerberos.min.time.before.relogin = 60000
	sasl.kerberos.service.name = null
	sasl.kerberos.ticket.renew.jitter = 0.05
	sasl.kerberos.ticket.renew.window.factor = 0.8
	sasl.login.callback.handler.class = null
	sasl.login.class = null
	sasl.login.refresh.buffer.seconds = 300
	sasl.login.refresh.min.period.seconds = 60
	sasl.login.refresh.window.factor = 0.8
	sasl.login.refresh.window.jitter = 0.05
	sasl.mechanism = GSSAPI
	security.protocol = PLAINTEXT
	send.buffer.bytes = 131072
	session.timeout.ms = 10000
	ssl.cipher.suites = null
	ssl.enabled.protocols = [TLSv1.2, TLSv1.1, TLSv1]
	ssl.endpoint.identification.algorithm = https
	ssl.key.password = null
	ssl.keymanager.algorithm = SunX509
	ssl.keystore.location = null
	ssl.keystore.password = null
	ssl.keystore.type = JKS
	ssl.protocol = TLS
	ssl.provider = null
	ssl.secure.random.implementation = null
	ssl.trustmanager.algorithm = PKIX
	ssl.truststore.location = null
	ssl.truststore.password = null
	ssl.truststore.type = JKS
	value.deserializer = class org.apache.kafka.common.serialization.StringDeserializer

>>> 19/01/29 14:35:20 INFO utils.AppInfoParser: Kafka version : 2.0.0
>>> 19/01/29 14:35:20 INFO utils.AppInfoParser: Kafka commitId : 3402a8361b734732
>>> 19/01/29 14:35:20 INFO consumer.KafkaConsumer$: OK
>>> 19/01/29 14:35:21 WARN clients.NetworkClient: [Consumer clientId=consumer-1, groupId=consumer-static-group-id] Error while fetching metadata with correlation id 3 : {dst-topic=LEADER_NOT_AVAILABLE}
>>> 19/01/29 14:35:21 INFO clients.Metadata: Cluster ID: S-VDN7m3RCmgAs3vzt9aZw
>>> 19/01/29 14:35:21 INFO internals.AbstractCoordinator: [Consumer clientId=consumer-1, groupId=consumer-static-group-id] Discovered group coordinator host:9092 (id: 2147483598 rack: null)
>>> 19/01/29 14:35:21 INFO internals.ConsumerCoordinator: [Consumer clientId=consumer-1, groupId=consumer-static-group-id] Revoking previously assigned partitions []
>>> 19/01/29 14:35:21 INFO internals.AbstractCoordinator: [Consumer clientId=consumer-1, groupId=consumer-static-group-id] (Re-)joining group
>>> 19/01/29 14:35:26 INFO internals.AbstractCoordinator: [Consumer clientId=consumer-1, groupId=consumer-static-group-id] Successfully joined group with generation 1
>>> 19/01/29 14:35:26 INFO internals.ConsumerCoordinator: [Consumer clientId=consumer-1, groupId=consumer-static-group-id] Setting newly assigned partitions [dst-topic-0]
>>> 19/01/29 14:35:27 INFO internals.Fetcher: [Consumer clientId=consumer-1, groupId=consumer-static-group-id] Resetting offset for partition dst-topic-0 to offset 0.
>>> 19/01/29 14:35:55 INFO consumer.KafkaConsumer$: Record: ConsumerRecord(topic = dst-topic, partition = 0, offset = 0, CreateTime = 1548768954509, serialized key size = -1, serialized value size = 12, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-0)
>>> 19/01/29 14:35:55 INFO consumer.KafkaConsumer$: Record: ConsumerRecord(topic = dst-topic, partition = 0, offset = 1, CreateTime = 1548768954515, serialized key size = -1, serialized value size = 12, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-1)
>>> 19/01/29 14:35:55 INFO consumer.KafkaConsumer$: Record: ConsumerRecord(topic = dst-topic, partition = 0, offset = 2, CreateTime = 1548768954515, serialized key size = -1, serialized value size = 12, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-2)
>>> 19/01/29 14:35:55 INFO consumer.KafkaConsumer$: Record: ConsumerRecord(topic = dst-topic, partition = 0, offset = 3, CreateTime = 1548768954515, serialized key size = -1, serialized value size = 12, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-3)
>>> 19/01/29 14:35:55 INFO consumer.KafkaConsumer$: Record: ConsumerRecord(topic = dst-topic, partition = 0, offset = 4, CreateTime = 1548768954516, serialized key size = -1, serialized value size = 12, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-4)
>>> 19/01/29 14:35:55 INFO consumer.KafkaConsumer$: Record: ConsumerRecord(topic = dst-topic, partition = 0, offset = 5, CreateTime = 1548768954516, serialized key size = -1, serialized value size = 12, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-5)
>>> 19/01/29 14:35:55 INFO consumer.KafkaConsumer$: Record: ConsumerRecord(topic = dst-topic, partition = 0, offset = 6, CreateTime = 1548768954516, serialized key size = -1, serialized value size = 12, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-6)
>>> 19/01/29 14:35:55 INFO consumer.KafkaConsumer$: Record: ConsumerRecord(topic = dst-topic, partition = 0, offset = 7, CreateTime = 1548768954516, serialized key size = -1, serialized value size = 12, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-7)
>>> 19/01/29 14:35:55 INFO consumer.KafkaConsumer$: Record: ConsumerRecord(topic = dst-topic, partition = 0, offset = 8, CreateTime = 1548768954516, serialized key size = -1, serialized value size = 12, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-8)
>>> 19/01/29 14:35:55 INFO consumer.KafkaConsumer$: Record: ConsumerRecord(topic = dst-topic, partition = 0, offset = 9, CreateTime = 1548768954516, serialized key size = -1, serialized value size = 12, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-9)
>>> 19/01/29 14:35:55 INFO consumer.KafkaConsumer$: Record: ConsumerRecord(topic = dst-topic, partition = 0, offset = 10, CreateTime = 1548768954516, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-10)
>>> 19/01/29 14:35:55 INFO consumer.KafkaConsumer$: Record: ConsumerRecord(topic = dst-topic, partition = 0, offset = 11, CreateTime = 1548768954517, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-11)
>>> 19/01/29 14:35:55 INFO consumer.KafkaConsumer$: Record: ConsumerRecord(topic = dst-topic, partition = 0, offset = 12, CreateTime = 1548768954517, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-12)
>>> 19/01/29 14:35:55 INFO consumer.KafkaConsumer$: Record: ConsumerRecord(topic = dst-topic, partition = 0, offset = 13, CreateTime = 1548768954517, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-13)
>>> 19/01/29 14:35:55 INFO consumer.KafkaConsumer$: Record: ConsumerRecord(topic = dst-topic, partition = 0, offset = 14, CreateTime = 1548768954517, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-14)
>>> 19/01/29 14:35:55 INFO consumer.KafkaConsumer$: Record: ConsumerRecord(topic = dst-topic, partition = 0, offset = 15, CreateTime = 1548768954517, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-15)
>>> 19/01/29 14:35:55 INFO consumer.KafkaConsumer$: Record: ConsumerRecord(topic = dst-topic, partition = 0, offset = 16, CreateTime = 1548768954518, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-16)
>>> 19/01/29 14:35:55 INFO consumer.KafkaConsumer$: Record: ConsumerRecord(topic = dst-topic, partition = 0, offset = 17, CreateTime = 1548768954518, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-17)
>>> 19/01/29 14:35:55 INFO consumer.KafkaConsumer$: Record: ConsumerRecord(topic = dst-topic, partition = 0, offset = 18, CreateTime = 1548768954518, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-18)
>>> 19/01/29 14:35:55 INFO consumer.KafkaConsumer$: Record: ConsumerRecord(topic = dst-topic, partition = 0, offset = 19, CreateTime = 1548768954518, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-19)
>>> 19/01/29 14:35:55 INFO consumer.KafkaConsumer$: Record: ConsumerRecord(topic = dst-topic, partition = 0, offset = 20, CreateTime = 1548768954838, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-20)
>>> 19/01/29 14:35:55 INFO consumer.KafkaConsumer$: Record: ConsumerRecord(topic = dst-topic, partition = 0, offset = 21, CreateTime = 1548768954839, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-21)
>>> 19/01/29 14:35:55 INFO consumer.KafkaConsumer$: Record: ConsumerRecord(topic = dst-topic, partition = 0, offset = 22, CreateTime = 1548768954839, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-22)
>>> 19/01/29 14:35:55 INFO consumer.KafkaConsumer$: Record: ConsumerRecord(topic = dst-topic, partition = 0, offset = 23, CreateTime = 1548768954839, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-23)
>>> 19/01/29 14:35:55 INFO consumer.KafkaConsumer$: Record: ConsumerRecord(topic = dst-topic, partition = 0, offset = 24, CreateTime = 1548768954839, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-24)
>>> 19/01/29 14:35:55 INFO consumer.KafkaConsumer$: Record: ConsumerRecord(topic = dst-topic, partition = 0, offset = 25, CreateTime = 1548768954840, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-25)
>>> 19/01/29 14:35:55 INFO consumer.KafkaConsumer$: Record: ConsumerRecord(topic = dst-topic, partition = 0, offset = 26, CreateTime = 1548768954840, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-26)
>>> 19/01/29 14:35:55 INFO consumer.KafkaConsumer$: Record: ConsumerRecord(topic = dst-topic, partition = 0, offset = 27, CreateTime = 1548768954840, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-27)
>>> 19/01/29 14:35:55 INFO consumer.KafkaConsumer$: Record: ConsumerRecord(topic = dst-topic, partition = 0, offset = 28, CreateTime = 1548768954840, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-28)
>>> 19/01/29 14:35:55 INFO consumer.KafkaConsumer$: Record: ConsumerRecord(topic = dst-topic, partition = 0, offset = 29, CreateTime = 1548768954840, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-29)
>>> 19/01/29 14:35:56 INFO consumer.KafkaConsumer$: Record: ConsumerRecord(topic = dst-topic, partition = 0, offset = 30, CreateTime = 1548768955769, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-30)
>>> 19/01/29 14:35:56 INFO consumer.KafkaConsumer$: Record: ConsumerRecord(topic = dst-topic, partition = 0, offset = 31, CreateTime = 1548768955770, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-31)
>>> 19/01/29 14:35:56 INFO consumer.KafkaConsumer$: Record: ConsumerRecord(topic = dst-topic, partition = 0, offset = 32, CreateTime = 1548768955770, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-32)
>>> 19/01/29 14:35:56 INFO consumer.KafkaConsumer$: Record: ConsumerRecord(topic = dst-topic, partition = 0, offset = 33, CreateTime = 1548768955770, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-33)
>>> 19/01/29 14:35:56 INFO consumer.KafkaConsumer$: Record: ConsumerRecord(topic = dst-topic, partition = 0, offset = 34, CreateTime = 1548768955770, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-34)
>>> 19/01/29 14:35:56 INFO consumer.KafkaConsumer$: Record: ConsumerRecord(topic = dst-topic, partition = 0, offset = 35, CreateTime = 1548768955771, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-35)
>>> 19/01/29 14:35:56 INFO consumer.KafkaConsumer$: Record: ConsumerRecord(topic = dst-topic, partition = 0, offset = 36, CreateTime = 1548768955771, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-36)
>>> 19/01/29 14:35:56 INFO consumer.KafkaConsumer$: Record: ConsumerRecord(topic = dst-topic, partition = 0, offset = 37, CreateTime = 1548768955771, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-37)
>>> 19/01/29 14:35:56 INFO consumer.KafkaConsumer$: Record: ConsumerRecord(topic = dst-topic, partition = 0, offset = 38, CreateTime = 1548768955771, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-38)
>>> 19/01/29 14:35:56 INFO consumer.KafkaConsumer$: Record: ConsumerRecord(topic = dst-topic, partition = 0, offset = 39, CreateTime = 1548768955772, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-39)
>>> 19/01/29 14:36:04 INFO consumer.KafkaConsumer$: Record: ConsumerRecord(topic = dst-topic, partition = 0, offset = 40, CreateTime = 1548768963918, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-30)
>>> 19/01/29 14:36:04 ERROR consumer.KafkaConsumer$: Arrived the same data with offset 30, exactly-once semantics broken
>>> 19/01/29 14:36:04 INFO consumer.KafkaConsumer$: Closing kafka consumer...
>>> 19/01/29 14:36:05 INFO consumer.KafkaConsumer$: OK
Exception in thread "main" java.lang.Exception
	at com.kafka.consumer.SemanticsTester$.checkExactlyOnce(SemanticsTester.scala:52)
	at com.kafka.consumer.KafkaConsumer$$anonfun$main$1.apply(KafkaConsumer.scala:50)
	at com.kafka.consumer.KafkaConsumer$$anonfun$main$1.apply(KafkaConsumer.scala:46)
	at scala.collection.Iterator$class.foreach(Iterator.scala:891)
	at scala.collection.AbstractIterator.foreach(Iterator.scala:1334)
	at scala.collection.IterableLike$class.foreach(IterableLike.scala:72)
	at scala.collection.AbstractIterable.foreach(Iterable.scala:54)
	at com.kafka.consumer.KafkaConsumer$.main(KafkaConsumer.scala:46)
	at com.kafka.consumer.KafkaConsumer.main(KafkaConsumer.scala)
```

app-to-test-console
```
./start-standalone-app-to-test.sh false
>>> 19/01/29 14:35:38 INFO processor.KafkaProcessor$: Creating config properties...
>>> 19/01/29 14:35:38 INFO processor.KafkaProcessor$: OK
>>> 19/01/29 14:35:38 INFO processor.KafkaProcessor$: Creating kafka consumer...
>>> 19/01/29 14:35:38 INFO consumer.ConsumerConfig: ConsumerConfig values:
	auto.commit.interval.ms = 5000
	auto.offset.reset = latest
	bootstrap.servers = [host:9092]
	check.crcs = true
	client.id =
	connections.max.idle.ms = 540000
	default.api.timeout.ms = 60000
	enable.auto.commit = false
	exclude.internal.topics = true
	fetch.max.bytes = 52428800
	fetch.max.wait.ms = 500
	fetch.min.bytes = 1
	group.id = processor-static-group-id
	heartbeat.interval.ms = 3000
	interceptor.classes = []
	internal.leave.group.on.close = true
	isolation.level = read_uncommitted
	key.deserializer = class org.apache.kafka.common.serialization.StringDeserializer
	max.partition.fetch.bytes = 1048576
	max.poll.interval.ms = 300000
	max.poll.records = 500
	metadata.max.age.ms = 300000
	metric.reporters = []
	metrics.num.samples = 2
	metrics.recording.level = INFO
	metrics.sample.window.ms = 30000
	partition.assignment.strategy = [class org.apache.kafka.clients.consumer.RangeAssignor]
	receive.buffer.bytes = 65536
	reconnect.backoff.max.ms = 1000
	reconnect.backoff.ms = 50
	request.timeout.ms = 30000
	retry.backoff.ms = 100
	sasl.client.callback.handler.class = null
	sasl.jaas.config = null
	sasl.kerberos.kinit.cmd = /usr/bin/kinit
	sasl.kerberos.min.time.before.relogin = 60000
	sasl.kerberos.service.name = null
	sasl.kerberos.ticket.renew.jitter = 0.05
	sasl.kerberos.ticket.renew.window.factor = 0.8
	sasl.login.callback.handler.class = null
	sasl.login.class = null
	sasl.login.refresh.buffer.seconds = 300
	sasl.login.refresh.min.period.seconds = 60
	sasl.login.refresh.window.factor = 0.8
	sasl.login.refresh.window.jitter = 0.05
	sasl.mechanism = GSSAPI
	security.protocol = PLAINTEXT
	send.buffer.bytes = 131072
	session.timeout.ms = 10000
	ssl.cipher.suites = null
	ssl.enabled.protocols = [TLSv1.2, TLSv1.1, TLSv1]
	ssl.endpoint.identification.algorithm = https
	ssl.key.password = null
	ssl.keymanager.algorithm = SunX509
	ssl.keystore.location = null
	ssl.keystore.password = null
	ssl.keystore.type = JKS
	ssl.protocol = TLS
	ssl.provider = null
	ssl.secure.random.implementation = null
	ssl.trustmanager.algorithm = PKIX
	ssl.truststore.location = null
	ssl.truststore.password = null
	ssl.truststore.type = JKS
	value.deserializer = class org.apache.kafka.common.serialization.StringDeserializer

>>> 19/01/29 14:35:38 INFO utils.AppInfoParser: Kafka version : 2.0.0
>>> 19/01/29 14:35:38 INFO utils.AppInfoParser: Kafka commitId : 3402a8361b734732
>>> 19/01/29 14:35:38 INFO processor.KafkaProcessor$: OK
>>> 19/01/29 14:35:38 INFO processor.KafkaProcessor$: Creating config properties...
>>> 19/01/29 14:35:38 INFO processor.KafkaProcessor$: OK
>>> 19/01/29 14:35:38 INFO processor.KafkaProcessor$: Creating kafka producer...
>>> 19/01/29 14:35:38 INFO producer.ProducerConfig: ProducerConfig values:
	acks = 1
	batch.size = 16384
	bootstrap.servers = [host:9092]
	buffer.memory = 33554432
	client.id =
	compression.type = none
	connections.max.idle.ms = 540000
	enable.idempotence = false
	interceptor.classes = []
	key.serializer = class org.apache.kafka.common.serialization.StringSerializer
	linger.ms = 0
	max.block.ms = 60000
	max.in.flight.requests.per.connection = 5
	max.request.size = 1048576
	metadata.max.age.ms = 300000
	metric.reporters = []
	metrics.num.samples = 2
	metrics.recording.level = INFO
	metrics.sample.window.ms = 30000
	partitioner.class = class org.apache.kafka.clients.producer.internals.DefaultPartitioner
	receive.buffer.bytes = 32768
	reconnect.backoff.max.ms = 1000
	reconnect.backoff.ms = 50
	request.timeout.ms = 30000
	retries = 0
	retry.backoff.ms = 100
	sasl.client.callback.handler.class = null
	sasl.jaas.config = null
	sasl.kerberos.kinit.cmd = /usr/bin/kinit
	sasl.kerberos.min.time.before.relogin = 60000
	sasl.kerberos.service.name = null
	sasl.kerberos.ticket.renew.jitter = 0.05
	sasl.kerberos.ticket.renew.window.factor = 0.8
	sasl.login.callback.handler.class = null
	sasl.login.class = null
	sasl.login.refresh.buffer.seconds = 300
	sasl.login.refresh.min.period.seconds = 60
	sasl.login.refresh.window.factor = 0.8
	sasl.login.refresh.window.jitter = 0.05
	sasl.mechanism = GSSAPI
	security.protocol = PLAINTEXT
	send.buffer.bytes = 131072
	ssl.cipher.suites = null
	ssl.enabled.protocols = [TLSv1.2, TLSv1.1, TLSv1]
	ssl.endpoint.identification.algorithm = https
	ssl.key.password = null
	ssl.keymanager.algorithm = SunX509
	ssl.keystore.location = null
	ssl.keystore.password = null
	ssl.keystore.type = JKS
	ssl.protocol = TLS
	ssl.provider = null
	ssl.secure.random.implementation = null
	ssl.trustmanager.algorithm = PKIX
	ssl.truststore.location = null
	ssl.truststore.password = null
	ssl.truststore.type = JKS
	transaction.timeout.ms = 60000
	transactional.id = null
	value.serializer = class org.apache.kafka.common.serialization.StringSerializer

>>> 19/01/29 14:35:38 INFO utils.AppInfoParser: Kafka version : 2.0.0
>>> 19/01/29 14:35:38 INFO utils.AppInfoParser: Kafka commitId : 3402a8361b734732
>>> 19/01/29 14:35:38 INFO processor.KafkaProcessor$: OK
>>> 19/01/29 14:35:38 INFO processor.KafkaProcessor$: Creating kafka producer send strategy...
>>> 19/01/29 14:35:38 INFO processor.KafkaProcessor$: OK
>>> 19/01/29 14:35:39 WARN clients.NetworkClient: [Consumer clientId=consumer-1, groupId=processor-static-group-id] Error while fetching metadata with correlation id 3 : {src-topic=LEADER_NOT_AVAILABLE}
>>> 19/01/29 14:35:39 INFO clients.Metadata: Cluster ID: S-VDN7m3RCmgAs3vzt9aZw
>>> 19/01/29 14:35:39 INFO internals.AbstractCoordinator: [Consumer clientId=consumer-1, groupId=processor-static-group-id] Discovered group coordinator host:9092 (id: 2147483596 rack: null)
>>> 19/01/29 14:35:39 INFO internals.ConsumerCoordinator: [Consumer clientId=consumer-1, groupId=processor-static-group-id] Revoking previously assigned partitions []
>>> 19/01/29 14:35:39 INFO internals.AbstractCoordinator: [Consumer clientId=consumer-1, groupId=processor-static-group-id] (Re-)joining group
>>> 19/01/29 14:35:43 INFO internals.AbstractCoordinator: [Consumer clientId=consumer-1, groupId=processor-static-group-id] Successfully joined group with generation 1
>>> 19/01/29 14:35:43 INFO internals.ConsumerCoordinator: [Consumer clientId=consumer-1, groupId=processor-static-group-id] Setting newly assigned partitions [src-topic-0]
>>> 19/01/29 14:35:44 INFO internals.Fetcher: [Consumer clientId=consumer-1, groupId=processor-static-group-id] Resetting offset for partition src-topic-0 to offset 0.
>>> 19/01/29 14:35:46 INFO processor.KafkaProcessor$: Closing kafka consumer...
>>> 19/01/29 14:35:46 INFO processor.KafkaProcessor$: OK
Exception in thread "main" java.lang.Exception: Intentional exception to break semantics
	at com.kafka.standalone.processor.SendStrategy$class.injectFault(KafkaProcessor.scala:28)
	at com.kafka.standalone.processor.AtLeastOnceSendStrategy.injectFault(KafkaProcessor.scala:35)
	at com.kafka.standalone.processor.KafkaProcessor$.main(KafkaProcessor.scala:157)
	at com.kafka.standalone.processor.KafkaProcessor.main(KafkaProcessor.scala)
>>> 19/01/29 14:35:47 INFO processor.KafkaProcessor$: Creating config properties...
>>> 19/01/29 14:35:47 INFO processor.KafkaProcessor$: OK
>>> 19/01/29 14:35:47 INFO processor.KafkaProcessor$: Creating kafka consumer...
>>> 19/01/29 14:35:47 INFO consumer.ConsumerConfig: ConsumerConfig values:
	auto.commit.interval.ms = 5000
	auto.offset.reset = latest
	bootstrap.servers = [host:9092]
	check.crcs = true
	client.id =
	connections.max.idle.ms = 540000
	default.api.timeout.ms = 60000
	enable.auto.commit = false
	exclude.internal.topics = true
	fetch.max.bytes = 52428800
	fetch.max.wait.ms = 500
	fetch.min.bytes = 1
	group.id = processor-static-group-id
	heartbeat.interval.ms = 3000
	interceptor.classes = []
	internal.leave.group.on.close = true
	isolation.level = read_uncommitted
	key.deserializer = class org.apache.kafka.common.serialization.StringDeserializer
	max.partition.fetch.bytes = 1048576
	max.poll.interval.ms = 300000
	max.poll.records = 500
	metadata.max.age.ms = 300000
	metric.reporters = []
	metrics.num.samples = 2
	metrics.recording.level = INFO
	metrics.sample.window.ms = 30000
	partition.assignment.strategy = [class org.apache.kafka.clients.consumer.RangeAssignor]
	receive.buffer.bytes = 65536
	reconnect.backoff.max.ms = 1000
	reconnect.backoff.ms = 50
	request.timeout.ms = 30000
	retry.backoff.ms = 100
	sasl.client.callback.handler.class = null
	sasl.jaas.config = null
	sasl.kerberos.kinit.cmd = /usr/bin/kinit
	sasl.kerberos.min.time.before.relogin = 60000
	sasl.kerberos.service.name = null
	sasl.kerberos.ticket.renew.jitter = 0.05
	sasl.kerberos.ticket.renew.window.factor = 0.8
	sasl.login.callback.handler.class = null
	sasl.login.class = null
	sasl.login.refresh.buffer.seconds = 300
	sasl.login.refresh.min.period.seconds = 60
	sasl.login.refresh.window.factor = 0.8
	sasl.login.refresh.window.jitter = 0.05
	sasl.mechanism = GSSAPI
	security.protocol = PLAINTEXT
	send.buffer.bytes = 131072
	session.timeout.ms = 10000
	ssl.cipher.suites = null
	ssl.enabled.protocols = [TLSv1.2, TLSv1.1, TLSv1]
	ssl.endpoint.identification.algorithm = https
	ssl.key.password = null
	ssl.keymanager.algorithm = SunX509
	ssl.keystore.location = null
	ssl.keystore.password = null
	ssl.keystore.type = JKS
	ssl.protocol = TLS
	ssl.provider = null
	ssl.secure.random.implementation = null
	ssl.trustmanager.algorithm = PKIX
	ssl.truststore.location = null
	ssl.truststore.password = null
	ssl.truststore.type = JKS
	value.deserializer = class org.apache.kafka.common.serialization.StringDeserializer

>>> 19/01/29 14:35:47 INFO utils.AppInfoParser: Kafka version : 2.0.0
>>> 19/01/29 14:35:47 INFO utils.AppInfoParser: Kafka commitId : 3402a8361b734732
>>> 19/01/29 14:35:47 INFO processor.KafkaProcessor$: OK
>>> 19/01/29 14:35:47 INFO processor.KafkaProcessor$: Creating config properties...
>>> 19/01/29 14:35:47 INFO processor.KafkaProcessor$: OK
>>> 19/01/29 14:35:47 INFO processor.KafkaProcessor$: Creating kafka producer...
>>> 19/01/29 14:35:47 INFO producer.ProducerConfig: ProducerConfig values:
	acks = 1
	batch.size = 16384
	bootstrap.servers = [host:9092]
	buffer.memory = 33554432
	client.id =
	compression.type = none
	connections.max.idle.ms = 540000
	enable.idempotence = false
	interceptor.classes = []
	key.serializer = class org.apache.kafka.common.serialization.StringSerializer
	linger.ms = 0
	max.block.ms = 60000
	max.in.flight.requests.per.connection = 5
	max.request.size = 1048576
	metadata.max.age.ms = 300000
	metric.reporters = []
	metrics.num.samples = 2
	metrics.recording.level = INFO
	metrics.sample.window.ms = 30000
	partitioner.class = class org.apache.kafka.clients.producer.internals.DefaultPartitioner
	receive.buffer.bytes = 32768
	reconnect.backoff.max.ms = 1000
	reconnect.backoff.ms = 50
	request.timeout.ms = 30000
	retries = 0
	retry.backoff.ms = 100
	sasl.client.callback.handler.class = null
	sasl.jaas.config = null
	sasl.kerberos.kinit.cmd = /usr/bin/kinit
	sasl.kerberos.min.time.before.relogin = 60000
	sasl.kerberos.service.name = null
	sasl.kerberos.ticket.renew.jitter = 0.05
	sasl.kerberos.ticket.renew.window.factor = 0.8
	sasl.login.callback.handler.class = null
	sasl.login.class = null
	sasl.login.refresh.buffer.seconds = 300
	sasl.login.refresh.min.period.seconds = 60
	sasl.login.refresh.window.factor = 0.8
	sasl.login.refresh.window.jitter = 0.05
	sasl.mechanism = GSSAPI
	security.protocol = PLAINTEXT
	send.buffer.bytes = 131072
	ssl.cipher.suites = null
	ssl.enabled.protocols = [TLSv1.2, TLSv1.1, TLSv1]
	ssl.endpoint.identification.algorithm = https
	ssl.key.password = null
	ssl.keymanager.algorithm = SunX509
	ssl.keystore.location = null
	ssl.keystore.password = null
	ssl.keystore.type = JKS
	ssl.protocol = TLS
	ssl.provider = null
	ssl.secure.random.implementation = null
	ssl.trustmanager.algorithm = PKIX
	ssl.truststore.location = null
	ssl.truststore.password = null
	ssl.truststore.type = JKS
	transaction.timeout.ms = 60000
	transactional.id = null
	value.serializer = class org.apache.kafka.common.serialization.StringSerializer

>>> 19/01/29 14:35:47 INFO utils.AppInfoParser: Kafka version : 2.0.0
>>> 19/01/29 14:35:47 INFO utils.AppInfoParser: Kafka commitId : 3402a8361b734732
>>> 19/01/29 14:35:47 INFO processor.KafkaProcessor$: OK
>>> 19/01/29 14:35:47 INFO processor.KafkaProcessor$: Creating kafka producer send strategy...
>>> 19/01/29 14:35:47 INFO processor.KafkaProcessor$: OK
>>> 19/01/29 14:35:48 INFO clients.Metadata: Cluster ID: S-VDN7m3RCmgAs3vzt9aZw
>>> 19/01/29 14:35:48 INFO internals.AbstractCoordinator: [Consumer clientId=consumer-1, groupId=processor-static-group-id] Discovered group coordinator host:9092 (id: 2147483596 rack: null)
>>> 19/01/29 14:35:48 INFO internals.ConsumerCoordinator: [Consumer clientId=consumer-1, groupId=processor-static-group-id] Revoking previously assigned partitions []
>>> 19/01/29 14:35:48 INFO internals.AbstractCoordinator: [Consumer clientId=consumer-1, groupId=processor-static-group-id] (Re-)joining group
>>> 19/01/29 14:35:52 INFO internals.AbstractCoordinator: [Consumer clientId=consumer-1, groupId=processor-static-group-id] Successfully joined group with generation 3
>>> 19/01/29 14:35:52 INFO internals.ConsumerCoordinator: [Consumer clientId=consumer-1, groupId=processor-static-group-id] Setting newly assigned partitions [src-topic-0]
>>> 19/01/29 14:35:53 INFO processor.AtLeastOnceSendStrategy: Record: ConsumerRecord(topic = src-topic, partition = 0, offset = 0, CreateTime = 1548768949704, serialized key size = -1, serialized value size = 12, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-0)
>>> 19/01/29 14:35:54 INFO clients.Metadata: Cluster ID: S-VDN7m3RCmgAs3vzt9aZw
>>> 19/01/29 14:35:54 INFO processor.AtLeastOnceSendStrategy: Record: ConsumerRecord(topic = src-topic, partition = 0, offset = 1, CreateTime = 1548768949715, serialized key size = -1, serialized value size = 12, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-1)
>>> 19/01/29 14:35:54 INFO processor.AtLeastOnceSendStrategy: Record: ConsumerRecord(topic = src-topic, partition = 0, offset = 2, CreateTime = 1548768949716, serialized key size = -1, serialized value size = 12, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-2)
>>> 19/01/29 14:35:54 INFO processor.AtLeastOnceSendStrategy: Record: ConsumerRecord(topic = src-topic, partition = 0, offset = 3, CreateTime = 1548768949716, serialized key size = -1, serialized value size = 12, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-3)
>>> 19/01/29 14:35:54 INFO processor.AtLeastOnceSendStrategy: Record: ConsumerRecord(topic = src-topic, partition = 0, offset = 4, CreateTime = 1548768949716, serialized key size = -1, serialized value size = 12, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-4)
>>> 19/01/29 14:35:54 INFO processor.AtLeastOnceSendStrategy: Record: ConsumerRecord(topic = src-topic, partition = 0, offset = 5, CreateTime = 1548768949716, serialized key size = -1, serialized value size = 12, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-5)
>>> 19/01/29 14:35:54 INFO processor.AtLeastOnceSendStrategy: Record: ConsumerRecord(topic = src-topic, partition = 0, offset = 6, CreateTime = 1548768949717, serialized key size = -1, serialized value size = 12, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-6)
>>> 19/01/29 14:35:54 INFO processor.AtLeastOnceSendStrategy: Record: ConsumerRecord(topic = src-topic, partition = 0, offset = 7, CreateTime = 1548768949717, serialized key size = -1, serialized value size = 12, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-7)
>>> 19/01/29 14:35:54 INFO processor.AtLeastOnceSendStrategy: Record: ConsumerRecord(topic = src-topic, partition = 0, offset = 8, CreateTime = 1548768949717, serialized key size = -1, serialized value size = 12, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-8)
>>> 19/01/29 14:35:54 INFO processor.AtLeastOnceSendStrategy: Record: ConsumerRecord(topic = src-topic, partition = 0, offset = 9, CreateTime = 1548768949717, serialized key size = -1, serialized value size = 12, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-9)
>>> 19/01/29 14:35:54 INFO processor.AtLeastOnceSendStrategy: Record: ConsumerRecord(topic = src-topic, partition = 0, offset = 11, CreateTime = 1548768952034, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-10)
>>> 19/01/29 14:35:54 INFO processor.AtLeastOnceSendStrategy: Record: ConsumerRecord(topic = src-topic, partition = 0, offset = 12, CreateTime = 1548768952034, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-11)
>>> 19/01/29 14:35:54 INFO processor.AtLeastOnceSendStrategy: Record: ConsumerRecord(topic = src-topic, partition = 0, offset = 13, CreateTime = 1548768952035, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-12)
>>> 19/01/29 14:35:54 INFO processor.AtLeastOnceSendStrategy: Record: ConsumerRecord(topic = src-topic, partition = 0, offset = 14, CreateTime = 1548768952035, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-13)
>>> 19/01/29 14:35:54 INFO processor.AtLeastOnceSendStrategy: Record: ConsumerRecord(topic = src-topic, partition = 0, offset = 15, CreateTime = 1548768952035, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-14)
>>> 19/01/29 14:35:54 INFO processor.AtLeastOnceSendStrategy: Record: ConsumerRecord(topic = src-topic, partition = 0, offset = 16, CreateTime = 1548768952035, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-15)
>>> 19/01/29 14:35:54 INFO processor.AtLeastOnceSendStrategy: Record: ConsumerRecord(topic = src-topic, partition = 0, offset = 17, CreateTime = 1548768952036, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-16)
>>> 19/01/29 14:35:54 INFO processor.AtLeastOnceSendStrategy: Record: ConsumerRecord(topic = src-topic, partition = 0, offset = 18, CreateTime = 1548768952036, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-17)
>>> 19/01/29 14:35:54 INFO processor.AtLeastOnceSendStrategy: Record: ConsumerRecord(topic = src-topic, partition = 0, offset = 19, CreateTime = 1548768952036, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-18)
>>> 19/01/29 14:35:54 INFO processor.AtLeastOnceSendStrategy: Record: ConsumerRecord(topic = src-topic, partition = 0, offset = 20, CreateTime = 1548768952036, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-19)
>>> 19/01/29 14:35:54 INFO processor.AtLeastOnceSendStrategy: Record: ConsumerRecord(topic = src-topic, partition = 0, offset = 22, CreateTime = 1548768953687, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-20)
>>> 19/01/29 14:35:54 INFO processor.AtLeastOnceSendStrategy: Record: ConsumerRecord(topic = src-topic, partition = 0, offset = 23, CreateTime = 1548768953687, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-21)
>>> 19/01/29 14:35:54 INFO processor.AtLeastOnceSendStrategy: Record: ConsumerRecord(topic = src-topic, partition = 0, offset = 24, CreateTime = 1548768953687, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-22)
>>> 19/01/29 14:35:54 INFO processor.AtLeastOnceSendStrategy: Record: ConsumerRecord(topic = src-topic, partition = 0, offset = 25, CreateTime = 1548768953687, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-23)
>>> 19/01/29 14:35:54 INFO processor.AtLeastOnceSendStrategy: Record: ConsumerRecord(topic = src-topic, partition = 0, offset = 26, CreateTime = 1548768953688, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-24)
>>> 19/01/29 14:35:54 INFO processor.AtLeastOnceSendStrategy: Record: ConsumerRecord(topic = src-topic, partition = 0, offset = 27, CreateTime = 1548768953688, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-25)
>>> 19/01/29 14:35:54 INFO processor.AtLeastOnceSendStrategy: Record: ConsumerRecord(topic = src-topic, partition = 0, offset = 28, CreateTime = 1548768953688, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-26)
>>> 19/01/29 14:35:54 INFO processor.AtLeastOnceSendStrategy: Record: ConsumerRecord(topic = src-topic, partition = 0, offset = 29, CreateTime = 1548768953688, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-27)
>>> 19/01/29 14:35:54 INFO processor.AtLeastOnceSendStrategy: Record: ConsumerRecord(topic = src-topic, partition = 0, offset = 30, CreateTime = 1548768953688, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-28)
>>> 19/01/29 14:35:54 INFO processor.AtLeastOnceSendStrategy: Record: ConsumerRecord(topic = src-topic, partition = 0, offset = 31, CreateTime = 1548768953689, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-29)
>>> 19/01/29 14:35:55 INFO processor.AtLeastOnceSendStrategy: Message 0 sent to topic dst-topic in partition 0 at offset 0
>>> 19/01/29 14:35:55 INFO processor.AtLeastOnceSendStrategy: Message 1 sent to topic dst-topic in partition 0 at offset 1
>>> 19/01/29 14:35:55 INFO processor.AtLeastOnceSendStrategy: Message 2 sent to topic dst-topic in partition 0 at offset 2
>>> 19/01/29 14:35:55 INFO processor.AtLeastOnceSendStrategy: Message 3 sent to topic dst-topic in partition 0 at offset 3
>>> 19/01/29 14:35:55 INFO processor.AtLeastOnceSendStrategy: Message 4 sent to topic dst-topic in partition 0 at offset 4
>>> 19/01/29 14:35:55 INFO processor.AtLeastOnceSendStrategy: Message 5 sent to topic dst-topic in partition 0 at offset 5
>>> 19/01/29 14:35:55 INFO processor.AtLeastOnceSendStrategy: Message 6 sent to topic dst-topic in partition 0 at offset 6
>>> 19/01/29 14:35:55 INFO processor.AtLeastOnceSendStrategy: Message 7 sent to topic dst-topic in partition 0 at offset 7
>>> 19/01/29 14:35:55 INFO processor.AtLeastOnceSendStrategy: Message 8 sent to topic dst-topic in partition 0 at offset 8
>>> 19/01/29 14:35:55 INFO processor.AtLeastOnceSendStrategy: Message 9 sent to topic dst-topic in partition 0 at offset 9
>>> 19/01/29 14:35:55 INFO processor.AtLeastOnceSendStrategy: Message 11 sent to topic dst-topic in partition 0 at offset 10
>>> 19/01/29 14:35:55 INFO processor.AtLeastOnceSendStrategy: Message 12 sent to topic dst-topic in partition 0 at offset 11
>>> 19/01/29 14:35:55 INFO processor.AtLeastOnceSendStrategy: Message 13 sent to topic dst-topic in partition 0 at offset 12
>>> 19/01/29 14:35:55 INFO processor.AtLeastOnceSendStrategy: Message 14 sent to topic dst-topic in partition 0 at offset 13
>>> 19/01/29 14:35:55 INFO processor.AtLeastOnceSendStrategy: Message 15 sent to topic dst-topic in partition 0 at offset 14
>>> 19/01/29 14:35:55 INFO processor.AtLeastOnceSendStrategy: Message 16 sent to topic dst-topic in partition 0 at offset 15
>>> 19/01/29 14:35:55 INFO processor.AtLeastOnceSendStrategy: Message 17 sent to topic dst-topic in partition 0 at offset 16
>>> 19/01/29 14:35:55 INFO processor.AtLeastOnceSendStrategy: Message 18 sent to topic dst-topic in partition 0 at offset 17
>>> 19/01/29 14:35:55 INFO processor.AtLeastOnceSendStrategy: Message 19 sent to topic dst-topic in partition 0 at offset 18
>>> 19/01/29 14:35:55 INFO processor.AtLeastOnceSendStrategy: Message 20 sent to topic dst-topic in partition 0 at offset 19
>>> 19/01/29 14:35:55 INFO processor.AtLeastOnceSendStrategy: Message 22 sent to topic dst-topic in partition 0 at offset 20
>>> 19/01/29 14:35:55 INFO processor.AtLeastOnceSendStrategy: Message 23 sent to topic dst-topic in partition 0 at offset 21
>>> 19/01/29 14:35:55 INFO processor.AtLeastOnceSendStrategy: Message 24 sent to topic dst-topic in partition 0 at offset 22
>>> 19/01/29 14:35:55 INFO processor.AtLeastOnceSendStrategy: Message 25 sent to topic dst-topic in partition 0 at offset 23
>>> 19/01/29 14:35:55 INFO processor.AtLeastOnceSendStrategy: Message 26 sent to topic dst-topic in partition 0 at offset 24
>>> 19/01/29 14:35:55 INFO processor.AtLeastOnceSendStrategy: Message 27 sent to topic dst-topic in partition 0 at offset 25
>>> 19/01/29 14:35:55 INFO processor.AtLeastOnceSendStrategy: Message 28 sent to topic dst-topic in partition 0 at offset 26
>>> 19/01/29 14:35:55 INFO processor.AtLeastOnceSendStrategy: Message 29 sent to topic dst-topic in partition 0 at offset 27
>>> 19/01/29 14:35:55 INFO processor.AtLeastOnceSendStrategy: Message 30 sent to topic dst-topic in partition 0 at offset 28
>>> 19/01/29 14:35:55 INFO processor.AtLeastOnceSendStrategy: Message 31 sent to topic dst-topic in partition 0 at offset 29
>>> 19/01/29 14:35:55 INFO processor.AtLeastOnceSendStrategy: Record: ConsumerRecord(topic = src-topic, partition = 0, offset = 33, CreateTime = 1548768955339, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-30)
>>> 19/01/29 14:35:55 INFO processor.AtLeastOnceSendStrategy: Record: ConsumerRecord(topic = src-topic, partition = 0, offset = 34, CreateTime = 1548768955340, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-31)
>>> 19/01/29 14:35:55 INFO processor.AtLeastOnceSendStrategy: Record: ConsumerRecord(topic = src-topic, partition = 0, offset = 35, CreateTime = 1548768955340, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-32)
>>> 19/01/29 14:35:55 INFO processor.AtLeastOnceSendStrategy: Record: ConsumerRecord(topic = src-topic, partition = 0, offset = 36, CreateTime = 1548768955340, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-33)
>>> 19/01/29 14:35:55 INFO processor.AtLeastOnceSendStrategy: Record: ConsumerRecord(topic = src-topic, partition = 0, offset = 37, CreateTime = 1548768955341, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-34)
>>> 19/01/29 14:35:55 INFO processor.AtLeastOnceSendStrategy: Record: ConsumerRecord(topic = src-topic, partition = 0, offset = 38, CreateTime = 1548768955341, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-35)
>>> 19/01/29 14:35:55 INFO processor.AtLeastOnceSendStrategy: Record: ConsumerRecord(topic = src-topic, partition = 0, offset = 39, CreateTime = 1548768955341, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-36)
>>> 19/01/29 14:35:55 INFO processor.AtLeastOnceSendStrategy: Record: ConsumerRecord(topic = src-topic, partition = 0, offset = 40, CreateTime = 1548768955341, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-37)
>>> 19/01/29 14:35:55 INFO processor.AtLeastOnceSendStrategy: Record: ConsumerRecord(topic = src-topic, partition = 0, offset = 41, CreateTime = 1548768955342, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-38)
>>> 19/01/29 14:35:55 INFO processor.AtLeastOnceSendStrategy: Record: ConsumerRecord(topic = src-topic, partition = 0, offset = 42, CreateTime = 1548768955342, serialized key size = -1, serialized value size = 13, headers = RecordHeaders(headers = [], isReadOnly = false), key = null, value = streamtest-39)
>>> 19/01/29 14:35:55 INFO processor.KafkaProcessor$: Closing kafka consumer...
>>> 19/01/29 14:35:55 INFO processor.AtLeastOnceSendStrategy: Message 33 sent to topic dst-topic in partition 0 at offset 30
>>> 19/01/29 14:35:55 INFO processor.AtLeastOnceSendStrategy: Message 34 sent to topic dst-topic in partition 0 at offset 31
>>> 19/01/29 14:35:55 INFO processor.AtLeastOnceSendStrategy: Message 35 sent to topic dst-topic in partition 0 at offset 32
>>> 19/01/29 14:35:55 INFO processor.AtLeastOnceSendStrategy: Message 36 sent to topic dst-topic in partition 0 at offset 33
>>> 19/01/29 14:35:55 INFO processor.AtLeastOnceSendStrategy: Message 37 sent to topic dst-topic in partition 0 at offset 34
>>> 19/01/29 14:35:55 INFO processor.AtLeastOnceSendStrategy: Message 38 sent to topic dst-topic in partition 0 at offset 35
>>> 19/01/29 14:35:55 INFO processor.AtLeastOnceSendStrategy: Message 39 sent to topic dst-topic in partition 0 at offset 36
>>> 19/01/29 14:35:55 INFO processor.AtLeastOnceSendStrategy: Message 40 sent to topic dst-topic in partition 0 at offset 37
>>> 19/01/29 14:35:55 INFO processor.AtLeastOnceSendStrategy: Message 41 sent to topic dst-topic in partition 0 at offset 38
>>> 19/01/29 14:35:55 INFO processor.AtLeastOnceSendStrategy: Message 42 sent to topic dst-topic in partition 0 at offset 39
>>> 19/01/29 14:35:55 INFO processor.KafkaProcessor$: OK
Exception in thread "main" java.lang.Exception: Intentional exception to break semantics
	at com.kafka.standalone.processor.SendStrategy$class.injectFault(KafkaProcessor.scala:28)
	at com.kafka.standalone.processor.AtLeastOnceSendStrategy.injectFault(KafkaProcessor.scala:35)
	at com.kafka.standalone.processor.KafkaProcessor$.main(KafkaProcessor.scala:157)
	at com.kafka.standalone.processor.KafkaProcessor.main(KafkaProcessor.scala)
...
```

producer-console
```
$ ./start-producer.sh
>>> 19/01/29 14:35:47 INFO producer.KafkaProducer$: Creating config properties...
>>> 19/01/29 14:35:47 INFO producer.KafkaProducer$: OK
>>> 19/01/29 14:35:47 INFO producer.KafkaProducer$: Creating kafka producer...
>>> 19/01/29 14:35:47 INFO producer.ProducerConfig: ProducerConfig values:
	acks = 1
	batch.size = 16384
	bootstrap.servers = [host:9092]
	buffer.memory = 33554432
	client.id =
	compression.type = none
	connections.max.idle.ms = 540000
	enable.idempotence = true
	interceptor.classes = []
	key.serializer = class org.apache.kafka.common.serialization.StringSerializer
	linger.ms = 0
	max.block.ms = 60000
	max.in.flight.requests.per.connection = 5
	max.request.size = 1048576
	metadata.max.age.ms = 300000
	metric.reporters = []
	metrics.num.samples = 2
	metrics.recording.level = INFO
	metrics.sample.window.ms = 30000
	partitioner.class = class org.apache.kafka.clients.producer.internals.DefaultPartitioner
	receive.buffer.bytes = 32768
	reconnect.backoff.max.ms = 1000
	reconnect.backoff.ms = 50
	request.timeout.ms = 30000
	retries = 0
	retry.backoff.ms = 100
	sasl.client.callback.handler.class = null
	sasl.jaas.config = null
	sasl.kerberos.kinit.cmd = /usr/bin/kinit
	sasl.kerberos.min.time.before.relogin = 60000
	sasl.kerberos.service.name = null
	sasl.kerberos.ticket.renew.jitter = 0.05
	sasl.kerberos.ticket.renew.window.factor = 0.8
	sasl.login.callback.handler.class = null
	sasl.login.class = null
	sasl.login.refresh.buffer.seconds = 300
	sasl.login.refresh.min.period.seconds = 60
	sasl.login.refresh.window.factor = 0.8
	sasl.login.refresh.window.jitter = 0.05
	sasl.mechanism = GSSAPI
	security.protocol = PLAINTEXT
	send.buffer.bytes = 131072
	ssl.cipher.suites = null
	ssl.enabled.protocols = [TLSv1.2, TLSv1.1, TLSv1]
	ssl.endpoint.identification.algorithm = https
	ssl.key.password = null
	ssl.keymanager.algorithm = SunX509
	ssl.keystore.location = null
	ssl.keystore.password = null
	ssl.keystore.type = JKS
	ssl.protocol = TLS
	ssl.provider = null
	ssl.secure.random.implementation = null
	ssl.trustmanager.algorithm = PKIX
	ssl.truststore.location = null
	ssl.truststore.password = null
	ssl.truststore.type = JKS
	transaction.timeout.ms = 60000
	transactional.id = producer-static-transactional-id
	value.serializer = class org.apache.kafka.common.serialization.StringSerializer

>>> 19/01/29 14:35:47 INFO producer.KafkaProducer: [Producer clientId=producer-1, transactionalId=producer-static-transactional-id] Instantiated a transactional producer.
>>> 19/01/29 14:35:47 INFO producer.KafkaProducer: [Producer clientId=producer-1, transactionalId=producer-static-transactional-id] Overriding the default retries config to the recommended value of 2147483647 since the idempotent producer is enabled.
>>> 19/01/29 14:35:47 INFO producer.KafkaProducer: [Producer clientId=producer-1, transactionalId=producer-static-transactional-id] Overriding the default acks to all since idempotence is enabled.
>>> 19/01/29 14:35:47 INFO utils.AppInfoParser: Kafka version : 2.0.0
>>> 19/01/29 14:35:47 INFO utils.AppInfoParser: Kafka commitId : 3402a8361b734732
>>> 19/01/29 14:35:47 INFO internals.TransactionManager: [Producer clientId=producer-1, transactionalId=producer-static-transactional-id] ProducerId set to -1 with epoch -1
>>> 19/01/29 14:35:49 INFO internals.TransactionManager: [Producer clientId=producer-1, transactionalId=producer-static-transactional-id] ProducerId set to 0 with epoch 45
>>> 19/01/29 14:35:49 INFO producer.KafkaProducer$: OK
>>> 19/01/29 14:35:49 INFO clients.Metadata: Cluster ID: S-VDN7m3RCmgAs3vzt9aZw
>>> 19/01/29 14:35:49 INFO producer.KafkaProducer$: Record: ProducerRecord(topic=src-topic, partition=null, headers=RecordHeaders(headers = [], isReadOnly = true), key=null, value=streamtest-0, timestamp=null)
>>> 19/01/29 14:35:49 INFO producer.KafkaProducer$: Record: ProducerRecord(topic=src-topic, partition=null, headers=RecordHeaders(headers = [], isReadOnly = true), key=null, value=streamtest-1, timestamp=null)
>>> 19/01/29 14:35:49 INFO producer.KafkaProducer$: Record: ProducerRecord(topic=src-topic, partition=null, headers=RecordHeaders(headers = [], isReadOnly = true), key=null, value=streamtest-2, timestamp=null)
>>> 19/01/29 14:35:49 INFO producer.KafkaProducer$: Record: ProducerRecord(topic=src-topic, partition=null, headers=RecordHeaders(headers = [], isReadOnly = true), key=null, value=streamtest-3, timestamp=null)
>>> 19/01/29 14:35:49 INFO producer.KafkaProducer$: Record: ProducerRecord(topic=src-topic, partition=null, headers=RecordHeaders(headers = [], isReadOnly = true), key=null, value=streamtest-4, timestamp=null)
>>> 19/01/29 14:35:49 INFO producer.KafkaProducer$: Record: ProducerRecord(topic=src-topic, partition=null, headers=RecordHeaders(headers = [], isReadOnly = true), key=null, value=streamtest-5, timestamp=null)
>>> 19/01/29 14:35:49 INFO producer.KafkaProducer$: Record: ProducerRecord(topic=src-topic, partition=null, headers=RecordHeaders(headers = [], isReadOnly = true), key=null, value=streamtest-6, timestamp=null)
>>> 19/01/29 14:35:49 INFO producer.KafkaProducer$: Record: ProducerRecord(topic=src-topic, partition=null, headers=RecordHeaders(headers = [], isReadOnly = true), key=null, value=streamtest-7, timestamp=null)
>>> 19/01/29 14:35:49 INFO producer.KafkaProducer$: Record: ProducerRecord(topic=src-topic, partition=null, headers=RecordHeaders(headers = [], isReadOnly = true), key=null, value=streamtest-8, timestamp=null)
>>> 19/01/29 14:35:49 INFO producer.KafkaProducer$: Record: ProducerRecord(topic=src-topic, partition=null, headers=RecordHeaders(headers = [], isReadOnly = true), key=null, value=streamtest-9, timestamp=null)
>>> 19/01/29 14:35:49 INFO producer.KafkaProducer$: Commit
>>> 19/01/29 14:35:50 INFO producer.KafkaProducer$: Message 0 sent to topic src-topic in partition 0 at offset 0
>>> 19/01/29 14:35:50 INFO producer.KafkaProducer$: Message 1 sent to topic src-topic in partition 0 at offset 1
>>> 19/01/29 14:35:50 INFO producer.KafkaProducer$: Message 2 sent to topic src-topic in partition 0 at offset 2
>>> 19/01/29 14:35:50 INFO producer.KafkaProducer$: Message 3 sent to topic src-topic in partition 0 at offset 3
>>> 19/01/29 14:35:50 INFO producer.KafkaProducer$: Message 4 sent to topic src-topic in partition 0 at offset 4
>>> 19/01/29 14:35:50 INFO producer.KafkaProducer$: Message 5 sent to topic src-topic in partition 0 at offset 5
>>> 19/01/29 14:35:50 INFO producer.KafkaProducer$: Message 6 sent to topic src-topic in partition 0 at offset 6
>>> 19/01/29 14:35:50 INFO producer.KafkaProducer$: Message 7 sent to topic src-topic in partition 0 at offset 7
>>> 19/01/29 14:35:50 INFO producer.KafkaProducer$: Message 8 sent to topic src-topic in partition 0 at offset 8
>>> 19/01/29 14:35:50 INFO producer.KafkaProducer$: Message 9 sent to topic src-topic in partition 0 at offset 9
>>> 19/01/29 14:35:52 INFO producer.KafkaProducer$: Record: ProducerRecord(topic=src-topic, partition=null, headers=RecordHeaders(headers = [], isReadOnly = true), key=null, value=streamtest-10, timestamp=null)
>>> 19/01/29 14:35:52 INFO producer.KafkaProducer$: Record: ProducerRecord(topic=src-topic, partition=null, headers=RecordHeaders(headers = [], isReadOnly = true), key=null, value=streamtest-11, timestamp=null)
>>> 19/01/29 14:35:52 INFO producer.KafkaProducer$: Record: ProducerRecord(topic=src-topic, partition=null, headers=RecordHeaders(headers = [], isReadOnly = true), key=null, value=streamtest-12, timestamp=null)
>>> 19/01/29 14:35:52 INFO producer.KafkaProducer$: Record: ProducerRecord(topic=src-topic, partition=null, headers=RecordHeaders(headers = [], isReadOnly = true), key=null, value=streamtest-13, timestamp=null)
>>> 19/01/29 14:35:52 INFO producer.KafkaProducer$: Record: ProducerRecord(topic=src-topic, partition=null, headers=RecordHeaders(headers = [], isReadOnly = true), key=null, value=streamtest-14, timestamp=null)
>>> 19/01/29 14:35:52 INFO producer.KafkaProducer$: Record: ProducerRecord(topic=src-topic, partition=null, headers=RecordHeaders(headers = [], isReadOnly = true), key=null, value=streamtest-15, timestamp=null)
>>> 19/01/29 14:35:52 INFO producer.KafkaProducer$: Record: ProducerRecord(topic=src-topic, partition=null, headers=RecordHeaders(headers = [], isReadOnly = true), key=null, value=streamtest-16, timestamp=null)
>>> 19/01/29 14:35:52 INFO producer.KafkaProducer$: Record: ProducerRecord(topic=src-topic, partition=null, headers=RecordHeaders(headers = [], isReadOnly = true), key=null, value=streamtest-17, timestamp=null)
>>> 19/01/29 14:35:52 INFO producer.KafkaProducer$: Record: ProducerRecord(topic=src-topic, partition=null, headers=RecordHeaders(headers = [], isReadOnly = true), key=null, value=streamtest-18, timestamp=null)
>>> 19/01/29 14:35:52 INFO producer.KafkaProducer$: Record: ProducerRecord(topic=src-topic, partition=null, headers=RecordHeaders(headers = [], isReadOnly = true), key=null, value=streamtest-19, timestamp=null)
>>> 19/01/29 14:35:52 INFO producer.KafkaProducer$: Commit
>>> 19/01/29 14:35:52 INFO producer.KafkaProducer$: Message 10 sent to topic src-topic in partition 0 at offset 11
>>> 19/01/29 14:35:52 INFO producer.KafkaProducer$: Message 11 sent to topic src-topic in partition 0 at offset 12
>>> 19/01/29 14:35:52 INFO producer.KafkaProducer$: Message 12 sent to topic src-topic in partition 0 at offset 13
>>> 19/01/29 14:35:52 INFO producer.KafkaProducer$: Message 13 sent to topic src-topic in partition 0 at offset 14
>>> 19/01/29 14:35:52 INFO producer.KafkaProducer$: Message 14 sent to topic src-topic in partition 0 at offset 15
>>> 19/01/29 14:35:52 INFO producer.KafkaProducer$: Message 15 sent to topic src-topic in partition 0 at offset 16
>>> 19/01/29 14:35:52 INFO producer.KafkaProducer$: Message 16 sent to topic src-topic in partition 0 at offset 17
>>> 19/01/29 14:35:52 INFO producer.KafkaProducer$: Message 17 sent to topic src-topic in partition 0 at offset 18
>>> 19/01/29 14:35:52 INFO producer.KafkaProducer$: Message 18 sent to topic src-topic in partition 0 at offset 19
>>> 19/01/29 14:35:52 INFO producer.KafkaProducer$: Message 19 sent to topic src-topic in partition 0 at offset 20
>>> 19/01/29 14:35:53 INFO producer.KafkaProducer$: Record: ProducerRecord(topic=src-topic, partition=null, headers=RecordHeaders(headers = [], isReadOnly = true), key=null, value=streamtest-20, timestamp=null)
>>> 19/01/29 14:35:53 INFO producer.KafkaProducer$: Record: ProducerRecord(topic=src-topic, partition=null, headers=RecordHeaders(headers = [], isReadOnly = true), key=null, value=streamtest-21, timestamp=null)
>>> 19/01/29 14:35:53 INFO producer.KafkaProducer$: Record: ProducerRecord(topic=src-topic, partition=null, headers=RecordHeaders(headers = [], isReadOnly = true), key=null, value=streamtest-22, timestamp=null)
>>> 19/01/29 14:35:53 INFO producer.KafkaProducer$: Record: ProducerRecord(topic=src-topic, partition=null, headers=RecordHeaders(headers = [], isReadOnly = true), key=null, value=streamtest-23, timestamp=null)
>>> 19/01/29 14:35:53 INFO producer.KafkaProducer$: Record: ProducerRecord(topic=src-topic, partition=null, headers=RecordHeaders(headers = [], isReadOnly = true), key=null, value=streamtest-24, timestamp=null)
>>> 19/01/29 14:35:53 INFO producer.KafkaProducer$: Record: ProducerRecord(topic=src-topic, partition=null, headers=RecordHeaders(headers = [], isReadOnly = true), key=null, value=streamtest-25, timestamp=null)
>>> 19/01/29 14:35:53 INFO producer.KafkaProducer$: Record: ProducerRecord(topic=src-topic, partition=null, headers=RecordHeaders(headers = [], isReadOnly = true), key=null, value=streamtest-26, timestamp=null)
>>> 19/01/29 14:35:53 INFO producer.KafkaProducer$: Record: ProducerRecord(topic=src-topic, partition=null, headers=RecordHeaders(headers = [], isReadOnly = true), key=null, value=streamtest-27, timestamp=null)
>>> 19/01/29 14:35:53 INFO producer.KafkaProducer$: Record: ProducerRecord(topic=src-topic, partition=null, headers=RecordHeaders(headers = [], isReadOnly = true), key=null, value=streamtest-28, timestamp=null)
>>> 19/01/29 14:35:53 INFO producer.KafkaProducer$: Record: ProducerRecord(topic=src-topic, partition=null, headers=RecordHeaders(headers = [], isReadOnly = true), key=null, value=streamtest-29, timestamp=null)
>>> 19/01/29 14:35:53 INFO producer.KafkaProducer$: Commit
>>> 19/01/29 14:35:54 INFO producer.KafkaProducer$: Message 20 sent to topic src-topic in partition 0 at offset 22
>>> 19/01/29 14:35:54 INFO producer.KafkaProducer$: Message 21 sent to topic src-topic in partition 0 at offset 23
>>> 19/01/29 14:35:54 INFO producer.KafkaProducer$: Message 22 sent to topic src-topic in partition 0 at offset 24
>>> 19/01/29 14:35:54 INFO producer.KafkaProducer$: Message 23 sent to topic src-topic in partition 0 at offset 25
>>> 19/01/29 14:35:54 INFO producer.KafkaProducer$: Message 24 sent to topic src-topic in partition 0 at offset 26
>>> 19/01/29 14:35:54 INFO producer.KafkaProducer$: Message 25 sent to topic src-topic in partition 0 at offset 27
>>> 19/01/29 14:35:54 INFO producer.KafkaProducer$: Message 26 sent to topic src-topic in partition 0 at offset 28
>>> 19/01/29 14:35:54 INFO producer.KafkaProducer$: Message 27 sent to topic src-topic in partition 0 at offset 29
>>> 19/01/29 14:35:54 INFO producer.KafkaProducer$: Message 28 sent to topic src-topic in partition 0 at offset 30
>>> 19/01/29 14:35:54 INFO producer.KafkaProducer$: Message 29 sent to topic src-topic in partition 0 at offset 31
>>> 19/01/29 14:35:55 INFO producer.KafkaProducer$: Record: ProducerRecord(topic=src-topic, partition=null, headers=RecordHeaders(headers = [], isReadOnly = true), key=null, value=streamtest-30, timestamp=null)
>>> 19/01/29 14:35:55 INFO producer.KafkaProducer$: Record: ProducerRecord(topic=src-topic, partition=null, headers=RecordHeaders(headers = [], isReadOnly = true), key=null, value=streamtest-31, timestamp=null)
>>> 19/01/29 14:35:55 INFO producer.KafkaProducer$: Record: ProducerRecord(topic=src-topic, partition=null, headers=RecordHeaders(headers = [], isReadOnly = true), key=null, value=streamtest-32, timestamp=null)
>>> 19/01/29 14:35:55 INFO producer.KafkaProducer$: Record: ProducerRecord(topic=src-topic, partition=null, headers=RecordHeaders(headers = [], isReadOnly = true), key=null, value=streamtest-33, timestamp=null)
>>> 19/01/29 14:35:55 INFO producer.KafkaProducer$: Record: ProducerRecord(topic=src-topic, partition=null, headers=RecordHeaders(headers = [], isReadOnly = true), key=null, value=streamtest-34, timestamp=null)
>>> 19/01/29 14:35:55 INFO producer.KafkaProducer$: Record: ProducerRecord(topic=src-topic, partition=null, headers=RecordHeaders(headers = [], isReadOnly = true), key=null, value=streamtest-35, timestamp=null)
>>> 19/01/29 14:35:55 INFO producer.KafkaProducer$: Record: ProducerRecord(topic=src-topic, partition=null, headers=RecordHeaders(headers = [], isReadOnly = true), key=null, value=streamtest-36, timestamp=null)
>>> 19/01/29 14:35:55 INFO producer.KafkaProducer$: Record: ProducerRecord(topic=src-topic, partition=null, headers=RecordHeaders(headers = [], isReadOnly = true), key=null, value=streamtest-37, timestamp=null)
>>> 19/01/29 14:35:55 INFO producer.KafkaProducer$: Record: ProducerRecord(topic=src-topic, partition=null, headers=RecordHeaders(headers = [], isReadOnly = true), key=null, value=streamtest-38, timestamp=null)
>>> 19/01/29 14:35:55 INFO producer.KafkaProducer$: Record: ProducerRecord(topic=src-topic, partition=null, headers=RecordHeaders(headers = [], isReadOnly = true), key=null, value=streamtest-39, timestamp=null)
>>> 19/01/29 14:35:55 INFO producer.KafkaProducer$: Commit
>>> 19/01/29 14:35:55 INFO producer.KafkaProducer$: Message 30 sent to topic src-topic in partition 0 at offset 33
>>> 19/01/29 14:35:55 INFO producer.KafkaProducer$: Message 31 sent to topic src-topic in partition 0 at offset 34
>>> 19/01/29 14:35:55 INFO producer.KafkaProducer$: Message 32 sent to topic src-topic in partition 0 at offset 35
>>> 19/01/29 14:35:55 INFO producer.KafkaProducer$: Message 33 sent to topic src-topic in partition 0 at offset 36
>>> 19/01/29 14:35:55 INFO producer.KafkaProducer$: Message 34 sent to topic src-topic in partition 0 at offset 37
>>> 19/01/29 14:35:55 INFO producer.KafkaProducer$: Message 35 sent to topic src-topic in partition 0 at offset 38
>>> 19/01/29 14:35:55 INFO producer.KafkaProducer$: Message 36 sent to topic src-topic in partition 0 at offset 39
>>> 19/01/29 14:35:55 INFO producer.KafkaProducer$: Message 37 sent to topic src-topic in partition 0 at offset 40
>>> 19/01/29 14:35:55 INFO producer.KafkaProducer$: Message 38 sent to topic src-topic in partition 0 at offset 41
>>> 19/01/29 14:35:55 INFO producer.KafkaProducer$: Message 39 sent to topic src-topic in partition 0 at offset 42
>>> 19/01/29 14:35:56 INFO producer.KafkaProducer$: Record: ProducerRecord(topic=src-topic, partition=null, headers=RecordHeaders(headers = [], isReadOnly = true), key=null, value=streamtest-40, timestamp=null)
>>> 19/01/29 14:35:56 INFO producer.KafkaProducer$: Record: ProducerRecord(topic=src-topic, partition=null, headers=RecordHeaders(headers = [], isReadOnly = true), key=null, value=streamtest-41, timestamp=null)
>>> 19/01/29 14:35:56 INFO producer.KafkaProducer$: Record: ProducerRecord(topic=src-topic, partition=null, headers=RecordHeaders(headers = [], isReadOnly = true), key=null, value=streamtest-42, timestamp=null)
>>> 19/01/29 14:35:56 INFO producer.KafkaProducer$: Record: ProducerRecord(topic=src-topic, partition=null, headers=RecordHeaders(headers = [], isReadOnly = true), key=null, value=streamtest-43, timestamp=null)
>>> 19/01/29 14:35:56 INFO producer.KafkaProducer$: Record: ProducerRecord(topic=src-topic, partition=null, headers=RecordHeaders(headers = [], isReadOnly = true), key=null, value=streamtest-44, timestamp=null)
>>> 19/01/29 14:35:56 INFO producer.KafkaProducer$: Record: ProducerRecord(topic=src-topic, partition=null, headers=RecordHeaders(headers = [], isReadOnly = true), key=null, value=streamtest-45, timestamp=null)
>>> 19/01/29 14:35:56 INFO producer.KafkaProducer$: Record: ProducerRecord(topic=src-topic, partition=null, headers=RecordHeaders(headers = [], isReadOnly = true), key=null, value=streamtest-46, timestamp=null)
>>> 19/01/29 14:35:56 INFO producer.KafkaProducer$: Record: ProducerRecord(topic=src-topic, partition=null, headers=RecordHeaders(headers = [], isReadOnly = true), key=null, value=streamtest-47, timestamp=null)
>>> 19/01/29 14:35:56 INFO producer.KafkaProducer$: Record: ProducerRecord(topic=src-topic, partition=null, headers=RecordHeaders(headers = [], isReadOnly = true), key=null, value=streamtest-48, timestamp=null)
>>> 19/01/29 14:35:56 INFO producer.KafkaProducer$: Record: ProducerRecord(topic=src-topic, partition=null, headers=RecordHeaders(headers = [], isReadOnly = true), key=null, value=streamtest-49, timestamp=null)
>>> 19/01/29 14:35:56 INFO producer.KafkaProducer$: Commit
>>> 19/01/29 14:35:57 INFO producer.KafkaProducer$: Message 40 sent to topic src-topic in partition 0 at offset 44
>>> 19/01/29 14:35:57 INFO producer.KafkaProducer$: Message 41 sent to topic src-topic in partition 0 at offset 45
>>> 19/01/29 14:35:57 INFO producer.KafkaProducer$: Message 42 sent to topic src-topic in partition 0 at offset 46
>>> 19/01/29 14:35:57 INFO producer.KafkaProducer$: Message 43 sent to topic src-topic in partition 0 at offset 47
>>> 19/01/29 14:35:57 INFO producer.KafkaProducer$: Message 44 sent to topic src-topic in partition 0 at offset 48
>>> 19/01/29 14:35:57 INFO producer.KafkaProducer$: Message 45 sent to topic src-topic in partition 0 at offset 49
>>> 19/01/29 14:35:57 INFO producer.KafkaProducer$: Message 46 sent to topic src-topic in partition 0 at offset 50
>>> 19/01/29 14:35:57 INFO producer.KafkaProducer$: Message 47 sent to topic src-topic in partition 0 at offset 51
>>> 19/01/29 14:35:57 INFO producer.KafkaProducer$: Message 48 sent to topic src-topic in partition 0 at offset 52
...
```
