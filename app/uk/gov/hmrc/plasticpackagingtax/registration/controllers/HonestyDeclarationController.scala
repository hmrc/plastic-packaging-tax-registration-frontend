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
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.IncorpIdConnector
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthAction
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.IncorpIdCreateRequest
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.honesty_declaration
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.ExecutionContext

@Singleton
class HonestyDeclarationController @Inject() (
  authenticate: AuthAction,
  mcc: MessagesControllerComponents,
  honesty_declaration: honesty_declaration,
  incorpIdConnector: IncorpIdConnector
)(implicit appConfig: AppConfig, val executionContext: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport {

  def displayPage(): Action[AnyContent] =
    authenticate { implicit request =>
      Ok(honesty_declaration())
    }

  def submit(): Action[AnyContent] =
    authenticate.async { implicit request =>
      incorpIdConnector.createJourney(
        IncorpIdCreateRequest(appConfig.incorpIdJourneyCallbackUrl,
                              Some(request2Messages(request)("service.name")),
                              appConfig.serviceIdentifier,
                              appConfig.exitSurveyUrl
        )
      ).map(journeyStartUrl => SeeOther(journeyStartUrl).addingToSession())
    }

}
