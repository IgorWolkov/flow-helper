# Flow Helper

Flow Helper is a simple library inspired by [Finagle](https://twitter.github.io/finagle/)
and [Finch](https://github.com/finagle/finch) (v < 0.7.0).

The use case of this library is to restrict your self by pipe-line
codding and make your code fine grained and easy to read.

Installation
-------
Maven dependency is coming soon

Flow Hello World
-------
```scala
  import scala.concurrent.Future
  import karazinscalausersgroup.flow.Service

  trait Request
  trait AuthorizedRequest
  trait ValidatedRequest
  trait Order

  object Authorize extends Service[Request, AuthorizedRequest] {
    override def apply(request: Request): Future[AuthorizedRequest] = ???
  }

  object RequestToOrder extends Service[ValidatedRequest, Order] {
    override def apply(request: ValidatedRequest): Future[Order] = ???
  }

  object Validate extends Service[AuthorizedRequest, ValidatedRequest] {
    override def apply(request: AuthorizedRequest): Future[ValidatedRequest] = ???
  }

  val `order flow` = Authorize --> Validate --> RequestToOrder
```

See [examples](src/test/karazinscalausersgroup/flow/examples) for more complete examples.

Routing Hello World
-------

See [examples](src/test/karazinscalausersgroup/routing/examples) for more complete examples.

License
-------
Licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except in compliance with the License.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.