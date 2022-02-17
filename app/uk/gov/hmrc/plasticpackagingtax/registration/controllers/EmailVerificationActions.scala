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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers

import play.api.mvc.AnyContent
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.JourneyRequest
import uk.gov.hmrc.plasticpackagingtax.registration.services.EmailVerificationService

import scala.concurrent.{ExecutionContext, Future}

trait EmailVerificationActions {

  def emailVerificationService: EmailVerificationService

  def isEmailVerificationRequired(email: String, isEmailChanged: String => Boolean)(implicit
    request: JourneyRequest[AnyContent],
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Boolean] =
    if (isEmailChanged(email))
      emailVerificationService.isEmailVerified(email, request.user.credId).map(!_)
    else
      Future.successful(false)

}
