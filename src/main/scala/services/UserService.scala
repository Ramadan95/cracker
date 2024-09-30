package services

import models.User
import repositories.UserRepository
import cats.effect._
import cats.implicits._

class UserService[F[_]: Sync](userRepo: UserRepository[F]) {

  def register(user: User): F[Int] = userRepo.create(user)

  def login(username: String, password: String): F[Option[User]] = {
    userRepo.findByUsername(username).map {
      case Some(user) if user.password == password => Some(user)
      case _ => None
    }
  }

  def updateDescription(userId: Long, description: String): F[Int] = {
    userRepo.updateDescription(userId, description)
  }

  def getUserByUsername(username: String): F[Option[User]] = {
    userRepo.findByUsername(username)
  }

  // Другие методы...
}
