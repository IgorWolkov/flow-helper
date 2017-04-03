package karazinscalausersgroup.routing

import argonaut.Json
import karazinscalausersgroup.routing._
import karazinscalausersgroup.routing.conversions._
import org.scalatest.{Matchers, FlatSpec}

import scala.language.implicitConversions

/**
  * @author Igor Wolkov
  */
class RoutingTest extends FlatSpec with Matchers {

  case class Error()

  val flow = new Extractor[Json, String] {
    def `extract property from`(message: Json): String =
      message.field("key").get.stringOrEmpty.toString
  }

  val `either flow` = new Extractor[Either[Error, Json], Either[Error, String]] {
    def `extract property from`(message: Either[Error, Json]): Either[Error, String] =
      message.right map { _.field("key").get.stringOrEmpty.toString }
  }

  implicit def stringToJson(str: String): Json =
    Parse.parseOption(str).get

  "Router" should "correctly route message to the first flow and skip the second and the third flow" in {

    val router = new Router[Json, String, String] {}

    val `one message`: Json = """{"a": "1", "b": "2", "c": "3", "key": "one"}""".stripMargin
    val `two message`: Json = """{"a": "1", "b": "2", "c": "3", "key": "two"}""".stripMargin
    val `three message`: Json = """{"a": "1", "b": "2", "c": "3", "key": "three"}""".stripMargin

    Consume / router /> { message => "flow one" }
    Consume / router /> { message => "flow two" }
    Consume / router /> { message => "flow three" }

    assert((router process `one message`) == "flow one")
    assert((router process `two message`) == "flow one")
    assert((router process `three message`) == "flow one")

  }

  "Router" should "correctly route message to the appropriate flow" in {

    val router = new Router[Json, String, String] {}

    val `one message`: Json = """{"a": "1", "b": "2", "c": "3", "key": "one"}""".stripMargin
    val `two message`: Json = """{"a": "1", "b": "2", "c": "3", "key": "two"}""".stripMargin
    val `three message`: Json = """{"a": "1", "b": "2", "c": "3", "key": "three"}""".stripMargin

    Consume / router / flow / "one" /> { message => "flow one" }
    Consume / router / flow / "two" /> { message => "flow two" }
    Consume / router / flow / "three" /> { message => "flow three" }

    assert((router process `one message`) == "flow one")
    assert((router process `two message`) == "flow two")
    assert((router process `three message`) == "flow three")

  }

  "Router" should "correctly route message by wildcard to appropriate flow" in {

    val router = new Router[Json, String, String] {}

    val `wildcard flow message`: Json = """{"a": "1", "b": "2", "c": "3", "key": "wildcard"}""".stripMargin

    Consume / router / flow / "flow" /> { message => "flow flow" }
    Consume / router / flow / ".*" /> { message => "wildcard" }

    assert((router process `wildcard flow message`) == "wildcard")

  }

  "Router" should "correctly route message by the prefix to appropriate flow" in {

    val router = new Router[Json, String, String] {}

    val `wildcard flow message`: Json = """{"a": "1", "b": "2", "c": "3", "key": "prefix"}""".stripMargin

    Consume / router / flow / "flow" /> { message => "flow flow" }
    Consume / router / flow / "pre.*" /> { message => "prefix" }

    assert((router process `wildcard flow message`) == "prefix")

  }

  "Router" should "correctly rout message by presented alternatives" in {

    val router = new Router[Json, String, String] {}

    val `alternative message`: Json = """{"a": "1", "b": "2", "c": "3", "key": "alternative"}""".stripMargin

    Consume / router / flow / "alternative1|alternative2|alternative" /> { message => "alternative"}

    assert((router process `alternative message`) == "alternative")

  }

  "Router" should "fail with match error for the unexpected alternatives" in {

    val router = new Router[Json, String, String] {}

    val `alternative message`: Json = """{"a": "1", "b": "2", "c": "3", "key": "alternative"}""".stripMargin

    Consume / router / flow / "alternative1|alternative2|alternative3" /> { message => "alternative"}

    intercept[MatchError]((router process `alternative message`) == "alternative")

  }

  "Router" should "fail with match error for the unexpected message" in {

    val router = new Router[Json, String, String] {}

    val `unexpected message`: Json = """{"a": "1", "b": "2", "c": "3", "key": "unexpected"}""".stripMargin

    Consume / router / flow / "flow" /> { message => "flow" }

    intercept[MatchError](router process `unexpected message`)

  }

  "Router" should "correctly route message to the appropriate flow with match error handling" in {

    val router = new Router[Json, String, String] {}

    implicit val `match error`: Json => Boolean = (json: Json) => false

    val `one message`: Json = """{"a": "1", "b": "2", "c": "3", "key": "one"}""".stripMargin
    val `two message`: Json = """{"a": "1", "b": "2", "c": "3", "key": "two"}""".stripMargin
    val `three message`: Json = """{"a": "1", "b": "2", "c": "3", "key": "three"}""".stripMargin

    Consume / router / flow / "one" /> { message => "flow one" }
    Consume / router / flow / "two" /> { message => "flow two" }
    Consume / router / flow / "three" /> { message => "flow three" }

    assert((router `process with match error handling`[Boolean] `one message`) == Right("flow one"))
    assert((router `process with match error handling`[Boolean] `two message`) == Right("flow two"))
    assert((router `process with match error handling`[Boolean] `three message`) == Right("flow three"))

  }

  "Router" should "be recovered after the unexpected message when processed with match error handling" in {

    val router = new Router[Json, String, String] {}

    val `unexpected message`: Json = """{"a": "1", "b": "2", "c": "3", "key": "unexpected"}""".stripMargin

    implicit val `match error`: Json => String = (json: Json) => "recovered"

    Consume / router / flow / "flow" /> { message => "flow" }

    assert((router `process with match error handling`[String] `unexpected message`) == Left("recovered"))

  }

  "Router" should "correctly route message to the appropriate flow with error handling" in {

    val router = new Router[Json, String, String] {}

    implicit val `match error`: Json => Boolean = (json: Json) => false
    implicit val `error handling`: Throwable => Boolean = (throwable: Throwable) => false

    val `one message`: Json = """{"a": "1", "b": "2", "c": "3", "key": "one"}""".stripMargin
    val `two message`: Json = """{"a": "1", "b": "2", "c": "3", "key": "two"}""".stripMargin
    val `three message`: Json = """{"a": "1", "b": "2", "c": "3", "key": "three"}""".stripMargin

    Consume / router / flow / "one" /> { message => "flow one" }
    Consume / router / flow / "two" /> { message => "flow two" }
    Consume / router / flow / "three" /> { message => "flow three" }

    assert((router `process with error handling`[Boolean] `one message`) == Right("flow one"))
    assert((router `process with error handling`[Boolean] `two message`) == Right("flow two"))
    assert((router `process with error handling`[Boolean] `three message`) == Right("flow three"))

  }

  "Router" should "be recovered after the unexpected message when processed with error handling" in {

    val router = new Router[Json, String, String] {}

    implicit val `match error`: Json => String = (json: Json) => "recovered"
    implicit val `error handling`: Throwable => String = (throwable: Throwable) => "recovered"

    val `unexpected message`: Json = """{"a": "1", "b": "2", "c": "3", "key": "unexpected"}""".stripMargin

    Consume / router / flow / "flow" /> { message => "flow" }

    assert((router `process with error handling`[String] `unexpected message`) == Left("recovered"))

  }

  "Router" should "be recovered after the unexpected error when processed with error handling" in {

    val router = new Router[Json, String, String] {}

    implicit val `match error`: Json => String = (json: Json) => "recovered"
    implicit val `error handling`: Throwable => String = (throwable: Throwable) => "recovered"

    val `unexpected message`: Json = """{"a": "1", "b": "2", "c": "3", "key": "unexpected"}""".stripMargin

    Consume / router / flow / "flow" /> { message => throw new RuntimeException("Unexpected exception")}

    assert((router `process with error handling`[String] `unexpected message`) == Left("recovered"))

  }

  "Router" should "correctly process right message" in {

    val router = new Router[Either[Error, Json], Either[Error, String], Either[Error, String]] {}

    val `one message`: Either[Error, Json] = Right("""{"a": "1", "b": "2", "c": "3", "key": "one"}""".stripMargin)
    val `two message`: Either[Error, Json] = Right("""{"a": "1", "b": "2", "c": "3", "key": "two"}""".stripMargin)
    val `three message`: Either[Error, Json] = Right("""{"a": "1", "b": "2", "c": "3", "key": "three"}""".stripMargin)

    Consume / router / `either flow` / "one" /> { message => message.right map (_ => "flow one") }
    Consume / router / `either flow` / "two" /> { message => message.right map (_ => "flow two") }
    Consume / router / `either flow` / "three" /> { message => message.right map (_ =>"flow three") }

    assert((router process `one message`) == Right("flow one"))
    assert((router process `two message`) == Right("flow two"))
    assert((router process `three message`) == Right("flow three"))

  }

  "Router" should "correctly process error message" in {

    val error = new Error()

    val router = new Router[Either[Error, Json], Either[Error, String], Either[Error, String]] {}

    val message: Either[Error, Json] = Left[Error, Json](error)

    Consume / router / `either flow` / "one" /> { message => message.right map ( _ => "flow one") }

    assert((router process message) == Left(error))

  }

}
