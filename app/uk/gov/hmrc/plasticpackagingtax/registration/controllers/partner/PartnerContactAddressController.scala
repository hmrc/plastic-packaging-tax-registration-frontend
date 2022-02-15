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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.addresslookup.AddressLookupFrontendConnector
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthAction
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.NewRegistrationUpdateService
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.JourneyAction

import scala.concurrent.ExecutionContext

@Singleton
class PartnerContactAddressController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  addressLookupFrontendConnector: AddressLookupFrontendConnector,
  appConfig: AppConfig,
  mcc: MessagesControllerComponents,
  registrationUpdater: NewRegistrationUpdateService
)(implicit val ec: ExecutionContext)
    extends PartnerContactAddressControllerBase(authenticate,
                                                journeyAction,
                                                addressLookupFrontendConnector,
                                                mcc,
                                                appConfig,
                                                registrationUpdater
    ) {

  def captureNewPartner(): Action[AnyContent] =
    doDisplayPage(None, routes.PartnerContactAddressController.alfCallbackNewPartner(None))

  def captureExistingPartner(partnerId: String): Action[AnyContent] =
    doDisplayPage(Some(partnerId),
                  routes.PartnerContactAddressController.alfCallbackExistingPartner(partnerId, None)
    )

  def alfCallbackNewPartner(id: Option[String]): Action[AnyContent] =
    doAlfCallback(None, id, routes.PartnerCheckAnswersController.displayNewPartner())

  def alfCallbackExistingPartner(partnerId: String, id: Option[String]): Action[AnyContent] =
    doAlfCallback(Some(partnerId),
                  id,
                  routes.PartnerCheckAnswersController.displayExistingPartner(partnerId)
    )

}
