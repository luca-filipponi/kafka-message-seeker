package com.filipponi.seeker

import java.time.Duration
import java.util.{Collections, Properties}

import com.filipponi.seeker.CommandLineParser.{Config, kafkaMsgSeekerArgsParser}
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord, ConsumerRecords, KafkaConsumer}
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}
import org.slf4j.{Logger, LoggerFactory}
import scopt.OParser

import scala.collection.JavaConverters._

object MsgSeeker extends App  {

  private val logger = LoggerFactory.getLogger(getClass)

  seek(logger = logger,args)

  private[seeker] def seek(logger: Logger, args: Array[String]) = {
    OParser.parse(kafkaMsgSeekerArgsParser, args, Config.empty()) match {
      case Some(config) =>

        logger.info(s"Searching for string: ${config.stringToSeek}, from offset: ${config.offset} on topic: ${config.topic}")

        val consumer = createConsumer(config.brokers)

        consumer.subscribe(Collections.singletonList(config.topic))

        val partitionInfos = consumer.partitionsFor(config.topic).asScala

        //this poll does the trick to assign all the partition to this consumer, otherwise i can't seek.
        consumer.poll(Duration.ofSeconds(1))

        partitionInfos.foreach { partitionInfo =>
          consumer.seek(new TopicPartition(partitionInfo.topic(), partitionInfo.partition()), config.offset)
        }

        var moreMessages = true //this is the way to stop cycle from odersky, but i don't really like it

        var timer = System.currentTimeMillis()

        while (moreMessages) {

          val records: ConsumerRecords[String, Array[Byte]] = consumer.poll(Duration.ofSeconds(1))
          if (records.isEmpty) moreMessages = false
          val iterator = records.iterator()
          while (iterator.hasNext) {
            val record: ConsumerRecord[String, Array[Byte]] = iterator.next()

            val value = new String(record.value())

            if (value.contains(config.stringToSeek)) {
              logger.info(s"I've found a match! \n {Key: ${record.key()} \n Offset: ${record.offset()} \n Partition: ${record.partition()} \n Value: $value}")
            }

            //prints updates roughly every 20 seconds
            if (System.currentTimeMillis() - timer > 20000) {
              logger.info(s"{Currently processing record at Offset: ${record.offset()} and partition: ${record.partition()} }")
              timer = System.currentTimeMillis()
            }
          }
        }

        logger.info(s"No more messages!")

        consumer.close()

      case _ =>

    }

  }

  private def createConsumer(brokers: String): KafkaConsumer[String, Array[Byte]] = {
    val props = new Properties()
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers)
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer])
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, classOf[ByteArrayDeserializer])
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")
    props.put(ConsumerConfig.GROUP_ID_CONFIG, s"kafka-message-seeker-${scala.util.Random.nextString(10)}")
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    val consumer = new KafkaConsumer[String, Array[Byte]](props)
    consumer
  }


}
