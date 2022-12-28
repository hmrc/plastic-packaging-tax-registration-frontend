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

package services

import com.google.inject.Inject
import uk.gov.hmrc.http.HeaderCarrier
import connectors.{
  EmailVerificationConnector,
  ServiceError
}
import models.emailverification._

import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailVerificationService @Inject() (emailVerificationConnector: EmailVerificationConnector)(
  implicit ec: ExecutionContext
) {

  def getStatus(
    credId: String
  )(implicit hc: HeaderCarrier): Future[Either[ServiceError, Option[VerificationStatus]]] =
    emailVerificationConnector.getStatus(credId)

  def isEmailVerified(email: String, credId: String)(implicit hc: HeaderCarrier): Future[Boolean] =
    getStatus(credId).map {
      case Right(resp) =>
        resp.exists { emailStatuses =>
          emailStatuses.emails.exists {
            case es @ EmailStatus(_, true, _) if es.emailAddress == email => true
            case _                                                        => false
          }
        }
      case Left(ex) => throw ex
    }

  def sendVerificationCode(email: String, credId: String, continueUrl: String)(implicit
    hc: HeaderCarrier
  ): Future[String] =
    emailVerificationConnector.create(
      CreateEmailVerificationRequest(credId = credId,
                                     continueUrl = continueUrl,
                                     origin = "ppt",
                                     accessibilityStatementUrl = "/accessibility",
                                     email = Email(address = email, enterUrl = "/start"),
                                     backUrl = "/back",
                                     pageTitle = "PPT Title",
                                     deskproServiceName = "plastic-packaging-tax"
      )
    ).map {
      case Right(resp) => resp.split("/").slice(0, 4).last
      case Left(ex)    => throw ex
    }

  def checkVerificationCode(code: String, email: String, journeyId: String)(implicit
    hc: HeaderCarrier
  ): Future[EmailVerificationJourneyStatus.Value] =
    emailVerificationConnector.verifyPasscode(journeyId, VerifyPasscodeRequest(code, email)).map {
      case Right(verificationStatus) => verificationStatus
      case Left(ex)                  => throw ex
    }

}
