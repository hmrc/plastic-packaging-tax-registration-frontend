/*
 * Copyright 2022 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package repositories

import com.google.inject.ImplementedBy
import play.api.Configuration
import play.api.libs.json.{Reads, Writes}
import uk.gov.hmrc.auth.core.SessionRecordNotFound
import uk.gov.hmrc.mongo.cache.CacheIdType.SessionCacheId.NoSessionException
import uk.gov.hmrc.mongo.cache.{CacheIdType, CacheItem, DataKey, MongoCacheRepository}
import uk.gov.hmrc.mongo.{MongoComponent, TimestampSupport}
import models.request.AuthenticatedRequest

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[MongoUserDataRepository])
trait UserDataRepository {

  def putData[T: Writes](key: String, data: T)(implicit
    request: AuthenticatedRequest[Any]
  ): Future[T]

  def putData[T: Writes](id: String, key: String, data: T): Future[T]

  def getData[T: Reads](key: String)(implicit request: AuthenticatedRequest[Any]): Future[Option[T]]
  def getData[T: Reads](id: String, key: String): Future[Option[T]]

  def deleteData[T: Writes](key: String)(implicit request: AuthenticatedRequest[Any]): Future[Unit]
  def deleteData[T: Writes](id: String, key: String): Future[Unit]

  def updateData[T: Reads: Writes](key: String, updater: T => T)(implicit
    request: AuthenticatedRequest[Any]
  ): Future[Unit]

  def reset(): Unit
}

@Singleton
class MongoUserDataRepository @Inject() (
  mongoComponent: MongoComponent,
  configuration: Configuration,
  timestampSupport: TimestampSupport
)(implicit ec: ExecutionContext)
    extends MongoCacheRepository(
      mongoComponent = mongoComponent,
      collectionName = "userDataCache",
      ttl = configuration.get[FiniteDuration]("mongodb.userDataCache.expiry"),
      timestampSupport = timestampSupport,
      cacheIdType = CacheIdType.SimpleCacheId
    ) with UserDataRepository {

  private def id(implicit request: AuthenticatedRequest[Any]): String =
    request.session
      .get("sessionId")
      .getOrElse(throw NoSessionException)

  override def putData[T: Writes](key: String, data: T)(implicit
    request: AuthenticatedRequest[Any]
  ): Future[T] =
    put[T](id)(DataKey(key), data).map(_ => data)

  override def putData[A: Writes](id: String, key: String, data: A): Future[A] =
    put[A](id)(DataKey(key), data).map(_ => data)

  override def getData[A: Reads](key: String)(implicit
    request: AuthenticatedRequest[Any]
  ): Future[Option[A]] = get[A](id)(DataKey(key))

  override def getData[A: Reads](id: String, key: String): Future[Option[A]] =
    get[A](id)(DataKey(key))

  def findBySessionId(id: String): Future[CacheItem] =
    findById(id).map(_.getOrElse(throw SessionRecordNotFound()))

  override def deleteData[A: Writes](
    key: String
  )(implicit request: AuthenticatedRequest[Any]): Future[Unit] =
    delete[A](id)(DataKey(key))

  override def deleteData[A: Writes](id: String, key: String): Future[Unit] =
    delete[A](id)(DataKey(key))

  override def updateData[A: Reads: Writes](key: String, updater: A => A)(implicit
    request: AuthenticatedRequest[Any]
  ): Future[Unit] =
    getData(key).map(data => data.map(data => putData(key, updater(data))))

  override def reset(): Unit = {}
}
