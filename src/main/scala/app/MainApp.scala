// src/main/scala/app/MainApp.scala
package app

import cats.effect._
import org.http4s.implicits._
import org.http4s.server.blaze._
import controllers.{UserController, MainController}
import database.Database
import repositories.UserRepository
import services.UserService
import cats.syntax.semigroupk._

object MainApp extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {

    Database.transactor[IO].use { xa =>
      val userRepo       = new UserRepository[IO](xa)
      val userService    = new UserService[IO](userRepo)
      val userController = new UserController[IO](userService)
      val mainController = new MainController[IO]()

      val httpApp = (
        mainController.routes <+>
          userController.routes
        ).orNotFound

      BlazeServerBuilder[IO]
        .bindHttp(8080, "localhost")
        .withHttpApp(httpApp)
        .serve
        .compile
        .drain
        .as(ExitCode.Success)
    }
  }
}
