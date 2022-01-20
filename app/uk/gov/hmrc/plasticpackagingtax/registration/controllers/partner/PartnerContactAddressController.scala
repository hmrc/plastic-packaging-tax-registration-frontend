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

import com.google.inject.{Inject, Singleton}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.RegistrationConnector
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.addresslookup.AddressLookupFrontendConnector
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.AddressLookupIntegration
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthAction
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.Address
import uk.gov.hmrc.plasticpackagingtax.registration.models.addresslookup.MissingAddressIdException
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{Cacheable, Registration}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.JourneyAction
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.ExecutionContext

@Singleton
class PartnerContactAddressController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  override val registrationConnector: RegistrationConnector,
  addressLookupFrontendConnector: AddressLookupFrontendConnector,
  appConfig: AppConfig,
  mcc: MessagesControllerComponents
)(implicit val ec: ExecutionContext)
    extends FrontendController(mcc) with AddressLookupIntegration with Cacheable with I18nSupport {

  def captureNewPartner(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      val newPartner = request.registration.inflightPartner
      initialiseAlf(routes.PartnerContactAddressController.alfCallbackNewPartner(None),
                    newPartner.map(_.name)
      )
    }

  def captureExistingPartner(partnerId: String): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      val existingPartner = request.registration.findPartner(partnerId)
      initialiseAlf(
        routes.PartnerContactAddressController.alfCallbackExistingPartner(partnerId, None),
        existingPartner.map(_.name)
      )
    }

  private def initialiseAlf(callback: Call, businessName: Option[String])(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier
  ) =
    initialiseAddressLookup(addressLookupFrontendConnector,
                            appConfig,
                            callback,
                            "addressLookup.partner",
                            businessName
    ).map(onRamp => Redirect(onRamp.redirectUrl))

  def alfCallbackNewPartner(id: Option[String]): Action[AnyContent] =
    updateRegistrationAndRedirect(id,
                                  (updatedAddress, reg) => {
                                    val updatedInflightPartner = reg.withInflightPartner(
                                      reg.inflightPartner.map(_.withContactAddress(updatedAddress))
                                    )
                                    updatedInflightPartner.withPromotedInflightPartner()
                                  },
                                  routes.PartnerCheckAnswersController.displayNominatedPartner()
    )

  def alfCallbackExistingPartner(partnerId: String, id: Option[String]): Action[AnyContent] =
    updateRegistrationAndRedirect(id,
                                  (updatedAddress, reg) =>
                                    reg.withUpdatedPartner(
                                      partnerId,
                                      partner =>
                                        partner.copy(contactDetails =
                                          partner.contactDetails.map(
                                            _.copy(address = Some(updatedAddress))
                                          )
                                        )
                                    ),
                                  routes.PartnerCheckAnswersController.displayPartner(partnerId)
    )

  private def updateRegistrationAndRedirect(
    id: Option[String],
    registrationUpdater: (Address, Registration) => Registration,
    redirect: Call
  ): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      addressLookupFrontendConnector.getAddress(
        id.getOrElse(throw new MissingAddressIdException)
      ).flatMap {
        // TODO: consider re-validating here as we do for contact address. The code there suggests that ALF might return
        //       an address which we consider invalid. Need to check this out!
        confirmedAddress =>
          update { registration =>
            registrationUpdater(Address(confirmedAddress), registration)
          }.map {
            case Right(_) =>
              Redirect(redirect)
          }
      }
    }

}
