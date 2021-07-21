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
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, Flash, MessagesControllerComponents}
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.plasticpackagingtax.registration.audit.Auditor
import uk.gov.hmrc.plasticpackagingtax.registration.connectors._
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthAction
import uk.gov.hmrc.plasticpackagingtax.registration.forms.OrgType
import uk.gov.hmrc.plasticpackagingtax.registration.forms.OrgType.{SOLE_TRADER, UK_COMPANY}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{Cacheable, Registration}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.models.response.FlashKeys
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
  page: review_registration_page
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport {

  private val successSubmissionCounter =
    metrics.defaultRegistry.counter("ppt.registration.success.submission.counter")

  private val failedSubmissionCounter =
    metrics.defaultRegistry.counter("ppt.registration.failed.submission.counter")

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      if (request.registration.isCheckAndSubmitReady)
        request.registration.organisationDetails.organisationType match {
          case Some(UK_COMPANY)  => ukCompanyReview()
          case Some(SOLE_TRADER) => soleTraderReview()
          case _ =>
            throw new InternalServerException(s"Company type not supported")
        }
      else
        Future(Redirect(routes.RegistrationController.displayPage()))
    }

  private def soleTraderReview()(implicit request: JourneyRequest[AnyContent]) =
    for {
      soleTraderDetails <- request.registration.organisationDetails.soleTraderDetails.fold(
        throw new Exception("Unable to fetch sole trader details from cache")
      )(details => Future.successful(details))
      _ <- markRegistrationAsReviewed()
    } yield Ok(
      page(registration = request.registration, soleTraderDetails = Some(soleTraderDetails))
    )

  private def ukCompanyReview()(implicit request: JourneyRequest[AnyContent]) =
    for {
      incorporationDetails <- request.registration.organisationDetails.incorporationDetails.fold(
        throw new Exception("Unable to fetch incorporation details from cache")
      )(details => Future.successful(details))
      _ <- markRegistrationAsReviewed()
    } yield Ok(
      page(registration = request.registration, incorporationDetails = Some(incorporationDetails))
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
      markRegistrationAsCompleted().flatMap {
        case Right(updatedRegistration) =>
          val updatedRegistrationWithUserHeaders =
            updatedRegistration.copy(userHeaders = Some(request.headers.toSimpleMap))
          subscriptionsConnector.submitSubscription(getSafeId(updatedRegistrationWithUserHeaders),
                                                    updatedRegistrationWithUserHeaders
          ).map { response =>
            successSubmissionCounter.inc()
            auditor.registrationSubmitted(updatedRegistration)
            Redirect(routes.ConfirmationController.displayPage())
              .flashing(Flash(Map(FlashKeys.referenceId -> response.pptReference)))
          }
        case Left(error) =>
          auditor.registrationSubmitted(request.registration)
          failedSubmissionCounter.inc()
          throw error
      }
    }

  private def markRegistrationAsCompleted()(implicit
    req: JourneyRequest[AnyContent]
  ): Future[Either[ServiceError, Registration]] =
    update { registration =>
      val updatedMetaData =
        registration.metaData.copy(registrationCompleted = true)
      registration.copy(metaData = updatedMetaData)
    }

  private def getSafeId(registration: Registration): String =
    registration.organisationDetails.organisationType.flatMap {
      case OrgType.SOLE_TRADER =>
        registration.organisationDetails.soleTraderDetails.flatMap(
          details => details.registration.registeredBusinessPartnerId
        )
      case _ =>
        registration.organisationDetails.incorporationDetails.flatMap(
          details => details.registration.registeredBusinessPartnerId
        )
    }.getOrElse(throw new Exception("Safe Id is required for a Subscription create"))

}
