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
import models.deregistration.DeregistrationDetails
import models.request.AuthenticatedRequest
import repositories.DeregistrationDetailsRepository.repositoryKey

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[DeregistrationDetailRepositoryImpl])
trait DeregistrationDetailRepository {

  def put(deregistrationDetails: DeregistrationDetails)(implicit
    request: AuthenticatedRequest[Any]
  ): Future[DeregistrationDetails]

  def get()(implicit request: AuthenticatedRequest[Any]): Future[DeregistrationDetails]

  def update(update: DeregistrationDetails => DeregistrationDetails)(implicit
    request: AuthenticatedRequest[Any]
  ): Future[DeregistrationDetails]

  def delete()(implicit request: AuthenticatedRequest[Any]): Future[Unit]
}

@Singleton
class DeregistrationDetailRepositoryImpl @Inject() (userDataRepository: UserDataRepository)(implicit executionContext: ExecutionContext)
    extends DeregistrationDetailRepository {

  def put(
    deregistrationDetails: DeregistrationDetails
  )(implicit request: AuthenticatedRequest[Any]): Future[DeregistrationDetails] =
    userDataRepository.putData[DeregistrationDetails](repositoryKey, deregistrationDetails)

  def get()(implicit request: AuthenticatedRequest[Any]): Future[DeregistrationDetails] =
    userDataRepository.getData[DeregistrationDetails](repositoryKey).map {
      case Some(deregistrationDetails) => deregistrationDetails
      case None                        => DeregistrationDetails(None, None)
    }

  def update(
    update: DeregistrationDetails => DeregistrationDetails
  )(implicit request: AuthenticatedRequest[Any]): Future[DeregistrationDetails] =
    get().flatMap(deregistrationDetails => put(update(deregistrationDetails)))

  def delete()(implicit request: AuthenticatedRequest[Any]): Future[Unit] =
    userDataRepository.deleteData[DeregistrationDetails](repositoryKey)

  def reset(): Unit = userDataRepository.reset()
}

object DeregistrationDetailsRepository {
  val repositoryKey = "deregistrationDetails"
}
