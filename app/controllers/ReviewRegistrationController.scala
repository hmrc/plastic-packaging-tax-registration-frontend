/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers

import audit.Auditor
import uk.gov.hmrc.play.bootstrap.metrics.Metrics
import config.AppConfig
import connectors._
import controllers.actions.JourneyAction
import models.nrs.NrsDetails
import models.registration.{Cacheable, Registration}
import models.request.JourneyRequest
import models.response.FlashKeys
import models.subscriptions.{EisError, SubscriptionCreateOrUpdateResponseFailure, SubscriptionCreateOrUpdateResponseSuccess}
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.RegistrationGroupFilterService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.review_registration_page

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ReviewRegistrationController @Inject() (
  appConfig: AppConfig,
  journeyAction: JourneyAction,
  mcc: MessagesControllerComponents,
  subscriptionsConnector: SubscriptionsConnector,
  metrics: Metrics,
  override val registrationConnector: RegistrationConnector,
  auditor: Auditor,
  reviewRegistrationPage: review_registration_page,
  registrationFilterService: RegistrationGroupFilterService
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc)
    with Cacheable
    with I18nSupport {

  private val successSubmissionCounter =
    metrics.defaultRegistry.counter("ppt.registration.success.submission.counter")

  private val failedSubmissionCounter =
    metrics.defaultRegistry.counter("ppt.registration.failed.submission.counter")

  def displayPage(): Action[AnyContent] =
    journeyAction.register.async { implicit request =>
      if (request.registration.isCheckAndSubmitReady)
        displayReviewRegistrationPage()
      else
        Future.successful(Redirect(routes.TaskListController.displayPage()))
    }

  def submit(): Action[AnyContent] =
    journeyAction.register.async { implicit request =>
      // Don't let user submit with old liability answers
      if (!request.registration.hasOldLiabilityQuestions)
        submitRegistration(request)
      else
        Future.successful(Redirect(routes.TaskListController.displayPage()))
    }

  private def submitRegistration(request: JourneyRequest[AnyContent])(implicit hc: HeaderCarrier) = {
    val completedRegistration = request.registration.asCompleted()
    val internalId            = request.authenticatedRequest.identityData.internalId
    val completedRegistrationWithUserHeaders =
      completedRegistration.copy(userHeaders = Some(request.headers.toSimpleMap))

    subscriptionsConnector.submitSubscription(getSafeId(completedRegistration), completedRegistrationWithUserHeaders)
      .map {
        case successfulSubscription: SubscriptionCreateOrUpdateResponseSuccess =>
          handleSuccessfulSubscription(completedRegistration, successfulSubscription, internalId)
        case SubscriptionCreateOrUpdateResponseFailure(failures) =>
          handleFailedSubscription(completedRegistration, failures, internalId)
      }
      .recoverWith { case _ =>
        Future.successful(handleFailedSubscription(completedRegistration, internalId))
      }
  }

  private def handleSuccessfulSubscription(
    registration: Registration,
    response: SubscriptionCreateOrUpdateResponseSuccess,
    internalId: Option[String]
  )(implicit
    hc: HeaderCarrier
  ): Result = {
    successSubmissionCounter.inc()
    val updatedMetadata =
      registration.metaData.copy(nrsDetails = Some(NrsDetails(response.nrsSubmissionId, response.nrsFailureReason)))
    auditor.registrationSubmitted(
      registration = registration.copy(metaData = updatedMetadata),
      pptReference = Some(response.pptReference),
      internalId = internalId
    )
    if (response.enrolmentInitiatedSuccessfully.contains(true))
      Redirect(routes.ConfirmationController.displayPage())
        .flashing(
          Flash(
            Map(FlashKeys.referenceId -> response.pptReference, FlashKeys.groupReg -> registration.isGroup.toString)
          )
        )
    else
      Redirect(routes.NotableErrorController.enrolmentFailure())
        .flashing(
          Flash(
            Map(FlashKeys.referenceId -> response.pptReference, FlashKeys.groupReg -> registration.isGroup.toString)
          )
        )
  }

  private def handleFailedSubscription(registration: Registration, failures: Seq[EisError], internalId: Option[String])(
    implicit hc: HeaderCarrier
  ): Result = {
    performFailedSubscriptionCommonTasks(registration, internalId)
    if (failures.head.isDuplicateSubscription)
      Redirect(routes.NotableErrorController.duplicateRegistration())
    else
      Redirect(routes.NotableErrorController.subscriptionFailure())
  }

  private def handleFailedSubscription(registration: Registration, internalId: Option[String])(implicit
    hc: HeaderCarrier
  ): Result = {
    performFailedSubscriptionCommonTasks(registration, internalId)
    Redirect(routes.NotableErrorController.subscriptionFailure())
  }

  private def performFailedSubscriptionCommonTasks(registration: Registration, internalId: Option[String])(implicit
    hc: HeaderCarrier
  ): Unit = {
    failedSubmissionCounter.inc()
    auditor.registrationSubmitted(registration, internalId = internalId)
  }

  private def getSafeId(registration: Registration): String =
    registration.organisationDetails.businessPartnerId.getOrElse(
      throw new IllegalStateException("Safe Id is required for a Subscription create")
    )

  private def displayReviewRegistrationPage()(implicit request: JourneyRequest[AnyContent]): Future[Result] = {

    val reg2: Registration = removePartialGroupMembersIfGroup(request.registration)

    val reg = removeGroupDetailsIfNotGroup(reg2)

    if (reg.isGroup && reg.isFirstGroupMember)
      markAsCannotYetStarted(reg).map { _ =>
        Redirect(routes.TaskListController.displayPage())
      }
    else
      markRegistrationAsReviewed(reg).map(_ => Ok(reviewRegistrationPage(reg)))
  }

  private def markRegistrationAsReviewed(
    registration: Registration
  )(implicit req: JourneyRequest[AnyContent]): Future[Either[ServiceError, Registration]] =
    update { _ =>
      val updatedMetaData =
        registration.metaData.copy(registrationReviewed = true)
      registration.copy(metaData = updatedMetaData)
    }

  private def markAsCannotYetStarted(
    registration: Registration
  )(implicit req: JourneyRequest[AnyContent]): Future[Either[ServiceError, Registration]] =
    update { _ =>
      val updatedMetaData =
        registration.metaData.copy(registrationReviewed = false, registrationCompleted = false)
      registration.copy(metaData = updatedMetaData)
    }

  private def removePartialGroupMembersIfGroup(registration: Registration): Registration =
    if (registration.isGroup)
      registrationFilterService.removePartialGroupMembers(registration)
    else registration

  private def removeGroupDetailsIfNotGroup(registration: Registration): Registration =
    if (!registration.isGroup)
      registrationFilterService.removeGroupDetails(registration)
    else registration

}
