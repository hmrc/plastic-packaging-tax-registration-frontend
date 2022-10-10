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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Results}
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.RegistrationConnector
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.NotEnrolledAuthAction
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.JourneyAction
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.UpdateCompaniesHouseView
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import javax.inject.Inject
import scala.concurrent.ExecutionContext

/*
  Advice for users that are encountering problem where address
  from companies house does not meet the requirements of subscription create.
  Solution is to advise the user to fix companies house data, and try again.
  This also means purging the cache of any address previously bought in
  from GRS as it is now out of date.
 */

class UpdateCompaniesHouseController @Inject()(
  authenticate: NotEnrolledAuthAction,
  journeyAction: JourneyAction,
  registrationConnector: RegistrationConnector,
  updateCompaniesHouse: UpdateCompaniesHouseView,
  val messagesApi: MessagesApi
)(implicit ec: ExecutionContext) extends Results with FrontendHeaderCarrierProvider with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (authenticate andThen journeyAction) {
    implicit request =>
      {for {
        crn <- request.registration.organisationDetails.incorporationDetails.map(_.companyNumber)
        cn <- request.registration.organisationDetails.incorporationDetails.map(_.companyName)
      } yield
        Ok(updateCompaniesHouse(crn, cn))
      }.get
  }

  def reset(): Action[AnyContent] = (authenticate andThen journeyAction).async {
    implicit request =>
      registrationConnector.update(request.registration.clearAddressFromGrs)
        .map{
          case Left(e) => throw e
          case Right(_) => Redirect(routes.TaskListController.displayPage())
        }
  }
}
