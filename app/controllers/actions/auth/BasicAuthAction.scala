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

package controllers.actions.auth

import com.google.inject.{ImplementedBy, Inject}
import config.AppConfig
import models.request.AuthenticatedRequest.RegistrationRequest
import models.request.IdentityData
import play.api.Logging
import play.api.mvc.Results.Redirect
import play.api.mvc._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{credentials, internalId}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[BasicAuthActionImpl])
trait BasicAuthAction extends ActionBuilder[RegistrationRequest, AnyContent]

class BasicAuthActionImpl @Inject() (override val authConnector: AuthConnector, override val parser: BodyParsers.Default, appConfig: AppConfig)(implicit
  val executionContext: ExecutionContext
) extends BasicAuthAction with ActionFunction[Request, RegistrationRequest] with AuthorisedFunctions with Logging {

  private val retrievals = internalId and credentials

  override def invokeBlock[A](request: Request[A], block: RegistrationRequest[A] => Future[Result]): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    val continueUrl = request.target.path

    authorised(CredentialStrength(CredentialStrength.strong).or(AffinityGroup.Agent))
      .retrieve(retrievals) {
        case internalId ~ credentials => block(RegistrationRequest(request, identityData = IdentityData(internalId, credentials)))
      } recover {
      case _: NoActiveSession =>
        Results.Redirect(appConfig.loginUrl, Map("continue" -> Seq(continueUrl)))
      case _: IncorrectCredentialStrength => upliftCredentialStrength(continueUrl)
      case _: UnsupportedCredentialRole =>
        Results.Redirect(controllers.unauthorised.routes.UnauthorisedController.showAssistantUnauthorised())
      case _: UnsupportedAffinityGroup =>
        Results.Redirect(controllers.unauthorised.routes.UnauthorisedController.showAgentUnauthorised())
      case _: AuthorisationException =>
        Results.Redirect(controllers.unauthorised.routes.UnauthorisedController.showGenericUnauthorised())
    }
  }

  private def upliftCredentialStrength(continueUrl: String): Result =
    Redirect(appConfig.mfaUpliftUrl, Map("origin" -> Seq(appConfig.serviceIdentifier), "continueUrl" -> Seq(continueUrl)))

}
