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

package base.unit

import play.api.libs.json.{Reads, Writes}
import models.request.AuthenticatedRequest
import repositories.UserDataRepository

import scala.concurrent.{ExecutionContext, Future}

class InMemoryUserDetailsRepository()(implicit ec: ExecutionContext) extends UserDataRepository {

  private var cache = scala.collection.mutable.Map[String, Any]()

  override def putData[T: Writes](key: String, data: T)(implicit
    request: AuthenticatedRequest[Any]
  ): Future[T] =
    Future.successful(cache.put(key, data)).map(_ => data)

  override def putData[T: Writes](id: String, key: String, data: T): Future[T] =
    Future.successful(cache.put(key, data)).map(_ => data)

  override def getData[T: Reads](
    key: String
  )(implicit request: AuthenticatedRequest[Any]): Future[Option[T]] =
    Future.successful(cache.get(key).asInstanceOf[Option[T]])

  override def getData[T: Reads](id: String, key: String): Future[Option[T]] =
    Future.successful(cache.get(key).asInstanceOf[Option[T]])

  override def deleteData[T: Writes](
    key: String
  )(implicit request: AuthenticatedRequest[Any]): Future[Unit] =
    Future.successful(cache.remove(key))

  override def deleteData[T: Writes](id: String, key: String): Future[Unit] =
    Future.successful(cache.remove(key))

  override def updateData[T: Reads: Writes](key: String, updater: T => T)(implicit
    request: AuthenticatedRequest[Any]
  ): Future[Unit] =
    Future.successful(
      cache.put(key, cache.get(key).map(data => data.asInstanceOf[T]).map(data => updater(data)))
    )

  override def reset(): Unit = cache = scala.collection.mutable.Map[String, Any]()

}
