# kafka-message-seeker
 
This application will start a kafka consumer that will scan a topic looking for the a string.
To run the application you need the jar (you can build or download it from [releases](https://github.com/LFilips/kafka-message-seeker/releases)) with these options:

```
Usage: kafkaMessage seeker [options]

  -t, --topic <value>      The topic for which seek the message
  -s, --search-for <value>
                           The string that will be searched in topic
  -o, --offset <value>     The offset to start with (for every partition!)
```

For example if you want to find all the messages that contains the string "hello" starting from
offset 1000 on the topic "test_topic" you should run in this way:

`java -jar kafka-message-seeker.jar --topic test_topic --offset 1000 --search-for hello`

### Build your jar:

You can build your own jar using sbt:
```
sbt clean assembly
```

This will run tests as well (will take less than a minute). You can skip test in this way:

```
sbt "set test in assembly := {}" clean assembly
```