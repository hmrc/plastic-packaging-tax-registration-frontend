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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.testOnly

import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.testOnly.EmailTestOnlyPasscodeConnector
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthAction
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.JourneyAction
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmailPasscodeController @Inject() (
  authenticate: AuthAction,
  mcc: MessagesControllerComponents,
  journeyAction: JourneyAction,
  emailTestOnlyPasscodeConnector: EmailTestOnlyPasscodeConnector
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport {

  def testOnlyGetPasscodes(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      emailTestOnlyPasscodeConnector.getTestOnlyPasscode.flatMap {
        case Right(response) => Future.successful(Ok(response))
        case Left(error)     => throw error
      }
    }

}
