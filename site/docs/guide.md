#### Domain model & Errors

```scala mdoc:reset-object:silent
final case class Username(value: String) extends AnyVal
final case class User(username: Username)

sealed trait UserError extends Exception
case class UserAlreadyExists(username: Username) extends UserError
case class UserNotFound(username: Username)      extends UserError
```

#### Algebras

Also known as interfaces, they define the functionality we want to expose and we will only operate in terms of these definitions:

```scala mdoc:silent
trait UserAlgebra[F[_]] {
  def find(username: Username): F[User]
  def persist(user: User): F[Unit]
}

trait UserRepository[F[_]] {
  def find(username: Username): F[Option[User]]
  def persist(user: User): F[Unit]
}

trait UserRegistry[F[_]] {
  def register(user: User): F[Unit]
}
```

#### Programs

Contains pure logic. It can possiby combine multiple algebras as well as other programs but without commiting to a specific implementation:

```scala mdoc:silent
import cats.{MonadError, Parallel}
import cats.implicits._

class UserProgram[F[_]: Parallel](repo: UserRepository[F], registry: UserRegistry[F])(implicit F: MonadError[F, Throwable]) extends UserAlgebra[F] {

  def find(username: Username): F[User] =
    repo.find(username).flatMap {
      case Some(u) => F.pure(u)
      case None    => F.raiseError(UserNotFound(username))
    }

  def persist(user: User): F[Unit] =
    repo.find(user.username).flatMap {
      case Some(_) => F.raiseError(UserAlreadyExists(user.username))
      case None    => (registry.register(user), repo.persist(user)).parTupled.void
    }

}
```

#### Interpreters

In this case we will only have a single interpreter for our `Repository`: an in-memory implementation based on `Ref`.

```scala mdoc:silent
import cats.effect._
import cats.effect.concurrent.Ref

class MemUserRepository[F[_]: Sync] (
    state: Ref[F, Map[Username, User]]
) extends UserRepository[F] {

  override def find(username: Username): F[Option[User]] =
    state.get.map(_.get(username))

  override def persist(user: User): F[Unit] =
    state.update(_.updated(user.username, user))

}

object MemUserRepository {
  def create[F[_]: Sync]: F[UserRepository[F]] =
    Ref.of[F, Map[Username, User]](Map.empty).map(new MemUserRepository[F](_))
}
```

And an interpreter for our `UserRegistry` which calls an external http service. But first we need to define some Json codecs that will also be used by all our `HttpRoutes`:

```scala mdoc:silent
import io.circe.{Decoder, Encoder}
import io.circe.generic.extras.decoding.UnwrappedDecoder
import io.circe.generic.extras.encoding.UnwrappedEncoder
import org.http4s._
import org.http4s.circe._

implicit def valueClassEncoder[A: UnwrappedEncoder]: Encoder[A] = implicitly
implicit def valueClassDecoder[A: UnwrappedDecoder]: Decoder[A] = implicitly

implicit def jsonDecoder[F[_]: Sync, A <: Product: Decoder]: EntityDecoder[F, A] = jsonOf[F, A]
implicit def jsonEncoder[F[_]: Sync, A <: Product: Encoder]: EntityEncoder[F, A] = jsonEncoderOf[F, A]
```

Here's our interpreter for `UserRegistry`:

```scala mdoc:silent
import io.circe.syntax._
import org.http4s.Method._
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl

final case class LiveUserRegistry[F[_]: Sync](client: Client[F]) extends UserRegistry[F]  with Http4sClientDsl[F] {

  private val uri = Uri.uri("https://jsonplaceholder.typicode.com/posts")

  def register(user: User): F[Unit] =
    client.successful(POST(user.asJson, uri)).void
}

```
