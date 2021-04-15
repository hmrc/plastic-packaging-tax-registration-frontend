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

import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.{
  IncorpIdConnector,
  RegistrationConnector,
  ServiceError
}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.{
  AuthAction,
  FormAction,
  SaveAndContinue
}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.ConfirmAddress.form
import uk.gov.hmrc.plasticpackagingtax.registration.forms.{Address, ConfirmAddress}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{Cacheable, Registration}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.confirm_address
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ContactDetailsConfirmAddressController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  override val registrationConnector: RegistrationConnector,
  incorpIdConnector: IncorpIdConnector,
  mcc: MessagesControllerComponents,
  page: confirm_address
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      request.registration.incorpJourneyId match {
        case Some(journeyId) =>
          for {
            incorporationDetails <- incorpIdConnector.getDetails(journeyId)
            updatedAddress <- updateBusinessAddress(
              incorporationDetails.companyAddress.toPptAddress
            )
          } yield Ok(
            page(form().fill(
                   ConfirmAddress(request.registration.primaryContactDetails.useRegisteredAddress)
                 ),
                 updatedAddress
            )
          )
        case _ => Future(Redirect(routes.RegistrationController.displayPage()))
      }
    }

  private def updateBusinessAddress(
    address: Address
  )(implicit req: JourneyRequest[AnyContent]): Future[Address] =
    update { registration =>
      registration.copy(businessRegisteredAddress = Some(address))
    }.map {
      case Right(_)    => address
      case Left(error) => throw error
    }

  def submit(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      request.registration.incorpJourneyId match {
        case Some(_) =>
          form()
            .bindFromRequest()
            .fold(
              (formWithErrors: Form[ConfirmAddress]) =>
                Future(
                  BadRequest(
                    page(formWithErrors, request.registration.businessRegisteredAddress.get)
                  )
                ),
              confirmAddress =>
                updateRegistration(confirmAddress,
                                   request.registration.businessRegisteredAddress
                ).map {
                  case Right(_) =>
                    FormAction.bindFromRequest match {
                      case SaveAndContinue =>
                        if (confirmAddress.useRegisteredAddress.getOrElse(false))
                          Redirect(routes.ContactDetailsCheckAnswersController.displayPage())
                        else Redirect(routes.ContactDetailsAddressController.displayPage())
                      case _ => Redirect(routes.RegistrationController.displayPage())
                    }
                  case Left(error) => throw error
                }
            )

        case _ => Future(Redirect(routes.RegistrationController.displayPage()))
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
                                                  address = None
          )
      }
      registration.copy(primaryContactDetails = updatedPrimaryContactDetails,
                        businessRegisteredAddress = businessAddress
      )
    }

}
