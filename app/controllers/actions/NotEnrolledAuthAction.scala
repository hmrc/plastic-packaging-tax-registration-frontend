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

package controllers.actions

import com.google.inject.{ImplementedBy, Inject}
import com.kenshoo.play.metrics.Metrics
import play.api.Logger
import play.api.mvc.Results.Redirect
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import config.AppConfig
import controllers.unauthorised.{routes => unauthorisedRoutes}
import models.SignedInUser
import models.enrolment.PptEnrolment
import models.request.{AuthenticatedRequest, IdentityData}
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

class NotEnrolledAuthActionImpl @Inject()(
  override val authConnector: AuthConnector,
  metrics: Metrics,
  mcc: MessagesControllerComponents,
  appConfig: AppConfig
) extends AuthActionBase(authConnector, metrics, mcc, appConfig) with NotEnrolledAuthAction {
  override val mustBeEnrolled: Boolean                 = false
  override val redirectEnrolledUsersToReturns: Boolean = true
  override val agentsAllowed: Boolean                  = false
}

class EnrolledAuthActionImpl @Inject()(
  override val authConnector: AuthConnector,
  metrics: Metrics,
  mcc: MessagesControllerComponents,
  appConfig: AppConfig
) extends AuthActionBase(authConnector, metrics, mcc, appConfig) with EnrolledAuthAction {
  override val mustBeEnrolled: Boolean                 = true
  override val redirectEnrolledUsersToReturns: Boolean = false
  override val agentsAllowed: Boolean                  = true
}

class PermissiveAuthActionImpl @Inject()(
  override val authConnector: AuthConnector,
  metrics: Metrics,
  mcc: MessagesControllerComponents,
  appConfig: AppConfig
) extends AuthActionBase(authConnector, metrics, mcc, appConfig) with PermissiveAuthAction {
  override val mustBeEnrolled: Boolean                 = false
  override val redirectEnrolledUsersToReturns: Boolean = false
  override val agentsAllowed: Boolean                  = true
}

abstract class AuthActionBase @Inject() (
  override val authConnector: AuthConnector,
  metrics: Metrics,
  mcc: MessagesControllerComponents,
  appConfig: AppConfig
) extends ActionBuilder[AuthenticatedRequest, AnyContent]
    with ActionFunction[Request, AuthenticatedRequest] with AuthorisedFunctions {

  val mustBeEnrolled: Boolean
  val redirectEnrolledUsersToReturns: Boolean
  val agentsAllowed: Boolean

  implicit override val executionContext: ExecutionContext = mcc.executionContext
  override val parser: BodyParser[AnyContent]              = mcc.parsers.defaultBodyParser
  private val logger                                       = Logger(this.getClass)
  private val authTimer                                    = metrics.defaultRegistry.timer("ppt.registration.upstream.auth.timer")

  private val authData =
    credentials and name and email and externalId and internalId and affinityGroup and allEnrolments and
      agentCode and confidenceLevel and nino and saUtr and dateOfBirth and agentInformation and groupIdentifier and
      credentialRole and mdtpInformation and itmpName and itmpDateOfBirth and itmpAddress and credentialStrength and loginTimes

  private def acceptableCredentialStrength: Predicate = {
    val strongCredentials = CredentialStrength(CredentialStrength.strong)
    // Agents are allowed to use weak credentials
    // The order of this clause is important if we wish to preserve the MFA uplift of non agents.
    // If an auth OR clause evaluates to false, the auth AlternateAuthPredicate will respond with an exception
    // matching the last clause it evaluated. Strong credentials needs to be the last clause if we want to catch it
    AffinityGroup.Agent.or(strongCredentials)
  }

  override def invokeBlock[A](
    request: Request[A],
    block: AuthenticatedRequest[A] => Future[Result]
  ): Future[Result] = {
    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    def getSelectedClientIdentifier(): Option[String] = request.session.get("clientPPT")

    def authPredicate: Predicate =
      User.and(
        if (!mustBeEnrolled)
          acceptableCredentialStrength
        else
          getSelectedClientIdentifier().map { clientIdentifier =>
            // If this request is decorated with a selected client identifier this indicates
            // an agent at work; we need to request the delegated authority
            Enrolment(PptEnrolment.Identifier).withIdentifier(PptEnrolment.Key,
                                                              clientIdentifier
            ).withDelegatedAuthRule("ppt-auth")
          }.getOrElse {
            Enrolment(PptEnrolment.Identifier)
          }.and(acceptableCredentialStrength)
      )

    val continueUrl = request.target.path
    val authorisation = authTimer.time()

    authorised(authPredicate)
      .retrieve(authData) {
        case credentials ~ name ~ email ~ externalId ~ internalId ~ affinityGroup ~ allEnrolments ~ agentCode ~
            confidenceLevel ~ authNino ~ saUtr ~ dateOfBirth ~ agentInformation ~ groupIdentifier ~
            credentialRole ~ mdtpInformation ~ itmpName ~ itmpDateOfBirth ~ itmpAddress ~ credentialStrength ~ loginTimes =>
          authorisation.stop()
          val identityData = IdentityData(internalId,
                                          externalId,
                                          agentCode,
                                          credentials,
                                          Some(confidenceLevel),
                                          authNino,
                                          saUtr,
                                          name,
                                          dateOfBirth,
                                          email,
                                          Some(agentInformation),
                                          groupIdentifier,
                                          credentialRole.map(res => res.toJson.toString()),
                                          mdtpInformation,
                                          itmpName,
                                          itmpDateOfBirth,
                                          itmpAddress,
                                          affinityGroup,
                                          credentialStrength,
                                          Some(loginTimes)
          )

          executeRequest(request, block, identityData, allEnrolments)
      } recover {
      case _: NoActiveSession =>
        Results.Redirect(appConfig.loginUrl, Map("continue" -> Seq(continueUrl)))
      case _: IncorrectCredentialStrength =>
        upliftCredentialStrength(continueUrl)
      case _: InsufficientEnrolments =>
        // Returns has the best not enrolled explanation page and knows how to handle agents in this state
        Results.Redirect(appConfig.pptAccountUrl)
      case _: UnsupportedCredentialRole  =>
        Results.Redirect(unauthorisedRoutes.UnauthorisedController.showAssistantUnauthorised())
      case _: UnsupportedAffinityGroup =>
        Results.Redirect(unauthorisedRoutes.UnauthorisedController.showAgentUnauthorised())
      case _: AuthorisationException =>
        Results.Redirect(unauthorisedRoutes.UnauthorisedController.showGenericUnauthorised())
    }
  }

  private def upliftCredentialStrength[A](continueUrl: String): Result =
    Redirect(appConfig.mfaUpliftUrl,
             Map("origin"      -> Seq(appConfig.serviceIdentifier),
                 "continueUrl" -> Seq(continueUrl)
             )
    )

  private def executeRequest[A](
    request: Request[A],
    block: AuthenticatedRequest[A] => Future[Result],
    identityData: IdentityData,
    allEnrolments: Enrolments
  ) = {

    def getSelectedClientIdentifier(): Option[String] = request.session.get("clientPPT")

    val pptReference = {
      // The source of the ppt reference will be different if this is an agent request
      identityData.affinityGroup match {
        case Some(AffinityGroup.Agent) =>
          getSelectedClientIdentifier()
        case _ =>
          val pptEnrolment = allEnrolments.getEnrolment(PptEnrolment.Identifier)
          pptEnrolment.flatMap(enrolment => enrolment.getIdentifier(PptEnrolment.Key)).map(
            id => id.value
          )
      }
    }

    if (identityData.affinityGroup.contains(AffinityGroup.Agent) && !agentsAllowed)
      throw UnsupportedAffinityGroup()

    if (pptReference.isDefined && redirectEnrolledUsersToReturns)
      Future.successful(Results.Redirect(appConfig.pptAccountUrl))
    else if (identityData.affinityGroup.contains(AffinityGroup.Agent) && pptReference.isEmpty)
      Future.successful(Results.Redirect(appConfig.pptAccountUrl))
    else
      block {
        val user =
          SignedInUser(allEnrolments, identityData)
        new AuthenticatedRequest(request, user, pptReference)
      }
  }

}

trait AuthActioning
    extends ActionBuilder[AuthenticatedRequest, AnyContent]
    with ActionFunction[Request, AuthenticatedRequest]

@ImplementedBy(classOf[NotEnrolledAuthActionImpl])
trait NotEnrolledAuthAction extends AuthActioning

@ImplementedBy(classOf[EnrolledAuthActionImpl])
trait EnrolledAuthAction extends AuthActioning

@ImplementedBy(classOf[PermissiveAuthActionImpl])
trait PermissiveAuthAction extends AuthActioning
