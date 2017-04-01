package karazinscalausersgroup

import scala.collection.mutable.ArrayBuffer
import scala.language.implicitConversions
import scala.util.control.Exception._

/**
  * @author Igor Wolkov
  */
object routing {

  object conversions {

    implicit def stringToStringPattern(str: String): Pattern[String] =
      new Pattern[String] {
        val pattern = str

        private val Pattern = s"($pattern)".r

        def `is equal to`(value: String): Boolean =
          value match {
            case Pattern(v) => true
            case _          => false
          }
      }

    implicit def stringToStringEitherPattern[Error](str: String): Pattern[Either[Error, String]] =
      new Pattern[Either[Error, String]] {
        val pattern: Either[Error, String] = Right[Error, String](str)

        private val Pattern = pattern.right map { p => s"($p)".r }

        def `is equal to`(value: Either[Error, String]): Boolean =
          Pattern.right forall { P =>
            value.right forall {
              case P(v) => true
              case _    => false
            }
          }
      }
  }

  trait Pattern[U] {
    val pattern: U

    def `is equal to`(value: U): Boolean
  }

  trait Router[T, U, Result] {

    val routes: ArrayBuffer[Rout[T, U, Result]] = ArrayBuffer()

    def /(extractor: Extractor[T, U]): Extraction[T, U, Result] = Extraction[T, U, Result](this, extractor)

    def />(flow: T => Result) = Flow(this, Rout(Matcher.`through matcher`[T, U], flow))

    def process(message: T): Result =
      routes find ( _.matcher `is match` message ) match {
        case Some(matched) => matched process message
        case None          => throw new MatchError(s"Cannot match message $message")
      }

    def `process with match error handling`[Failure](message: T)(implicit `match error`: T => Failure): Failure Either Result =
      routes find ( _.matcher `is match` message ) match {
        case Some(matched) => Right(matched process message)
        case None          => Left(`match error`(message))
      }

    def `process with error handling`[Failure](message: T)(implicit `match error`: T => Failure, error: Throwable => Failure): Failure Either Result =
      routes find (_.matcher `is match` message) match {
        case Some(matched) => nonFatalCatch.either(matched process message).left map error
        case None          => Left(`match error`(message))
      }
  }

  trait Extractor[T, U] {
    def `extract property from`(message: T): U
  }

  case class Extraction[T, U, Result](router: Router[T, U, Result], extractor: Extractor[T, U]) {
    def /(pattern: Pattern[U]): Branch[T, U, Result] =
      new Branch(router, CompoundMatcher(extractor, pattern))
  }

  trait Matcher[T, U] {
    def `is match`(message: T): Boolean
  }

  object Matcher {
    def `through matcher`[T, U]: Matcher[T, U] = new Matcher[T, U] {
      override def `is match`(message: T): Boolean = true
    }
  }

  case class CompoundMatcher[T, U](extractor: Extractor[T, U], pattern: Pattern[U]) extends Matcher[T, U] {
    def `is match`(message: T): Boolean =
      pattern `is equal to` (extractor `extract property from` message)
  }

  case class Rout[T, U, Result](matcher: Matcher[T, U], process: T => Result)

  case class Branch[T, U, Result](router: Router[T, U, Result], matcher: Matcher[T, U]) {
    def />(flow: T => Result) =
      Flow(router, Rout(matcher, flow))
  }

  case class Flow[T, U, Result](router: Router[T, U, Result], rout: Rout[T, U, Result]) {
    router.routes += rout
  }

  object Consume {
    def /[T, U, Result](router: Router[T, U, Result]) = router
  }
}
