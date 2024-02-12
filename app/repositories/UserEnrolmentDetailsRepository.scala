/*
 * Copyright 2024 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}
import models.registration.UserEnrolmentDetails
import models.request.AuthenticatedRequest
import repositories.UserEnrolmentDetailsRepository.repositoryKey

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserEnrolmentDetailsRepository @Inject() (userDataRepository: UserDataRepository)(implicit executionContext: ExecutionContext) {

  def put(
    registration: UserEnrolmentDetails
  )(implicit request: AuthenticatedRequest[Any]): Future[UserEnrolmentDetails] =
    userDataRepository.putData[UserEnrolmentDetails](repositoryKey, registration)

  def get()(implicit request: AuthenticatedRequest[Any]): Future[UserEnrolmentDetails] =
    userDataRepository.getData[UserEnrolmentDetails](repositoryKey).map {
      case Some(data) => data
      case _          => UserEnrolmentDetails()
    }

  def update(
    update: UserEnrolmentDetails => UserEnrolmentDetails
  )(implicit request: AuthenticatedRequest[Any]): Future[UserEnrolmentDetails] =
    get().flatMap(data => put(update(data)))

  def delete()(implicit request: AuthenticatedRequest[Any]): Future[Unit] =
    userDataRepository.deleteData[UserEnrolmentDetails](repositoryKey)

}

object UserEnrolmentDetailsRepository {
  val repositoryKey = "userEnrolmentDetails"
}
