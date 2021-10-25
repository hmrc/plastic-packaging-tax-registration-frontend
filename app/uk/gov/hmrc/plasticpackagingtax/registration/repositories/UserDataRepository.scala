/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.plasticpackagingtax.registration.repositories

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.libs.json.{Reads, Writes}
import uk.gov.hmrc.mongo.cache.CacheIdType.SessionCacheId.NoSessionException
import uk.gov.hmrc.mongo.cache.{CacheIdType, DataKey, MongoCacheRepository}
import uk.gov.hmrc.mongo.{MongoComponent, TimestampSupport}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.AuthenticatedRequest

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserDataRepository @Inject() (
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
    ) {

  private def id(implicit request: AuthenticatedRequest[Any]): String =
    request.session
      .get("sessionId")
      .getOrElse(throw NoSessionException)

  def putData[A: Writes](key: String, data: A)(implicit
    request: AuthenticatedRequest[Any]
  ): Future[A] =
    put[A](id)(DataKey(key), data).map(_ => data)

  def getData[A: Reads](key: String)(implicit
    request: AuthenticatedRequest[Any]
  ): Future[Option[A]] = get[A](id)(DataKey(key))

}
