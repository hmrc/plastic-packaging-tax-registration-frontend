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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.group

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.RegistrationConnector
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.addresslookup.AddressLookupFrontendConnector
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.AddressLookupIntegration
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthAction
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.Address
import uk.gov.hmrc.plasticpackagingtax.registration.models.addresslookup.MissingAddressIdException
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.group.GroupMemberContactDetails
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{Cacheable, GroupDetail}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.JourneyAction
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ContactDetailsConfirmAddressController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  override val registrationConnector: RegistrationConnector,
  addressLookupFrontendConnector: AddressLookupFrontendConnector,
  appConfig: AppConfig,
  mcc: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with AddressLookupIntegration with Cacheable with I18nSupport {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      initialiseAddressLookup(addressLookupFrontendConnector,
                              appConfig,
                              routes.ContactDetailsConfirmAddressController.alfCallback(None),
                              "addressLookup.member.contact",
                              request.registration.groupDetail.flatMap(_.businessName)
      ).map(onRamp => Redirect(onRamp.redirectUrl))
    }

  def alfCallback(id: Option[String]): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      addressLookupFrontendConnector.getAddress(
        id.getOrElse(throw new MissingAddressIdException)
      ).flatMap {
        // TODO: consider re-validating here as we do for contact address. The code there suggests that ALF might return
        //       an address which we consider invalid. Need to check this out!
        confirmedAddress =>
          update { registration =>
            val contactDetails: Option[GroupMemberContactDetails] =
              registration.lastMember.map(_.withGroupMemberAddress(Address(confirmedAddress)))
            val detail: GroupDetail =
              registration.groupDetail.getOrElse(throw new IllegalStateException("No group detail"))
            registration.copy(groupDetail =
              Some(
                detail.copy(members =
                  detail.updateMember(
                    registration.lastMember.map(_.copy(contactDetails = contactDetails)).get
                  )
                )
              )
            )
          }.map { _ =>
            Redirect(routes.ContactDetailsCheckAnswersController.displayPage())
          }
      }
    }

}

case class RegistrationException(message: String) extends Exception
