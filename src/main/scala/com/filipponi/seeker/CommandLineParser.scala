package com.filipponi.seeker

import scopt.{OParser, OParserBuilder}

object CommandLineParser {

  /**
    * Simple configuration class for the input args.
    * @param brokers the kafka brokers.
    * @param topic the topic to use.
    * @param stringToSeek the string to seek in the kafka messages.
    * @param offset the offset to start with.
    */
  case class Config(brokers: String,
                    topic: String,
                    stringToSeek: String,
                    offset: Long)

  object Config {
    def empty(): Config = new Config("","", "", 0L)
  }

  val builder: OParserBuilder[Config] = OParser.builder[Config]

  val kafkaMsgSeekerArgsParser: OParser[Unit, Config] = {
    import builder._
    OParser.sequence(
      programName("kafkaMessage seeker"),
      head("kafkaMessageSeeker", "0.1"),
      opt[String]( 'b',"brokers")
        .required()
        .action((x, c) => c.copy(brokers = x))
        .text("The kafka brokers"),
      opt[String]( 't',"topic")
        .required()
        .action((x, c) => c.copy(topic = x))
        .text("The topic for which seek the message"),
      opt[String]( 's',"search-for")
        .required()
        .action((x, c) => c.copy(stringToSeek = x))
        .text("The string that will be searched in topic"),
      opt[Long]( 'o',"offset")
        .required()
        .action((x, c) => c.copy(offset = x))
        .text("The offset to start with (for every partition!)")
    )
  }
}
