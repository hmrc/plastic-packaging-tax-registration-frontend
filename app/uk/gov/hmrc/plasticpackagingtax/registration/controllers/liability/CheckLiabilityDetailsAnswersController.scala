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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.liability

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.{RegistrationConnector, ServiceError}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.NotEnrolledAuthAction
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.{routes => commonRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.OldDate
import uk.gov.hmrc.plasticpackagingtax.registration.forms.liability.RegType
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{Cacheable, Registration}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.services.TaxStartDateService
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.liability.check_liability_details_answers_page

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CheckLiabilityDetailsAnswersController @Inject() (authenticate: NotEnrolledAuthAction,
                                                        journeyAction: JourneyAction,
                                                        mcc: MessagesControllerComponents,
                                                        override val registrationConnector: RegistrationConnector,
                                                        taxStarDateService: TaxStartDateService,
                                                        page: check_liability_details_answers_page)
                                                       (implicit ec: ExecutionContext) extends LiabilityController(mcc)
  with Cacheable with I18nSupport {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      Ok(page(request.registration, backLink))
    }

  def submit(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>

      val taxStartDate = taxStarDateService.calculateTaxStartDate2(request.registration.liabilityDetails)
      taxStartDate.oldDate match {
        case Some(date) =>
          updateRegistration(date).map { _ =>
            Redirect(commonRoutes.TaskListController.displayPage())
          }
        case _ => throw new IllegalStateException("Problem calculating the Tax Start Date")
      }
    }

  private def backLink()(implicit request: JourneyRequest[AnyContent]): Call =
    request.registration.registrationType match {
      case Some(regType) if regType == RegType.GROUP =>
        routes.MembersUnderGroupControlController.displayPage()
      case _ => routes.RegistrationTypeController.displayPage()
    }

  private def updateRegistration(startDate: LocalDate)
                                (implicit req: JourneyRequest[AnyContent]): Future[Either[ServiceError, Registration]] =
    update { registration =>
      val updatedLiabilityDetails =
        registration.liabilityDetails.copy(startDate =
          Some(
            OldDate(Some(startDate.getDayOfMonth),
              Some(startDate.getMonth.getValue),
              Some(startDate.getYear)
            )
          )
        )
      registration.copy(liabilityDetails = updatedLiabilityDetails)
    }
}
