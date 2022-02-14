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

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.grs.{
  PartnershipGrsConnector,
  RegisteredSocietyGrsConnector,
  SoleTraderGrsConnector,
  UkCompanyGrsConnector
}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthAction
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.NewRegistrationUpdateService
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.JourneyAction
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.organisation.partner_type

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class PartnerTypeController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  registrationUpdater: NewRegistrationUpdateService,
  mcc: MessagesControllerComponents,
  page: partner_type,
  val appConfig: AppConfig,
  val soleTraderGrsConnector: SoleTraderGrsConnector,
  val ukCompanyGrsConnector: UkCompanyGrsConnector,
  val registeredSocietyGrsConnector: RegisteredSocietyGrsConnector,
  val partnershipGrsConnector: PartnershipGrsConnector
)(implicit ec: ExecutionContext)
    extends PartnerTypeControllerBase(authenticate,
                                      journeyAction = journeyAction,
                                      mcc,
                                      page,
                                      registrationUpdater
    ) {

  def displayNewPartner(): Action[AnyContent] =
    doDisplayPage(submitCall = routes.PartnerTypeController.submitNewPartner())

  def displayExistingPartner(partnerId: String): Action[AnyContent] =
    doDisplayPage(partnerId = Some(partnerId),
                  submitCall = routes.PartnerTypeController.submitExistingPartner(partnerId)
    )

  def submitNewPartner(): Action[AnyContent] =
    doSubmit(submitCall = routes.PartnerTypeController.submitNewPartner())

  def submitExistingPartner(partnerId: String): Action[AnyContent] =
    doSubmit(Some(partnerId),
             submitCall = routes.PartnerTypeController.submitExistingPartner(partnerId)
    )

  override def grsCallbackUrl(partnerId: Option[String]): String =
    appConfig.partnerGrsCallbackUrl(partnerId)

}
