package models

case class Question(
                     word: String, // Слово на английском
                     options: List[String], // Варианты ответов
                     correctAnswer: String // Правильный ответ
                   )

object Question {
  def getQuestions: List[Question] = List(
    Question("apple", List("яблоко", "стол", "собака", "книга"), "яблоко"),
    Question("book", List("машина", "собака", "книга", "яблоко"), "книга"),
    Question("dog", List("кот", "птица", "собака", "яблоко"), "собака"),
    Question("table", List("яблоко", "стол", "книга", "собака"), "стол")
  )
}
