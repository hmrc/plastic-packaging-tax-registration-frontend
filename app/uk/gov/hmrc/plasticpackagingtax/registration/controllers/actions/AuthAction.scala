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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions

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
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.routes
import uk.gov.hmrc.plasticpackagingtax.registration.models.SignedInUser
import uk.gov.hmrc.plasticpackagingtax.registration.models.enrolment.PptEnrolment
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{AuthenticatedRequest, IdentityData}
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

class AuthActionImpl @Inject() (
  override val authConnector: AuthConnector,
  allowedUsers: AllowedUsers,
  metrics: Metrics,
  mcc: MessagesControllerComponents,
  appConfig: AppConfig
) extends AuthActionBase(authConnector, allowedUsers, metrics, mcc, appConfig) with AuthAction {
  override val checkAlreadyEnrolled: Boolean = true
}

class AuthNoEnrolmentCheckActionImpl @Inject() (
  override val authConnector: AuthConnector,
  allowedUsers: AllowedUsers,
  metrics: Metrics,
  mcc: MessagesControllerComponents,
  appConfig: AppConfig
) extends AuthActionBase(authConnector, allowedUsers, metrics, mcc, appConfig)
    with AuthNoEnrolmentCheckAction {
  override val checkAlreadyEnrolled: Boolean = false
}

abstract class AuthActionBase @Inject() (
  override val authConnector: AuthConnector,
  allowedUsers: AllowedUsers,
  metrics: Metrics,
  mcc: MessagesControllerComponents,
  appConfig: AppConfig
) extends ActionBuilder[AuthenticatedRequest, AnyContent]
    with ActionFunction[Request, AuthenticatedRequest] with AuthorisedFunctions {

  val checkAlreadyEnrolled: Boolean

  implicit override val executionContext: ExecutionContext = mcc.executionContext
  override val parser: BodyParser[AnyContent]              = mcc.parsers.defaultBodyParser
  private val logger                                       = Logger(this.getClass)
  private val authTimer                                    = metrics.defaultRegistry.timer("ppt.registration.upstream.auth.timer")

  private val authData =
    credentials and name and email and externalId and internalId and affinityGroup and allEnrolments and
      agentCode and confidenceLevel and nino and saUtr and dateOfBirth and agentInformation and groupIdentifier and
      credentialRole and mdtpInformation and itmpName and itmpDateOfBirth and itmpAddress and credentialStrength and loginTimes

  override def invokeBlock[A](
    request: Request[A],
    block: AuthenticatedRequest[A] => Future[Result]
  ): Future[Result] = {
    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    def getSelectedClientIdentifier(): Option[String] = request.session.get("clientPPT")

    def authPredicate: Predicate = {
      // TODO restore; also not used in returns
      // CredentialStrength(CredentialStrength.strong)

      // TODO Using Enrolment in this way is locking out the registration journey
      // Needs to be conditional; may be what checkAlreadyEnrolled is doing or a new switch

      getSelectedClientIdentifier.map { clientIdentifier =>
        // If this request is decorated with a selected client identifier this indicates
        // an agent at work; we need to request the delegated authority
        Enrolment(PptEnrolment.Identifier).withIdentifier(PptEnrolment.Key,
          clientIdentifier
        ).withDelegatedAuthRule("ppt-auth")
      }.getOrElse {
        Enrolment(PptEnrolment.Identifier)
      }
    }

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

          executeRequest(request, block, identityData, email.getOrElse(""), allEnrolments)
      } recover {
      case _: NoActiveSession =>
        Results.Redirect(appConfig.loginUrl, Map("continue" -> Seq(appConfig.loginContinueUrl)))
      case _: IncorrectCredentialStrength =>
        upliftCredentialStrength()
      case _: AuthorisationException =>
        Results.Redirect(routes.UnauthorisedController.onPageLoad())

    }
  }

  private def upliftCredentialStrength[A](): Result =
    Redirect(appConfig.mfaUpliftUrl,
             Map("origin"      -> Seq(appConfig.serviceIdentifier),
                 "continueUrl" -> Seq(appConfig.loginContinueUrl)
             )
    )

  private def executeRequest[A](
    request: Request[A],
    block: AuthenticatedRequest[A] => Future[Result],
    identityData: IdentityData,
    email: String,
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
    logger.info("Authed with affinity group " + identityData.affinityGroup + " and ppt reference " + pptReference)

    if (checkAlreadyEnrolled && pptReference.isDefined)
      Future.successful(Results.Redirect(appConfig.pptAccountUrl))

    else if (allowedUsers.isAllowed(email, identityData.affinityGroup))
      block {
        val user =
          SignedInUser(allEnrolments,
                       identityData,
                       allowedUsers.getUserFeatures(email).getOrElse(appConfig.defaultFeatures)
          )
        new AuthenticatedRequest(request, user, appConfig, pptReference)
      }
    else {
      logger.warn("User is not allowed, access denied")
      Future.successful(Results.Redirect(routes.UnauthorisedController.onPageLoad()))
    }
  }

}

trait AuthActioning
    extends ActionBuilder[AuthenticatedRequest, AnyContent]
    with ActionFunction[Request, AuthenticatedRequest]

@ImplementedBy(classOf[AuthActionImpl])
trait AuthAction extends AuthActioning

@ImplementedBy(classOf[AuthNoEnrolmentCheckActionImpl])
trait AuthNoEnrolmentCheckAction extends AuthActioning
