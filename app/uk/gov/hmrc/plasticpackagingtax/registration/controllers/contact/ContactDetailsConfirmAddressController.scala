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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.contact

import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.{RegistrationConnector, ServiceError}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.{
  NotEnrolledAuthAction,
  FormAction,
  SaveAndContinue
}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.{routes => commonRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.{Address, ConfirmAddress}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{Cacheable, Registration}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.contact.confirm_address
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ContactDetailsConfirmAddressController @Inject() (
                                                         authenticate: NotEnrolledAuthAction,
                                                         journeyAction: JourneyAction,
                                                         override val registrationConnector: RegistrationConnector,
                                                         mcc: MessagesControllerComponents,
                                                         page: confirm_address
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      val businessRegisteredAddress = request.registration.organisationDetails.businessRegisteredAddress.getOrElse(
        throw new IllegalStateException("Registered business address must be present")
      )

      if (request.registration.organisationDetails.incorporationDetails.isDefined && businessRegisteredAddress.isMissingCountryCode)
        Redirect(commonRoutes.UpdateCompaniesHouseController.onPageLoad())
      else
        Ok(
          page(
            ConfirmAddress.form().fill(
              ConfirmAddress(request.registration.primaryContactDetails.useRegisteredAddress)
            ),
            businessRegisteredAddress,
            request.registration.isGroup
          )
        )
    }

  def submit(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      request.registration.incorpJourneyId match {
        case Some(_) =>
          ConfirmAddress.form()
            .bindFromRequest()
            .fold(
              (formWithErrors: Form[ConfirmAddress]) =>
                Future(
                  BadRequest(
                    page(formWithErrors,
                         request.registration.organisationDetails.businessRegisteredAddress.get,
                      request.registration.isGroup
                    )
                  )
                ),
              confirmAddress =>
                updateRegistration(
                  confirmAddress,
                  request.registration.organisationDetails.businessRegisteredAddress
                ).map {
                  case Right(_) =>
                    FormAction.bindFromRequest match {
                      case SaveAndContinue =>
                        if (confirmAddress.useRegisteredAddress.getOrElse(false))
                          Redirect(routes.ContactDetailsCheckAnswersController.displayPage())
                        else Redirect(routes.ContactDetailsAddressController.displayPage())
                      case _ => Redirect(commonRoutes.TaskListController.displayPage())
                    }
                  case Left(error) => throw error
                }
            )

        case _ => Future(Redirect(commonRoutes.TaskListController.displayPage()))
      }
    }

  private def updateRegistration(formData: ConfirmAddress, businessAddress: Option[Address])(
    implicit req: JourneyRequest[AnyContent]
  ): Future[Either[ServiceError, Registration]] =
    update { registration =>
      val updatedPrimaryContactDetails = {
        if (formData.useRegisteredAddress.getOrElse(false))
          registration.primaryContactDetails.copy(useRegisteredAddress =
                                                    formData.useRegisteredAddress,
                                                  address = businessAddress
          )
        else
          registration.primaryContactDetails.copy(useRegisteredAddress =
                                                    formData.useRegisteredAddress,
                                                  address =
                                                    registration.primaryContactDetails.address
          )
      }
      val updatedOrgDetails =
        registration.organisationDetails.copy(businessRegisteredAddress = businessAddress)
      registration.copy(primaryContactDetails = updatedPrimaryContactDetails,
                        organisationDetails = updatedOrgDetails
      )
    }

}
