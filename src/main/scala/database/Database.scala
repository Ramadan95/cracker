package database

import cats.effect.{Async, Resource, Sync}
import com.typesafe.config.ConfigFactory
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts

object Database {

  def transactor[F[_]: Async]: Resource[F, HikariTransactor[F]] = {
    for {
      config <- Resource.eval(Sync[F].delay(ConfigFactory.load().getConfig("db")))
      url    <- Resource.eval(Sync[F].delay(config.getString("url")))
      user   <- Resource.eval(Sync[F].delay(config.getString("user")))
      pass   <- Resource.eval(Sync[F].delay(config.getString("password")))
      driver <- Resource.eval(Sync[F].delay(config.getString("driver")))
      ce     <- ExecutionContexts.cachedThreadPool // Или выберите подходящий вам размер пула
      xa     <- HikariTransactor.newHikariTransactor[F](
        driverClassName = driver,
        url             = url,
        user            = user,
        pass            = pass,
        connectEC       = ce
      )
    } yield xa
  }
}