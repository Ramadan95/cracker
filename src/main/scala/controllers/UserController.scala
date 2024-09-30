// src/main/scala/controllers/UserController.scala
package controllers

import cats.effect._
import cats.implicits._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import services.UserService
import models.User
import org.http4s.implicits._
import org.http4s.UrlForm
import org.http4s.headers.{Location, `Content-Type`}
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.io.Source

class UserController[F[_]: Async](userService: UserService[F]) extends Http4sDsl[F] {

  private val logger = Slf4jLogger.getLogger[F]

  val routes: HttpRoutes[F] = HttpRoutes.of[F] {

    // Обработка формы регистрации (уже реализовано)
    case req @ POST -> Root / "register" =>
      (for {
        formData <- req.as[UrlForm]
        username = formData.getFirst("username")
        password = formData.getFirst("password")
        email    = formData.getFirst("email")

        _ <- (username, password, email) match {
          case (Some(u), Some(p), Some(e)) if u.nonEmpty && p.nonEmpty && e.nonEmpty =>
            userService.register(User(0, u, p, e))
          case _ =>
            BadRequest("Все поля должны быть заполнены")
        }

        resp <- Ok("Регистрация успешна")
      } yield resp).recoverWith {
        case e: Exception =>
          logger.error(e)("Ошибка при регистрации") *> InternalServerError("Произошла ошибка при регистрации")
      }

    // Обработка формы входа
    case req @ POST -> Root / "login" =>
      (for {
        formData <- req.as[UrlForm]
        username = formData.getFirst("username")
        password = formData.getFirst("password")

        result <- (username, password) match {
          case (Some(u), Some(p)) if u.nonEmpty && p.nonEmpty =>
            userService.login(u, p).flatMap {
              case Some(user) =>
                // Создаем сессию (об этом далее)
                SeeOther(Location(uri"/cabinet")).map(
                  _.addCookie(ResponseCookie("username", user.username))
                )
              case None =>
                BadRequest("Неверное имя пользователя или пароль")
            }
          case _ =>
            BadRequest("Все поля должны быть заполнены")
        }
      } yield result).recoverWith {
        case e: Exception =>
          logger.error(e)("Ошибка при входе") *> InternalServerError("Произошла ошибка при входе")
      }

    case GET -> Root / "logout" =>
      // Удаление cookie и перенаправление на главную страницу
      SeeOther(Location(uri"/")).map(_.removeCookie("username"))


    // Маршрут для личного кабинета
    case req@GET -> Root / "cabinet" =>
      req.cookies.find(_.name == "username") match {
        case Some(cookie) =>
          val username = cookie.content
          userService.getUserByUsername(username).flatMap {
            case Some(user) =>
              // Загрузка HTML-файла
              val htmlContent = Source.fromResource("static/cabinet.html").getLines().mkString("\n")
              // Получение описания пользователя
              val description = user.description.getOrElse("Описание отсутствует")
              // Замена плейсхолдеров в HTML
              val personalizedContent = htmlContent
                .replace("{{username}}", user.username)
                .replace("{{description}}", description)
              Ok(personalizedContent).map(_.putHeaders(`Content-Type`(MediaType.text.html)))
            case None =>
              SeeOther(Location(uri"/login")).map(_.removeCookie("username"))
          }
        case None =>
          SeeOther(Location(uri"/login"))
      }

    // Маршрут для обновления описания
    case req@POST -> Root / "cabinet" / "updateDescription" =>
      req.cookies.find(_.name == "username") match {
        case Some(cookie) =>
          val username = cookie.content
          for {
            formData <- req.as[UrlForm]
            description = formData.getFirst("description").getOrElse("")
            userOption <- userService.getUserByUsername(username)
            response <- userOption match {
              case Some(user) =>
                userService.updateDescription(user.id, description) *>
                  SeeOther(Location(uri"/cabinet"))
              case None =>
                SeeOther(Location(uri"/login")).map(_.removeCookie("username"))
            }
          } yield response
        case None =>
          SeeOther(Location(uri"/login"))
      }

    // Другие маршруты...
  }
}
