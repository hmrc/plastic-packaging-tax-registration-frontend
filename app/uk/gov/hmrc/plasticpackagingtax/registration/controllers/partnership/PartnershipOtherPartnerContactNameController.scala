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
import uk.gov.hmrc.plasticpackagingtax.registration.forms.group.MemberName
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.PartnerTypeEnum
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.{
  Partner,
  PartnerContactDetails
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{Cacheable, Registration}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.partnerships.member_name_page
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PartnershipOtherPartnerContactNameController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  override val registrationConnector: RegistrationConnector,
  mcc: MessagesControllerComponents,
  page: member_name_page
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      val inflightOtherPartner =
        Some(
          Partner(organisationName = Some("Partner organisation"),
                  partnerType = Some(PartnerTypeEnum.SOLE_TRADER)
          ) // TODO will have been populated by the preceding GRS journey
        )

      (for {
        inflight         <- inflightOtherPartner
        organisationName <- inflight.organisationName
      } yield (for {
        contactDetails <- inflight.contactDetails
        firstName      <- contactDetails.firstName
        lastName       <- contactDetails.lastName

      } yield (firstName, lastName)) match {
        case Some(data) =>
          Ok(
            page(MemberName.form().fill(MemberName(data._1, data._2)),
                 organisationName,
                 partnershipRoutes.PartnershipPartnersListController.displayPage(),
                 partnershipRoutes.PartnershipOtherPartnerContactNameController.submit()
            )
          )
        case None =>
          Ok(
            page(MemberName.form(),
                 organisationName,
                 partnershipRoutes.PartnershipPartnersListController.displayPage(),
                 partnershipRoutes.PartnershipOtherPartnerContactNameController.submit()
            )
          )
      }).getOrElse(NotFound)
    }

  def submit(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      val inflightOtherPartner =
        Some(
          Partner(organisationName = Some("Partner organisation"),
                  partnerType = Some(PartnerTypeEnum.SOLE_TRADER)
          )
        ) // TODO will have been populated by the preceding GRS journey

      (for {
        inflight         <- inflightOtherPartner
        organisationName <- inflight.organisationName
      } yield MemberName.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[MemberName]) =>
            Future.successful(
              BadRequest(
                page(formWithErrors,
                     organisationName,
                     partnershipRoutes.PartnershipPartnersListController.displayPage(),
                     partnershipRoutes.PartnershipOtherPartnerContactNameController.submit()
                )
              )
            ),
          fullName =>
            updateRegistration(fullName).map {
              case Right(_) =>
                FormAction.bindFromRequest match {
                  case SaveAndContinue =>
                    Redirect(routes.PartnershipOtherPartnerEmailAddressController.displayPage())
                  case _ =>
                    Redirect(
                      partnershipRoutes.PartnershipOtherPartnerContactNameController.displayPage()
                    )
                }
              case Left(error) => throw error
            }
        )).getOrElse(Future.successful(NotFound))
    }

  private def updateRegistration(
    formData: MemberName
  )(implicit req: JourneyRequest[AnyContent]): Future[Either[ServiceError, Registration]] =
    update { registration =>
      val otherPartner = registration.inflightPartner.getOrElse(
        Partner(partnerType = Some(PartnerTypeEnum.SOLE_TRADER))
      ) // TODO type from previous GRS screen

      val withContactName = otherPartner.copy(contactDetails =
        Some(
          PartnerContactDetails(firstName = Some(formData.firstName),
                                lastName = Some(formData.lastName)
          )
        )
      )

      registration.withInflightPartner(Some(withContactName))
    }

}
