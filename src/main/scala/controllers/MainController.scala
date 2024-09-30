package controllers

import cats.effect._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.StaticFile

class MainController[F[_]: Async] extends Http4sDsl[F] {

  val routes: HttpRoutes[F] = HttpRoutes.of[F] {

    // Маршрут для главной страницы
    case GET -> Root =>
      StaticFile.fromResource("/static/index.html", Some(Request[F]()))
        .getOrElseF(NotFound("Страница не найдена"))

    // Маршрут для страницы регистрации
    case GET -> Root / "register" =>
      StaticFile.fromResource("/static/registration.html", Some(Request[F]()))
        .getOrElseF(NotFound("Страница не найдена"))

    // Маршрут для страницы входа
    case GET -> Root / "login" =>
      StaticFile.fromResource("/static/login.html", Some(Request[F]()))
        .getOrElseF(NotFound("Страница не найдена"))
  }
}
