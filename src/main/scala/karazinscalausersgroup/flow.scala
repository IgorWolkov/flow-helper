package karazinscalausersgroup

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object flow {

  object Service {

    implicit class ServiceOps1[Req, RepIn](service: Service[Req, RepIn]) {
      def -->[RepOut](next: Service[RepIn, RepOut]): Service[Req, RepOut] =
        new Service[Req, RepOut] {
          def apply(req: Req): Future[RepOut] = service(req) flatMap next
        }
    }

    implicit class ServiceOps2[Req, Rep, Error](service: Service[Either[Error, Req], Either[Error, Rep]]) {
      def `handle errors with`(handler: Service[Error, Error]): Service[Either[Error, Req], Either[Error, Rep]] =
        new Service[Either[Error, Req], Either[Error, Rep]] {
          def apply(req: Either[Error, Req]): Future[Either[Error, Rep]] = {
            service(req) flatMap {
              case Left(error) => handler(error) map {
                Left[Error, Rep]
              }
              case Right(response) => Future {
                Right[Error, Rep](response)
              }
            }
          }
        }
    }

    implicit class ServiceOps3[Req, Rep1, Rep2, Error](service1: Service[Either[Error, Req], Either[Error, Rep1]]) {
      def `join with`(service2: Service[Either[Error, Req], Either[Error, Rep2]]):
      Service[Either[Error, Req], Either[Error, (Rep1, Rep2)]] =
        new Service[Either[Error, Req], Either[Error, (Rep1, Rep2)]] {
          def apply(req: Either[Error, Req]): Future[Either[Error, (Rep1, Rep2)]] = {
            (service1(req) zip service2(req)) map {
              case (res1, res2) =>
                for {
                  r1 <- res1.right
                  r2 <- res2.right
                } yield (r1, r2)
            }
          }
        }

      def `in parallel with`(service2: Service[Either[Error, Req], Either[Error, Rep2]]):
      Service[Either[Error, Req], (Either[Error, Rep1], Either[Error, Rep2])] =
        new Service[Either[Error, Req], (Either[Error, Rep1], Either[Error, Rep2])] {
          def apply(req: Either[Error, Req]): Future[(Either[Error, Rep1], Either[Error, Rep2])] =
            service1(req) zip service2(req)
        }

      def `with background`(service2: Service[Either[Error, Req], Either[Error, Rep2]]):
      Service[Either[Error, Req], Either[Error, Rep1]] =
        new Service[Either[Error, Req], Either[Error, Rep1]] {
          def apply(req: Either[Error, Req]): Future[Either[Error, Rep1]] = {
            service2(req)
            service1(req)
          }
        }
    }

  }

  trait Service[-Req, +Rep] extends (Req => Future[Rep]) {
    def apply(request: Req): Future[Rep]
  }

}

