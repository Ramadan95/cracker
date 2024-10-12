package controllers

import cats.effect._
import cats.implicits._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import models.Question
import org.http4s.implicits._
import org.http4s.UrlForm
import org.http4s.headers.`Content-Type`

class TestController[F[_]: Async] extends Http4sDsl[F] {

  val routes: HttpRoutes[F] = HttpRoutes.of[F] {

    // Маршрут для показа теста
    case GET -> Root / "test" =>
      val questions = Question.getQuestions
      val htmlContent = generateTestHTML(questions)
      Ok(htmlContent).map(_.putHeaders(`Content-Type`(MediaType.text.html, Charset.`UTF-8`)))

    // Маршрут для обработки результатов теста
    case req @ POST -> Root / "submit-test" =>
      req.as[UrlForm].flatMap { formData =>
        val questions = Question.getQuestions
        val userAnswers = questions.map(q => formData.getFirst(q.word).getOrElse(""))
        val correctAnswers = questions.map(_.correctAnswer)

        val score = userAnswers.zip(correctAnswers).count {
          case (userAnswer, correctAnswer) => userAnswer == correctAnswer
        }

        val resultHtml = generateResultHTML(score, questions.length)
        Ok(resultHtml).map(_.putHeaders(`Content-Type`(MediaType.text.html, Charset.`UTF-8`)))
      }
  }

  // Генерация HTML для теста
  private def generateTestHTML(questions: List[Question]): String = {
    val questionsHtml = questions.map { question =>
      val optionsHtml = question.options.map { option =>
        s"""<div><input type="radio" name="${question.word}" value="$option"> $option</div>"""
      }.mkString("\n")
      s"""
         |<h3>${question.word}</h3>
         |$optionsHtml
       """.stripMargin
    }.mkString("\n")

    s"""
       |<html>
       |<head><meta charset="UTF-8"><title>Тест по английским словам</title></head>
       |<body>
       |<h1>Тест на знание английских слов</h1>
       |<form action="/submit-test" method="POST">
       |$questionsHtml
       |<input type="submit" value="Отправить ответы" class="btn btn-primary">
       |</form>
       |</body>
       |</html>
     """.stripMargin
  }

  // Генерация HTML для показа результата
  private def generateResultHTML(score: Int, totalQuestions: Int): String = {
    s"""
       |<html>
       |<head><meta charset="UTF-8"><title>Результаты теста</title></head>
       |<body>
       |<h1>Результаты теста</h1>
       |<p>Ваш результат: $score из $totalQuestions</p>
       |<a href="/cabinet">Вернуться в личный кабинет</a>
       |</body>
       |</html>
     """.stripMargin
  }
}
