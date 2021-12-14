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

import com.google.inject.{ImplementedBy, Inject, Singleton}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Registration
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.AuthenticatedRequest
import uk.gov.hmrc.plasticpackagingtax.registration.repositories.MongoRegistrationAmendmentRepository.repositoryKey

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[MongoRegistrationAmendmentRepository])
trait RegistrationAmendmentRepository {

  def put(id: String, registration: Registration): Future[Registration]

  def put(registration: Registration)(implicit
    request: AuthenticatedRequest[Any]
  ): Future[Registration]

  def get(id: String): Future[Option[Registration]]

  def get()(implicit request: AuthenticatedRequest[Any]): Future[Option[Registration]]

  def update(updateRegistration: Registration => Registration)(implicit
    request: AuthenticatedRequest[Any]
  ): Future[Registration]

}

@Singleton
class MongoRegistrationAmendmentRepository @Inject() (userDataRepository: UserDataRepository)(
  implicit ec: ExecutionContext
) extends RegistrationAmendmentRepository {

  def put(id: String, registration: Registration): Future[Registration] =
    userDataRepository.putData[Registration](id, repositoryKey, registration)

  def put(
    registration: Registration
  )(implicit request: AuthenticatedRequest[Any]): Future[Registration] =
    userDataRepository.putData[Registration](repositoryKey, registration)

  def get(id: String): Future[Option[Registration]] =
    userDataRepository.getData[Registration](id, repositoryKey)

  def get()(implicit request: AuthenticatedRequest[Any]): Future[Option[Registration]] =
    userDataRepository.getData[Registration](repositoryKey)

  def update(
    updateRegistration: Registration => Registration
  )(implicit request: AuthenticatedRequest[Any]): Future[Registration] =
    get().flatMap {
      case Some(registration) => put(updateRegistration(registration))
      case _                  => throw new IllegalStateException("Missing registration in user data repository")
    }

}

object MongoRegistrationAmendmentRepository {
  val repositoryKey = "registrationAmendment"
}
