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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.partnership

import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.{RegistrationConnector, ServiceError}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.{
  AuthAction,
  FormAction,
  SaveAndContinue
}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.partnership.{
  routes => partnershipRoutes
}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.{routes => commonRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.JobTitle
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{Cacheable, Registration}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.partnerships.job_title_page
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PartnershipOtherPartnerJobTitleController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  override val registrationConnector: RegistrationConnector,
  mcc: MessagesControllerComponents,
  page: job_title_page
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      (for {
        inflight    <- request.registration.inflightOtherPartner
        contactName <- inflight.contactName

      } yield inflight.jobTitle match {
        case Some(data) =>
          Ok(
            page(JobTitle.form().fill(JobTitle(data)),
                 partnershipRoutes.PartnershipOtherPartnerContactNameController.displayPage(),
                 partnershipRoutes.PartnershipOtherPartnerJobTitleController.submit(),
                 contactName
            )
          )
        case _ =>
          Ok(
            page(JobTitle.form(),
                 partnershipRoutes.PartnershipOtherPartnerContactNameController.displayPage(),
                 partnershipRoutes.PartnershipOtherPartnerJobTitleController.submit(),
                 contactName
            )
          )
      }).getOrElse(NotFound)
    }

  def submit(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      JobTitle.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[JobTitle]) =>
            (for {
              inflightOtherParter <- request.registration.inflightOtherPartner
              contactName         <- inflightOtherParter.contactName
            } yield Future.successful(
              BadRequest(
                page(formWithErrors,
                     partnershipRoutes.PartnershipOtherPartnerContactNameController.displayPage(),
                     partnershipRoutes.PartnershipOtherPartnerJobTitleController.submit(),
                     contactName
                )
              )
            )).getOrElse(Future.successful(NotFound)),
          jobTitle =>
            updateRegistration(jobTitle).map {
              case Right(_) =>
                FormAction.bindFromRequest match {
                  case SaveAndContinue =>
                    Redirect(
                      partnershipRoutes.PartnershipOtherPartnerEmailAddressController.displayPage()
                    )
                  case _ =>
                    Redirect(commonRoutes.TaskListController.displayPage())
                }
              case Left(error) => throw error
            }
        )
    }

  private def updateRegistration(
    formData: JobTitle
  )(implicit req: JourneyRequest[AnyContent]): Future[Either[ServiceError, Registration]] =
    update { registration =>
      (for {
        inflight <- registration.inflightOtherPartner
      } yield inflight.copy(jobTitle = Some(formData.value))).map { updated =>
        registration.withInflightOtherPartner(Some(updated))
      }.getOrElse {
        registration
      }
    }

}
