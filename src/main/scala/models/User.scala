package models

case class User(
                 id: Long,
                 username: String,
                 password: String,
                 email: String,
                 description: Option[String] = None // Описание по умолчанию отсутствует
               )
