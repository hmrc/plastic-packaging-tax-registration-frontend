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

import javax.inject.{Inject, Singleton}
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
import uk.gov.hmrc.plasticpackagingtax.registration.forms.ConfirmAddress
import uk.gov.hmrc.plasticpackagingtax.registration.forms.ConfirmAddress.form
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.IncorporationAddressDetails
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{Cacheable, Registration}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.confirm_address
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

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
          } yield Ok(
            page(form().fill(
                   ConfirmAddress(request.registration.primaryContactDetails.useRegisteredAddress)
                 ),
                 incorporationDetails.companyAddress
            )
          )
        case _ => Future(Redirect(routes.RegistrationController.displayPage()))
      }
    }

  def submit(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      request.registration.incorpJourneyId match {
        case Some(journeyId) =>
          incorpIdConnector.getDetails(journeyId).flatMap { incorporationDetails =>
            form()
              .bindFromRequest()
              .fold(
                (formWithErrors: Form[ConfirmAddress]) =>
                  Future(BadRequest(page(formWithErrors, incorporationDetails.companyAddress))),
                confirmAddress =>
                  updateRegistration(confirmAddress, incorporationDetails.companyAddress).map {
                    case Right(_) =>
                      FormAction.bindFromRequest match {
                        case SaveAndContinue =>
                          if (confirmAddress.useRegisteredAddress)
                            Redirect(routes.ContactDetailsCheckAnswersController.displayPage())
                          else Redirect(routes.ContactDetailsAddressController.displayPage())
                        case _ => Redirect(routes.RegistrationController.displayPage())
                      }
                    case Left(error) => throw error
                  }
              )
          }
        case _ => Future(Redirect(routes.RegistrationController.displayPage()))
      }
    }

  private def updateRegistration(
    formData: ConfirmAddress,
    incorporationAddressDetails: IncorporationAddressDetails
  )(implicit req: JourneyRequest[AnyContent]): Future[Either[ServiceError, Registration]] =
    update { registration =>
      val updatedPrimaryContactDetails = {
        if (formData.useRegisteredAddress)
          registration.primaryContactDetails.copy(useRegisteredAddress =
                                                    formData.useRegisteredAddress,
                                                  address =
                                                    Some(incorporationAddressDetails.toPptAddress)
          )
        else
          registration.primaryContactDetails.copy(useRegisteredAddress =
            formData.useRegisteredAddress
          )
      }
      registration.copy(primaryContactDetails = updatedPrimaryContactDetails)
    }

}
