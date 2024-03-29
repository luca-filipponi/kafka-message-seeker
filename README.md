# kafka-message-seeker
 
This application will start a kafka consumer that will scan a topic looking for the specified string. It is useful when you have a fragment of a kafka message and you want to find the whole together with its offset and partition.
To run the application you need the jar (you can build or download it from [releases](https://github.com/LFilips/kafka-message-seeker/releases)).

For example:
```
wget -O kafka-message-seeker-0.1.0.jar https://github.com/LFilips/kafka-message-seeker/releases/download/v0.1.0/kafka-message-seeker-0.1.0.jar
```

### How to use
```
Usage: kafkaMessage seeker [options]

  -t, --topic <value>      The topic for which seek the message
  -s, --search-for <value>
                           The string that will be searched in topic
  -o, --offset <value>     The offset to start with (for every partition!)
```

For example if you want to find all the messages containing the string "hello" starting from
offset 1000 on the topic "test_topic" you have to use these args:

`java -jar kafka-message-seeker.jar --topic test_topic --offset 1000 --search-for hello`

If you'd like to scan the whole topic just use 0 as offset. Be aware that scanning an entire kafka topic 
can take long time (depending on kafka message retention policy).

If a message is found will be printed in the console the record metadata and the record value, 
for example:

```
12:53:23.718 - I've found a match!
 {Key: null
 Offset: 3
 Partition: 0
 Value: hello}
```

value is the whole kafka message (not just the matched string).

Every 20 seconds there will be an update about the current offset/partition.

### Build your jar:

You can build your own jar using sbt:
```
sbt clean assembly
```

This will run tests as well (will take less than a minute). 

You can skip test in this way (but you shouldn't):

```
sbt "set test in assembly := {}" clean assembly
```

### Known issues

If the kafka-message-seeker can't connect to the broker will timeout after a while this error:

```
Exception in thread "main" org.apache.kafka.common.errors.TimeoutException: Timeout expired while fetching topic metadata
```
