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

class UserController[F[_]: Async](userService: UserService[F]) extends Http4sDsl[F] {

  private val logger = Slf4jLogger.getLogger[F]

  val routes: HttpRoutes[F] = HttpRoutes.of[F] {

    // Маршрут для отображения личного кабинета пользователя
    case req@GET -> Root / "cabinet" / username =>
      req.cookies.find(_.name == "username") match {
        case Some(cookie) if cookie.content == username =>
          userService.getUserByUsername(username).flatMap {
            case Some(user) =>
              // Загрузка HTML-шаблона и замена плейсхолдеров
              val description = user.description.getOrElse("Описание отсутствует")
              val htmlContent = generateCabinetHTML(user.username, description)
              Ok(htmlContent).map(_.putHeaders(`Content-Type`(MediaType.text.html, Charset.`UTF-8`)))
            case None =>
              SeeOther(Location(uri"/login")).map(_.removeCookie("username"))
          }
        case _ =>
          SeeOther(Location(uri"/login")).map(_.removeCookie("username"))
      }


    // Обработка формы регистрации
    case req@POST -> Root / "register" =>
      (for {
        formData <- req.as[UrlForm]
        username = formData.getFirst("username")
        password = formData.getFirst("password")
        email = formData.getFirst("email")

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
    case req@POST -> Root / "login" =>
      (for {
        formData <- req.as[UrlForm]
        username = formData.getFirst("username")
        password = formData.getFirst("password")

        result <- (username, password) match {
          case (Some(u), Some(p)) if u.nonEmpty && p.nonEmpty =>
            userService.login(u, p).flatMap {
              case Some(user) =>
                // Создаем сессию и перенаправляем на уникальный URL кабинета пользователя
                SeeOther(Location(Uri.unsafeFromString(s"/cabinet/${user.username}"))).map(
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

    // Обработка формы для обновления описания
    case req @ POST -> Root / "cabinet" / "updateDescription" =>
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
                  SeeOther(Location(Uri.unsafeFromString(s"/cabinet/${user.username}")))
              case None =>
                SeeOther(Location(uri"/login")).map(_.removeCookie("username"))
            }
          } yield response
        case None =>
          SeeOther(Location(uri"/login"))
      }
  }

  // Генерация HTML для личного кабинета
  private def generateCabinetHTML(username: String, description: String): String = {
    s"""
       |<html>
       |<head><meta charset="UTF-8"><title>Личный кабинет</title>
       |<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0-alpha1/dist/css/bootstrap.min.css" rel="stylesheet">
       |</head>
       |<body class="d-flex flex-column min-vh-100">
       |  <div class="container mt-5">
       |    <h1 class="text-center">Личный кабинет</h1>
       |    <p class="text-center">Добро пожаловать, <strong>$username</strong>!</p>
       |    <h3>Ваше описание:</h3>
       |    <p>$description</p>
       |
       |    <!-- Форма для редактирования описания -->
       |    <h4>Изменить описание</h4>
       |    <form action="/cabinet/updateDescription" method="POST" class="mb-4">
       |      <textarea name="description" rows="5" class="form-control mb-3">$description</textarea>
       |      <input type="submit" value="Сохранить" class="btn btn-primary">
       |    </form>
       |
       |    <div class="text-center">
       |      <a href="/test" class="btn btn-success mb-3">Пройти тест на знание слов</a>
       |    </div>
       |
       |    <div class="text-center">
       |      <a href="/logout" class="btn btn-danger">Выйти</a>
       |    </div>
       |  </div>
       |
       |  <footer class="mt-auto text-center p-3">
       |    <p>© 2024 Ваше приложение</p>
       |  </footer>
       |</body>
       |</html>
     """.stripMargin
  }
}
