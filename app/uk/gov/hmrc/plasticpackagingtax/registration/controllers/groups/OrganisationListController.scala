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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.groups

import javax.inject.{Inject, Singleton}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.RegistrationConnector
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.{
  AuthAction,
  FormAction,
  SaveAndContinue
}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.groups.AddOrganisation
import uk.gov.hmrc.plasticpackagingtax.registration.forms.groups.AddOrganisation.form
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Cacheable
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.JourneyAction
import uk.gov.hmrc.plasticpackagingtax.registration.views.groups.html.organisation_list
import uk.gov.hmrc.plasticpackagingtax.registration.{controllers => pptControllers}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

@Singleton
class OrganisationListController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  override val registrationConnector: RegistrationConnector,
  mcc: MessagesControllerComponents,
  page: organisation_list
) extends FrontendController(mcc) with Cacheable with I18nSupport {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      request.registration.groupDetail.fold(
        Redirect(pptControllers.routes.RegistrationController.displayPage())
      )(
        detail =>
          if (
            detail.members.isEmpty || request.registration.organisationDetails.businessName.isEmpty
          )
            // TODO goto select member page
            Redirect(pptControllers.routes.RegistrationController.displayPage())
          else
            Ok(
              page(form(),
                   request.registration.organisationDetails.businessName.get,
                   detail.members
              )
            )
      )
    }

  def submit(): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      form().bindFromRequest().fold(
        (formWithErrors: Form[AddOrganisation]) =>
          BadRequest(
            page(formWithErrors,
                 request.registration.organisationDetails.businessName.get,
                 request.registration.groupDetail.map(_.members).get
            )
          ),
        addOrganisation =>
          FormAction.bindFromRequest match {
            case SaveAndContinue =>
              if (addOrganisation.answer.getOrElse(true))
                Redirect(
                  pptControllers.groups.routes.OrganisationListController.displayPage()
                ) // TODO goto select member page
              else Redirect(pptControllers.routes.RegistrationController.displayPage())
            case _ => Redirect(pptControllers.routes.RegistrationController.displayPage())
          }
      )

    }

}