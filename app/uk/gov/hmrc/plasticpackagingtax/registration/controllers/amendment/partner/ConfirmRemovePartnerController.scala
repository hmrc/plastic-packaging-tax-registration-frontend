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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.amendment.partner

import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.EnrolledAuthAction
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.amendment.AmendmentController
import uk.gov.hmrc.plasticpackagingtax.registration.forms.partner.RemovePartner
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Registration
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{
  AmendmentJourneyAction,
  JourneyRequest
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.amendment.partner.confirm_remove_partner_page

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ConfirmRemovePartnerController @Inject() (
                                                 authenticate: EnrolledAuthAction,
                                                 amendmentJourneyAction: AmendmentJourneyAction,
                                                 mcc: MessagesControllerComponents,
                                                 page: confirm_remove_partner_page
)(implicit ec: ExecutionContext)
    extends AmendmentController(mcc, amendmentJourneyAction) with I18nSupport {

  def displayPage(partnerId: String): Action[AnyContent] =
    (authenticate andThen amendmentJourneyAction) { implicit request =>
      Ok(page(RemovePartner.form(), getPartner(partnerId, request.registration)))
    }

  private def getPartner(partnerId: String, registration: Registration) =
    registration.findPartner(partnerId).getOrElse(
      throw new IllegalStateException("Specified partner not found")
    )

  def submit(partnerId: String): Action[AnyContent] =
    (authenticate andThen amendmentJourneyAction).async { implicit request =>
      request.registration.findPartner(partnerId).map { partner =>
        RemovePartner.form().bindFromRequest().fold(
          { formWithErrors: Form[RemovePartner] =>
            Future.successful(BadRequest(page(formWithErrors, partner)))
          },
          { removePartner: RemovePartner =>
            removePartner.value match {
              case Some(true) =>
                doRemovePartner(partner.id)
              case _ =>
                Future.successful(Redirect(routes.PartnersListController.displayPage()))
            }
          }
        )
      }.getOrElse {
        throw new IllegalStateException("Could not find partner")
      }
    }

  private def doRemovePartner(
    partnerId: String
  )(implicit req: JourneyRequest[AnyContent]): Future[Result] =
    updateRegistration(
      registration =>
        registration.copy(organisationDetails =
          registration.organisationDetails.copy(partnershipDetails =
            registration.organisationDetails.partnershipDetails.map(
              _.copy(partners =
                registration.organisationDetails.partnershipDetails.map(
                  _.partners.filter(_.id != partnerId)
                ).getOrElse(Seq())
              )
            )
          )
        ),
      routes.PartnersListController.displayPage()
    )

}
