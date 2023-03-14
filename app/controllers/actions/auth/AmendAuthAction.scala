/*
 * Copyright 2023 HM Revenue & Customs
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

import com.google.inject.Inject
import config.AppConfig
import models.enrolment.PptEnrolment
import models.request.AuthenticatedRequest.PPTEnrolledRequest
import models.request.{AuthenticatedRequest, IdentityData}
import play.api.Logging
import play.api.mvc.Results.Redirect
import play.api.mvc._
import repositories.ReturnsSessionRepository
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{affinityGroup, allEnrolments, credentials, internalId}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}


//todo should this be called enrolledAuthAction or something? While its mostly use for amend there is exceptions
class AmendAuthAction @Inject()(
                                         override val authConnector: AuthConnector,
                                         controllerComponents: ControllerComponents,
                                         returnsSessionRepository: ReturnsSessionRepository,
                                         appConfig: AppConfig
                                       )(implicit val executionContext: ExecutionContext) extends ActionBuilder[PPTEnrolledRequest, AnyContent]
  with ActionFunction[Request, PPTEnrolledRequest] with AuthorisedFunctions with Logging {

  private val retrievals = credentials and internalId and allEnrolments and affinityGroup

  override def invokeBlock[A](
                               request: Request[A],
                               block: PPTEnrolledRequest[A] => Future[Result]
                             ): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    val continueUrl = request.target.path

    authorised(
      AffinityGroup.Agent.or(Enrolment(PptEnrolment.Key).and(CredentialStrength(CredentialStrength.strong)))
    ).retrieve(retrievals) {
        case credentials ~ internalId ~ _ ~ affinityGroup if affinityGroup.contains(AffinityGroup.Agent) =>
          returnsSessionRepository.getAgentSelectedPPTRef(internalId.getOrElse(throw new IllegalStateException("User has no Internal ID")))
            .flatMap{
              case Some(selectedPPTRef) => block(PPTEnrolledRequest(request, IdentityData(internalId, credentials), selectedPPTRef))
              case None => Future.successful(Redirect(appConfig.pptAccountUrl)) //todo to the returns select page, or home page? thinking
            }

        case credentials ~ internalId ~ allEnrolments ~ _ =>
          val pptIdentifier = allEnrolments
            .getEnrolment(PptEnrolment.Key).getOrElse(throw new IllegalStateException("No PPT Enrolment found"))
            .getIdentifier(PptEnrolment.Identifier).getOrElse(throw new IllegalStateException("PPT enrolment has no identifier"))
            .value

          block(PPTEnrolledRequest(request, IdentityData(internalId, credentials), pptIdentifier))
      } recover {
      case _: NoActiveSession =>
        Results.Redirect(appConfig.loginUrl, Map("continue" -> Seq(continueUrl)))
      case _: IncorrectCredentialStrength =>
        upliftCredentialStrength(continueUrl)
      case _: InsufficientEnrolments =>
        // Returns has the best not enrolled explanation page and knows how to handle agents in this state
        //todo wtf
        Results.Redirect(appConfig.pptAccountUrl)
      case _: UnsupportedCredentialRole =>
        Results.Redirect(controllers.unauthorised.routes.UnauthorisedController.showAssistantUnauthorised())
      case _: UnsupportedAffinityGroup =>
        Results.Redirect(controllers.unauthorised.routes.UnauthorisedController.showAgentUnauthorised())
      case _: AuthorisationException =>
        Results.Redirect(controllers.unauthorised.routes.UnauthorisedController.showGenericUnauthorised())
    }
  }

  private def upliftCredentialStrength(continueUrl: String): Result =
    Redirect(appConfig.mfaUpliftUrl,
      Map("origin" -> Seq(appConfig.serviceIdentifier),
        "continueUrl" -> Seq(continueUrl)
      )
    )

  //todo better way to get this?
  override def parser: BodyParser[AnyContent] = controllerComponents.parsers.default
}