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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.partner

import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.{RegistrationConnector, ServiceError}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.{
  AuthAction,
  FormAction,
  SaveAndContinue
}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.partner.{routes => partnerRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.group.MemberName
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.PartnerContactDetails
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{Cacheable, Registration}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.partnerships.member_name_page
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PartnerContactNameController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  override val registrationConnector: RegistrationConnector,
  mcc: MessagesControllerComponents,
  page: member_name_page
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      request.registration.inflightPartner.map { partner =>
        val existingNameFields = for {
          contactDetails <- partner.contactDetails
          firstName      <- contactDetails.firstName
          lastName       <- contactDetails.lastName
        } yield (firstName, lastName)

        val form = existingNameFields match {
          case Some(data) =>
            MemberName.form().fill(MemberName(data._1, data._2))
          case None =>
            MemberName.form()
        }

        Ok(
          page(form,
               partner.name,
               partnerRoutes.NominatedPartnerTypeController.displayPage(),
               partnerRoutes.PartnerContactNameController.submit()
          )
        )

      }.getOrElse(throw new IllegalStateException("Expected partner missing"))
    }

  def submit(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      request.registration.inflightPartner.map { partner =>
        MemberName.form()
          .bindFromRequest()
          .fold(
            (formWithErrors: Form[MemberName]) =>
              Future.successful(
                BadRequest(
                  page(formWithErrors,
                       partner.name,
                       partnerRoutes.NominatedPartnerTypeController.displayPage(),
                       partnerRoutes.PartnerContactNameController.submit()
                  )
                )
              ),
            fullName =>
              updateRegistration(fullName).map {
                case Right(_) =>
                  FormAction.bindFromRequest match {
                    case SaveAndContinue =>
                      Redirect(routes.PartnerEmailAddressController.displayPage())
                    case _ =>
                      Redirect(partnerRoutes.PartnerContactNameController.displayPage())
                  }
                case Left(error) => throw error
              }
          )
      }.getOrElse(
        Future.successful(throw new IllegalStateException("Expected inflight partner missing"))
      )
    }

  private def updateRegistration(
    formData: MemberName
  )(implicit req: JourneyRequest[AnyContent]): Future[Either[ServiceError, Registration]] =
    update { registration =>
      registration.inflightPartner.map { partner =>
        val withContactName = partner.copy(contactDetails =
          Some(
            PartnerContactDetails(firstName = Some(formData.firstName),
                                  lastName = Some(formData.lastName)
            )
          )
        )
        registration.withInflightPartner(Some(withContactName))
      }.getOrElse {
        registration
      }
    }

}
