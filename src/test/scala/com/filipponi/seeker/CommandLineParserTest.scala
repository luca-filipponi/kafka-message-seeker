package com.filipponi.seeker

import com.filipponi.seeker.CommandLineParser.{Config,kafkaMsgSeekerArgsParser}
import org.scalatest.{FlatSpec, Matchers}
import scopt.OParser

class CommandLineParserTest extends FlatSpec with Matchers {

  "CommandLineParser" should "return a none when options are not correct" in {

    val args = new Array[String](0)

    OParser.parse(kafkaMsgSeekerArgsParser, args, Config.empty()) should be(None)

  }

  "CommandLineParser" should "return a Some(config) all required options are passed" in {

    val args = new Array[String](8)

    args(0) = "--topic"
    args(1) = "test"
    args(2) = "--search-for"
    args(3) = "string"
    args(4) = "--offset"
    args(5) = "101010"
    args(6) = "--brokers"
    args(7) = "localhost:9092"


    OParser.parse(kafkaMsgSeekerArgsParser, args, Config.empty()) should be(Some(Config("localhost:9092","test", "string", 101010)))

  }

  "CommandLineParser" should "return a Some(config) regardless the order of options couple" in {

    val args = new Array[String](8)

    args(2) = "--topic"
    args(3) = "test"
    args(0) = "--search-for"
    args(1) = "string"
    args(6) = "--offset"
    args(7) = "101010"
    args(4) = "--brokers"
    args(5) = "localhost:9092"

    OParser.parse(kafkaMsgSeekerArgsParser, args, Config.empty()) should be(Some(Config("localhost:9092","test", "string", 101010)))

  }

}
