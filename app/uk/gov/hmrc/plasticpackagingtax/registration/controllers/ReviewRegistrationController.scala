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

import com.kenshoo.play.metrics.Metrics
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.plasticpackagingtax.registration.audit.Auditor
import uk.gov.hmrc.plasticpackagingtax.registration.connectors._
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthAction
import uk.gov.hmrc.plasticpackagingtax.registration.models.nrs.NrsDetails
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{Cacheable, Registration}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.models.response.FlashKeys
import uk.gov.hmrc.plasticpackagingtax.registration.models.subscriptions.{
  EisError,
  SubscriptionCreateOrUpdateResponseFailure,
  SubscriptionCreateOrUpdateResponseSuccess
}
import uk.gov.hmrc.plasticpackagingtax.registration.services.RegistrationFilterService
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.review_registration_page
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
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
  startRegistrationController: StartRegistrationController,
  reviewRegistrationPage: review_registration_page,
  registrationFilterService: RegistrationFilterService
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport {

  private val successSubmissionCounter =
    metrics.defaultRegistry.counter("ppt.registration.success.submission.counter")

  private val failedSubmissionCounter =
    metrics.defaultRegistry.counter("ppt.registration.failed.submission.counter")

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      if (request.registration.isCheckAndSubmitReady) {
        val reg: Registration = removePartialGroupMembers(request.registration)

        markRegistrationAsReviewed(reg).map(
          _ =>
            if (reg.isFirstGroupMember)
              Redirect(routes.TaskListController.displayPage())
            else
              Ok(
                reviewRegistrationPage(registration = reg,
                                       liabilityStartLink = startRegistrationController.startLink
                )
              )
        )
      } else
        Future.successful(Redirect(routes.TaskListController.displayPage()))
    }

  private def markRegistrationAsReviewed(
    registration: Registration
  )(implicit req: JourneyRequest[AnyContent]): Future[Either[ServiceError, Registration]] =
    update { _ =>
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
          case successfulSubscription @ SubscriptionCreateOrUpdateResponseSuccess(_,
                                                                                  _,
                                                                                  _,
                                                                                  _,
                                                                                  _,
                                                                                  _,
                                                                                  _
              ) =>
            handleSuccessfulSubscription(completedRegistration, successfulSubscription)
          case SubscriptionCreateOrUpdateResponseFailure(failures) =>
            handleFailedSubscription(completedRegistration, failures)
        }
        .recoverWith {
          case _ => Future.successful(handleFailedSubscription(completedRegistration))
        }
    }

  private def handleSuccessfulSubscription(
    registration: Registration,
    response: SubscriptionCreateOrUpdateResponseSuccess
  )(implicit hc: HeaderCarrier): Result = {
    successSubmissionCounter.inc()
    val updatedMetadata = registration.metaData.copy(nrsDetails =
      Some(NrsDetails(response.nrsSubmissionId, response.nrsFailureReason))
    )
    auditor.registrationSubmitted(registration.copy(metaData = updatedMetadata),
                                  Some(response.pptReference)
    )
    if (response.enrolmentInitiatedSuccessfully.contains(true))
      Redirect(routes.ConfirmationController.displayPage())
        .flashing(Flash(Map(FlashKeys.referenceId -> response.pptReference)))
    else
      Redirect(routes.NotableErrorController.enrolmentFailure())
        .flashing(Flash(Map(FlashKeys.referenceId -> response.pptReference)))
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
    registration.organisationDetails.businessPartnerId.getOrElse(
      throw new IllegalStateException("Safe Id is required for a Subscription create")
    )

  private def removePartialGroupMembers(registration: Registration) =
    if (registration.isGroup)
      registrationFilterService.filterByGroupMembers(registration)
    else registration

}
