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
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.{
  IncorpIdConnector,
  PartnershipConnector,
  RegistrationConnector,
  ServiceError
}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.{
  AuthAction,
  FormAction,
  SaveAndContinue
}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.ConfirmAddress.form
import uk.gov.hmrc.plasticpackagingtax.registration.forms.OrgType.{
  PARTNERSHIP,
  SOLE_TRADER,
  UK_COMPANY
}
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
  partnershipConnector: PartnershipConnector,
  mcc: MessagesControllerComponents,
  page: confirm_address
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      request.registration.incorpJourneyId match {
        case Some(journeyId) =>
          request.registration.organisationDetails.organisationType match {
            case Some(UK_COMPANY) =>
              executeAddressUpdate(
                journeyId,
                journeyId =>
                  incorpIdConnector.getDetails(journeyId)
                    .flatMap(
                      response =>
                        updateBusinessAddress(response.companyAddress.toPptAddress)(request)
                    )
              )
            case Some(SOLE_TRADER) =>
              executeAddressUpdate(journeyId,
                                   _ =>
                                     updateBusinessAddress(
                                       //TODO - temporary while we're working out where to get it from
                                       Address(addressLine1 = "2 Scala Street",
                                               addressLine2 = Some("Soho"),
                                               townOrCity = "London",
                                               postCode = "W1T 2HN"
                                       )
                                     )
              )
            case Some(PARTNERSHIP) =>
              executeAddressUpdate(journeyId,
                                   journeyId =>
                                     partnershipConnector.getDetails(journeyId)
                                       .flatMap(
                                         response =>
                                           updateBusinessAddress(
                                             // TODO - how do we get/lookup the rest of the address?
                                             Address(addressLine1 = "3 Scala Street",
                                                     addressLine2 = Some("Soho"),
                                                     townOrCity = "London",
                                                     postCode = response.postcode
                                             )
                                           )(request)
                                       )
              )
            case _ => throw new InternalServerException(s"Company type not supported")
          }

        case _ => Future(Redirect(routes.RegistrationController.displayPage()))
      }
    }

  private def executeAddressUpdate(
    journeyId: String,
    getAddressFunction: String => Future[Address]
  )(implicit request: JourneyRequest[AnyContent]) =
    for {
      updatedAddress <- getAddressFunction(journeyId)
    } yield Ok(
      page(form().fill(
             ConfirmAddress(request.registration.primaryContactDetails.useRegisteredAddress)
           ),
           updatedAddress
      )
    )

  private def updateBusinessAddress(
    address: Address
  )(implicit req: JourneyRequest[AnyContent]): Future[Address] =
    update { registration =>
      val updatedOrgDetails =
        registration.organisationDetails.copy(businessRegisteredAddress = Some(address))
      registration.copy(organisationDetails = updatedOrgDetails)
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
                    page(formWithErrors,
                         request.registration.organisationDetails.businessRegisteredAddress.get
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
      val updatedOrgDetails =
        registration.organisationDetails.copy(businessRegisteredAddress = businessAddress)
      registration.copy(primaryContactDetails = updatedPrimaryContactDetails,
                        organisationDetails = updatedOrgDetails
      )
    }

}
