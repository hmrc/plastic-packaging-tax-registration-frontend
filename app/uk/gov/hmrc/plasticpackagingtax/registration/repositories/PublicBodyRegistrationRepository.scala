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
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.PublicBodyRegistration
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.AuthenticatedRequest

import scala.concurrent.Future

@Singleton
class PublicBodyRegistrationRepository @Inject() (userDataRepository: UserDataRepository) {

  private val publicBodyRegistrationKey = "publicBodyRegistration"

  def put(
    registration: PublicBodyRegistration
  )(implicit request: AuthenticatedRequest[Any]): Future[PublicBodyRegistration] =
    userDataRepository.putData[PublicBodyRegistration](publicBodyRegistrationKey, registration)

  def get()(implicit request: AuthenticatedRequest[Any]): Future[Option[PublicBodyRegistration]] =
    userDataRepository.getData[PublicBodyRegistration](publicBodyRegistrationKey)

}
