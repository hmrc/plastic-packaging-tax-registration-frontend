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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers

import com.kenshoo.play.metrics.Metrics
import javax.inject.{Inject, Singleton}
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.plasticpackagingtax.registration.audit.Auditor
import uk.gov.hmrc.plasticpackagingtax.registration.connectors._
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthAction
import uk.gov.hmrc.plasticpackagingtax.registration.forms.OrgType
import uk.gov.hmrc.plasticpackagingtax.registration.forms.OrgType.{
  PARTNERSHIP,
  REGISTERED_SOCIETY,
  SOLE_TRADER,
  UK_COMPANY
}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.PartnershipTypeEnum.{
  GENERAL_PARTNERSHIP,
  SCOTTISH_PARTNERSHIP
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.{
  IncorporationDetails,
  PartnershipDetails,
  SoleTraderIncorporationDetails
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.nrs.NrsDetails
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{Cacheable, Registration}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.models.response.FlashKeys
import uk.gov.hmrc.plasticpackagingtax.registration.models.subscriptions.{
  EisError,
  SubscriptionCreateResponseFailure,
  SubscriptionCreateResponseSuccess
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.review_registration_page
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ReviewRegistrationController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  mcc: MessagesControllerComponents,
  subscriptionsConnector: SubscriptionsConnector,
  metrics: Metrics,
  override val registrationConnector: RegistrationConnector,
  auditor: Auditor,
  reviewRegistrationPage: review_registration_page
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport {

  private val successSubmissionCounter =
    metrics.defaultRegistry.counter("ppt.registration.success.submission.counter")

  private val failedSubmissionCounter =
    metrics.defaultRegistry.counter("ppt.registration.failed.submission.counter")

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      if (request.registration.isCheckAndSubmitReady)
        markRegistrationAsReviewed().map { _ =>
          // The call to check that the registration is in a suitable state before this means that
          // exhaustive matching is not needed
          (request.registration.organisationDetails.organisationType: @unchecked) match {
            case Some(UK_COMPANY) | Some(REGISTERED_SOCIETY) => incorpEntityReview()
            case Some(SOLE_TRADER)                           => soleTraderReview()
            case Some(PARTNERSHIP)                           => partnershipReview()
          }
        }
      else
        Future(Redirect(routes.RegistrationController.displayPage()))
    }

  private def soleTraderReview()(implicit request: JourneyRequest[AnyContent]) =
    Ok(
      reviewRegistrationPage(registration = request.registration,
                             soleTraderDetails = getSoleTraderDetails()
      )
    )

  private def partnershipReview()(implicit request: JourneyRequest[AnyContent]) =
    Ok(
      reviewRegistrationPage(registration = request.registration,
                             partnershipDetails = getPartnershipDetails()
      )
    )

  private def incorpEntityReview()(implicit request: JourneyRequest[AnyContent]) =
    Ok(
      reviewRegistrationPage(registration = request.registration,
                             incorporationDetails = getIncorporationDetails()
      )
    )

  private def markRegistrationAsReviewed()(implicit
    req: JourneyRequest[AnyContent]
  ): Future[Either[ServiceError, Registration]] =
    update { registration =>
      val updatedMetaData =
        registration.metaData.copy(registrationReviewed = true)
      registration.copy(metaData = updatedMetaData)
    }

  def submit(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      val completedRegistration = request.registration.asCompleted()
      val completedRegistrationWithUserHeaders =
        completedRegistration.copy(userHeaders = Some(request.headers.toSimpleMap))

      subscriptionsConnector.submitSubscription(getSafeId(completedRegistration),
                                                completedRegistrationWithUserHeaders
      )
        .map {
          case successfulSubscription @ SubscriptionCreateResponseSuccess(_, _, _, _, _, _, _) =>
            handleSuccessfulSubscription(completedRegistration, successfulSubscription)
          case SubscriptionCreateResponseFailure(failures) =>
            handleFailedSubscription(completedRegistration, failures)
        }
        .recoverWith {
          case _ => Future.successful(handleFailedSubscription(completedRegistration))
        }
    }

  private def handleSuccessfulSubscription(
    registration: Registration,
    response: SubscriptionCreateResponseSuccess
  )(implicit hc: HeaderCarrier): Result = {
    successSubmissionCounter.inc()
    val updatedMetadata = registration.metaData.copy(nrsDetails =
      Some(NrsDetails(response.nrsSubmissionId, response.nrsFailureReason))
    )
    auditor.registrationSubmitted(registration.copy(metaData = updatedMetadata),
                                  Some(response.pptReference)
    )
    if (response.enrolmentInitiatedSuccessfully)
      Redirect(routes.ConfirmationController.displayPage())
        .flashing(Flash(Map(FlashKeys.referenceId -> response.pptReference)))
    else
      Redirect(routes.NotableErrorController.enrolmentFailure())
  }

  private def handleFailedSubscription(registration: Registration, failures: Seq[EisError])(implicit
    hc: HeaderCarrier
  ): Result = {
    performFailedSubscriptionCommonTasks(registration)
    if (failures.head.isDuplicateSubscription)
      Redirect(routes.NotableErrorController.duplicateRegistration())
    else
      Redirect(routes.NotableErrorController.subscriptionFailure())
  }

  private def handleFailedSubscription(
    registration: Registration
  )(implicit hc: HeaderCarrier): Result = {
    performFailedSubscriptionCommonTasks(registration)
    Redirect(routes.NotableErrorController.subscriptionFailure())
  }

  private def performFailedSubscriptionCommonTasks(
    registration: Registration
  )(implicit hc: HeaderCarrier): Unit = {
    failedSubmissionCounter.inc()
    auditor.registrationSubmitted(registration)
  }

  private def getSafeId(registration: Registration): String =
    registration.organisationDetails.organisationType.flatMap {
      case OrgType.SOLE_TRADER =>
        registration.organisationDetails.soleTraderDetails.flatMap(
          soleTraderDetails => soleTraderDetails.registration.registeredBusinessPartnerId
        )
      case OrgType.PARTNERSHIP =>
        registration.organisationDetails.partnershipDetails.flatMap(
          partnershipDetails =>
            partnershipDetails.partnershipType match {
              case GENERAL_PARTNERSHIP =>
                partnershipDetails.generalPartnershipDetails.flatMap(
                  generalPartnershipDetails =>
                    generalPartnershipDetails.registration.registeredBusinessPartnerId
                )
              case SCOTTISH_PARTNERSHIP =>
                partnershipDetails.scottishPartnershipDetails.flatMap(
                  scottishPartnershipDetails =>
                    scottishPartnershipDetails.registration.registeredBusinessPartnerId
                )
              case _ => throw new IllegalStateException("Illegal partnership type")
            }
        )
      case _ =>
        registration.organisationDetails.incorporationDetails.flatMap(
          details => details.registration.registeredBusinessPartnerId
        )
    }.getOrElse(throw new IllegalStateException("Safe Id is required for a Subscription create"))

  private def getSoleTraderDetails()(implicit
    request: JourneyRequest[AnyContent]
  ): Option[SoleTraderIncorporationDetails] =
    request.registration.organisationDetails.soleTraderDetails

  private def getPartnershipDetails()(implicit
    request: JourneyRequest[AnyContent]
  ): Option[PartnershipDetails] =
    request.registration.organisationDetails.partnershipDetails

  private def getIncorporationDetails()(implicit
    request: JourneyRequest[AnyContent]
  ): Option[IncorporationDetails] =
    request.registration.organisationDetails.incorporationDetails

}
