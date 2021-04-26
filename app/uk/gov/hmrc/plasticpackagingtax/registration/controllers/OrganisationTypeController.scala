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
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.IncorpSoleTraderConnector
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthAction
import uk.gov.hmrc.plasticpackagingtax.registration.forms.OrganisationType
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.{IncorpIdCreateRequest, SoleTraderCreateRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.OrgType
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.JourneyAction
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.organisation_type
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OrganisationTypeController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  mcc: MessagesControllerComponents,
  page: organisation_type,
  incorpIdConnector: IncorpSoleTraderConnector
)(implicit appConfig: AppConfig, val executionContext: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      request.registration.orgType match {
        case Some(data) => Ok(page(OrganisationType.form().fill(OrganisationType(data.toString))))
        case _          => Ok(page(OrganisationType.form()))
      }
    }

  def submit(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      OrganisationType.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[OrganisationType]) =>
            Future.successful(BadRequest(page(formWithErrors))),
          orgType =>
            orgType match {
              case OrganisationType(OrgType.UK_LIMITED) =>
                Future(Redirect(routes.HonestyDeclarationController.displayPage()))
              case OrganisationType(OrgType.SOLE_TRADER) =>
                incorpIdConnector.createJourney(
                  SoleTraderCreateRequest(appConfig.incorpIdJourneyCallbackUrl,
                                        Some(request2Messages(request)("service.name")),
                                        appConfig.serviceIdentifier,
                                        appConfig.exitSurveyUrl
                  )
                ).map(journeyStartUrl => SeeOther(journeyStartUrl).addingToSession())
            }
        )
    }

}
