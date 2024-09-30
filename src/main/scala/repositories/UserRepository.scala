package repositories

import models.User
import doobie._
import doobie.implicits._
import cats.effect._

class UserRepository[F[_]: Async](xa: Transactor[F]) {

  def create(user: User): F[Int] = {
    sql"""
      INSERT INTO users (username, password, email, description)
      VALUES (${user.username}, ${user.password}, ${user.email}, ${user.description})
    """.update.run.transact(xa)
  }

  def findByUsername(username: String): F[Option[User]] = {
    sql"""
      SELECT id, username, password, email, description
      FROM users
      WHERE username = $username
    """.query[User].option.transact(xa)
  }

  def updateDescription(userId: Long, description: String): F[Int] = {
    sql"""
      UPDATE users
      SET description = $description
      WHERE id = $userId
    """.update.run.transact(xa)
  }

  // Другие методы...
}
