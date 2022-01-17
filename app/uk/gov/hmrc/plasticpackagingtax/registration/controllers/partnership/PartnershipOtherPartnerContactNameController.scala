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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.{RegistrationConnector, ServiceError}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.{
  AuthAction,
  FormAction,
  SaveAndContinue
}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.partnership.{
  routes => partnershipRoutes
}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.FullName
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.OtherPartner
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{Cacheable, Registration}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.partnerships.full_name_page
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PartnershipOtherPartnerContactNameController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  override val registrationConnector: RegistrationConnector,
  mcc: MessagesControllerComponents,
  page: full_name_page
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      {
        for {
          partnershipDetails <- request.registration.organisationDetails.partnershipDetails
          partnershipName    <- partnershipDetails.partnershipName

        } yield request.registration.inflightOtherPartner.flatMap(_.contactName) match {
          case Some(data) =>
            Ok(
              page(FullName.form().fill(FullName(data)),
                   partnershipRoutes.PartnershipPartnersListController.displayPage(),
                   partnershipRoutes.PartnershipOtherPartnerContactNameController.submit(),
                   partnershipName
              )
            )
          case None =>
            Ok(
              page(FullName.form(),
                   partnershipRoutes.PartnershipPartnersListController.displayPage(),
                   partnershipRoutes.PartnershipOtherPartnerContactNameController.submit(),
                   partnershipName
              )
            )
        }

      }.getOrElse(NotFound)
    }

  def submit(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      def partnerShipName =
        request.registration.organisationDetails.partnershipDetails.flatMap(
          _.partnershipName
        ).get // TODO unchecked get
      FullName.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[FullName]) =>
            Future.successful(
              BadRequest(
                page(formWithErrors,
                     partnershipRoutes.PartnershipPartnersListController.displayPage(),
                     partnershipRoutes.PartnershipOtherPartnerContactNameController.submit(),
                     partnerShipName
                )
              )
            ),
          fullName =>
            updateRegistration(fullName).map {
              case Right(_) =>
                FormAction.bindFromRequest match {
                  case SaveAndContinue =>
                    Redirect(routes.PartnershipOtherPartnerJobTitleController.displayPage())
                  case _ =>
                    Redirect(
                      partnershipRoutes.PartnershipOtherPartnerContactNameController.displayPage()
                    )
                }
              case Left(error) => throw error
            }
        )
    }

  private def updateRegistration(
    formData: FullName
  )(implicit req: JourneyRequest[AnyContent]): Future[Either[ServiceError, Registration]] =
    update { registration =>
      val otherPartner = registration.inflightOtherPartner.getOrElse(OtherPartner())
      registration.withInflightOtherPartner(
        Some(otherPartner.copy(contactName = Some(formData.value)))
      )
    }

}
