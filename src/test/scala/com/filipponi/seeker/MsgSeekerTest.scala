package com.filipponi.seeker

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets.UTF_8
import java.util.Properties

import com.filipponi.seeker.util.DockerKafkaService
import com.whisk.docker.impl.spotify.DockerKitSpotify
import com.whisk.docker.scalatest.DockerTestKit
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}
import org.mockito.Matchers.{any, contains}
import org.mockito.Mockito.{never, verify}
import org.scalatest.time.{Second, Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FlatSpec, Matchers}
import org.scalatestplus.mockito.MockitoSugar
import org.slf4j.{Logger, LoggerFactory}

import scala.util.Try

class MsgSeekerTest extends FlatSpec with BeforeAndAfterAll with Matchers
  with DockerKafkaService
  with DockerTestKit
  with DockerKitSpotify with MockitoSugar with BeforeAndAfterEach {

  private val logger = LoggerFactory.getLogger(getClass)

  private val producer: KafkaProducer[Array[Byte], Array[Byte]] = createKafkaProducer("localhost:9092")

  implicit val pc: PatienceConfig = PatienceConfig(Span(10, Seconds), Span(1, Second))

  override def beforeAll(): Unit = {
    super.beforeAll()
    assume(isContainerReady(kafkaContainer).futureValue,"Couldn't start the kafka docker container...")
  }

  "SeekMsg" should "print the usage when required options are not passed" in {

    val args = Array[String]("invalid", "param")

    val err = new ByteArrayOutputStream()
    val logger = mock[Logger]

    Console.withErr(err) {
      MsgSeeker.seek(logger,args)
    }

    verify(logger,never()).info(any())
    verify(logger,never()).debug(any())
    verify(logger,never()).error(any())
    verify(logger,never()).trace(any())

    val stderr = new String(err.toByteArray, UTF_8)

    stderr should (include("Error: Unknown argument 'invalid'") and
      include("Error: Unknown argument 'param'") and
      include("Error: Missing option --topic"))
  }

  "SeekMsg" should "find a message in the middle of the topic" in {

    val topic = "topic1"

    val args = Array[String]("--brokers","localhost:9092","--topic", s"$topic", "--offset", "0", "--search-for", "XY")

    val err = new ByteArrayOutputStream()

    val logger = mock[Logger]

    produceMessagesOnTopic(producer, topic, List("test", "abcdXY", "msg"))

    Console.withErr(err) {
      MsgSeeker.seek(logger, args)
    }
    val stderr = new String(err.toByteArray, UTF_8)

    stderr should be("")
    verify(logger).info(contains("I've found a match! \n {Key: null \n Offset: 1 \n Partition: 0 \n Value: abcdXY}"))
    verify(logger,never()).info(contains("test"))
    verify(logger,never()).info(contains("msg"))
  }

  "SeekMsg" should "find multiple matches" in {

    val topic = "topic2"

    val stringToFind = "a"
    val args = Array[String]("--brokers","localhost:9092","--topic", s"$topic", "--offset", "0", "--search-for", stringToFind)

    val err = new ByteArrayOutputStream()
    val messages = List("abcd", "abcdXY", "abcd2", "1234", "msg","blah")

    produceMessagesOnTopic(producer, topic, messages)

    val logger = mock[Logger]

    Console.withErr(err) {
        MsgSeeker.seek(logger,args)
    }

    val stderr = new String(err.toByteArray, UTF_8)

    stderr should be("")
    verify(logger).info(contains(buildFoundMessageString(0,messages.head)))
    verify(logger).info(contains(buildFoundMessageString(1,messages(1))))
    verify(logger).info(contains(buildFoundMessageString(2,messages(2))))
    verify(logger).info(contains(buildFoundMessageString(5,messages(5))))
    verify(logger, never()).info(contains("1234"))
    verify(logger, never()).info(contains("msg"))

  }


  "SeekMsg" should "not find a message if there is no match" in {

    val topic = "topicC"

    val stringToFind = "i'm not there!"
    val args = Array[String]("--brokers","localhost:9092","--topic", s"$topic", "--offset", "2", "--search-for", stringToFind)

    val err = new ByteArrayOutputStream()
    val out = new ByteArrayOutputStream()

    produceMessagesOnTopic(producer, topic, List("abcd", "abcdXY", "abcd", "1234", "msg","blah"))

    val logger = mock[Logger]

    Console.withErr(err) {
        MsgSeeker.seek(logger,args)
    }

    val stderr = new String(err.toByteArray, UTF_8)
    val stdout = new String(out.toByteArray, UTF_8)

    stderr should be("")
    verify(logger, never()).info(contains("I've found a match!"))

  }

  "SeekMsg" should "not find a message if starting from an higher offset" in {

    val topic = "topic4"

    val stringToFind = "match"
    val args = Array[String]("--brokers","localhost:9092","--topic", s"$topic", "--offset", "2", "--search-for", stringToFind)

    val err = new ByteArrayOutputStream()
    val out = new ByteArrayOutputStream()

    produceMessagesOnTopic(producer, topic, List(s"abcd$stringToFind", "XY", "abcd", "1234", "msg"))

    val logger = mock[Logger]

    Console.withErr(err) {
      MsgSeeker.seek(logger,args)
    }

    val stderr = new String(err.toByteArray, UTF_8)

    stderr should be("")
    verify(logger, never()).info(contains("I've found a match!"))
  }

  "SeekMsg" should "find messages only after the specified offset" in {

    val topic = "topic5"

    val stringToFind = "match"
    val eventualMatch = s"1234$stringToFind"
    val args = Array[String]("--brokers","localhost:9092","--topic", s"$topic", "--offset", "2", "--search-for", stringToFind)

    val err = new ByteArrayOutputStream()
    val out = new ByteArrayOutputStream()

    val messages = List(s"abcd$stringToFind", "XY", "abcd", eventualMatch , "msg")

    produceMessagesOnTopic(producer, topic, messages)

    val logger = mock[Logger]

    Console.withErr(err) {
      MsgSeeker.seek(logger,args)
    }

    val stderr = new String(err.toByteArray, UTF_8)

    stderr should be("")
    verify(logger).info(contains(buildFoundMessageString(3,messages(3))))
    verify(logger, never()).info(contains("abcdMatch"))
  }

  private def createKafkaProducer(broker: String):
  KafkaProducer[Array[Byte], Array[Byte]] = {
    val props = new Properties()
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, broker)
    props.put(ProducerConfig.CLIENT_ID_CONFIG, "integration-test-producer")
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer")
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer")
    new KafkaProducer[Array[Byte], Array[Byte]](props)
  }

  private def produceMessagesOnTopic[K, V](kafkaProducer: KafkaProducer[Array[Byte], Array[Byte]], topic: String, messages: List[String]): Unit = {

    //todo change to a fold that will save the number of messages sent
    messages.foreach {
      message => {
        val record: ProducerRecord[Array[Byte], Array[Byte]] = new ProducerRecord(topic, message.getBytes(UTF_8))
        val metadata = Try {
          kafkaProducer.send(record).get
        }
        metadata.fold(
          err => logger.error("There was an error producing the message", err),
          metadata => logger.debug(s"Record sent on topic ${metadata.topic}")
        )
      }
    }
  }

  private def buildFoundMessageString(offset: Int, value: String, partition : Int = 0) = {
    s"{Key: null \n Offset: $offset \n Partition: $partition \n Value: $value}"
  }

}
