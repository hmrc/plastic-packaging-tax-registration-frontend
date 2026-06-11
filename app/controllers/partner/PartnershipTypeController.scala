/*
 * Copyright 2026 HM Revenue & Customs
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

package controllers.partner

import config.AppConfig
import connectors._
import connectors.grs.{PartnershipGrsConnector, RegisteredSocietyGrsConnector, SoleTraderGrsConnector, UkCompanyGrsConnector}
import controllers.actions.JourneyAction
import controllers.organisation.{routes => organisationRoutes}
import forms.organisation.PartnerType
import forms.organisation.PartnerTypeEnum.{GENERAL_PARTNERSHIP, LIMITED_LIABILITY_PARTNERSHIP, LIMITED_PARTNERSHIP, SCOTTISH_LIMITED_PARTNERSHIP, SCOTTISH_PARTNERSHIP}
import models.genericregistration.PartnershipDetails
import models.registration.{Cacheable, Registration}
import models.request.JourneyRequest
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.GRSRedirections
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.organisation.partnership_type

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PartnershipTypeController @Inject() (
  journeyAction: JourneyAction,
  val appConfig: AppConfig,
  val soleTraderGrsConnector: SoleTraderGrsConnector,
  val ukCompanyGrsConnector: UkCompanyGrsConnector,
  val partnershipGrsConnector: PartnershipGrsConnector,
  val registeredSocietyGrsConnector: RegisteredSocietyGrsConnector,
  override val registrationConnector: RegistrationConnector,
  mcc: MessagesControllerComponents,
  page: partnership_type
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc)
    with Cacheable
    with I18nSupport
    with GRSRedirections {

  private val form: Form[PartnerType] = PartnerType.form(PartnerType.FormMode.PartnershipType)

  def displayPage(): Action[AnyContent] =
    journeyAction.register { implicit request =>
      request.registration.organisationDetails.partnershipDetails match {
        case Some(partnershipDetails) =>
          Ok(page(form.fill(PartnerType(partnershipDetails.partnershipType))))
        case _ => Ok(page(form))
      }
    }

  def submit(): Action[AnyContent] =
    journeyAction.register.async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[PartnerType]) => Future(BadRequest(page(formWithErrors))),
          partnerType =>
            updateRegistration(partnerType).flatMap {
              case Right(_) =>
                partnerType.answer match {
                  case GENERAL_PARTNERSHIP | SCOTTISH_PARTNERSHIP =>
                    Future(Redirect(routes.PartnershipNameController.displayPage().url))
                  case LIMITED_PARTNERSHIP =>
                    getPartnershipRedirectResultOrResume(
                      selectedPartnershipType = LIMITED_PARTNERSHIP,
                      createJourney =
                        getPartnershipRedirectUrl(appConfig.limitedPartnershipJourneyUrl, appConfig.grsCallbackUrl),
                      resumeUrl = appConfig.partnershipCompanyRegistrationNumberUrl
                    )
                  case SCOTTISH_LIMITED_PARTNERSHIP =>
                    getPartnershipRedirectResultOrResume(
                      selectedPartnershipType = SCOTTISH_LIMITED_PARTNERSHIP,
                      createJourney =
                        getPartnershipRedirectUrl(
                          appConfig.scottishLimitedPartnershipJourneyUrl,
                          appConfig.grsCallbackUrl
                        ),
                      resumeUrl = appConfig.partnershipCompanyRegistrationNumberUrl
                    )
                  case LIMITED_LIABILITY_PARTNERSHIP =>
                    getPartnershipRedirectResultOrResume(
                      selectedPartnershipType = LIMITED_LIABILITY_PARTNERSHIP,
                      createJourney =
                        getPartnershipRedirectUrl(
                          appConfig.limitedLiabilityPartnershipJourneyUrl,
                          appConfig.grsCallbackUrl
                        ),
                      resumeUrl = appConfig.partnershipCompanyRegistrationNumberUrl
                    )
                  case _ =>
                    Future(Redirect(organisationRoutes.RegisterAsOtherOrganisationController.onPageLoad()))
                }
              case Left(error) => throw error
            }
        )
    }

  private def getPartnershipRedirectResultOrResume(
    selectedPartnershipType: forms.organisation.PartnerTypeEnum,
    createJourney: => Future[String],
    resumeUrl: String => String
  )(implicit request: JourneyRequest[AnyContent]): Future[play.api.mvc.Result] =
    request.registration.incorpJourneyId match {
      case Some(journeyId)
          if request.registration.organisationDetails.partnershipDetails.exists(
            _.partnershipType == selectedPartnershipType
          ) =>
        Future.successful(SeeOther(resumeUrl(journeyId)))
      case _ =>
        createJourney.flatMap { journeyStartUrl =>
          extractPartnershipJourneyId(journeyStartUrl).fold(Future.successful(SeeOther(journeyStartUrl))) { journeyId =>
            update { registration =>
              registration.copy(
                incorpJourneyId = Some(journeyId),
                organisationDetails = registration.organisationDetails.copy(
                  partnershipDetails = Some(
                    registration.organisationDetails.partnershipDetails
                      .getOrElse(PartnershipDetails(partnershipType = selectedPartnershipType))
                      .copy(partnershipType = selectedPartnershipType)
                  )
                )
              )
            }.map {
              case Right(_)    => SeeOther(journeyStartUrl)
              case Left(error) => throw error
            }
          }
        }
    }

  private def extractPartnershipJourneyId(journeyStartUrl: String): Option[String] =
    """.*/identify-your-partnership/([^/]+)/company-registration-number$""".r
      .findFirstMatchIn(journeyStartUrl)
      .map(_.group(1))
      .orElse(
        """/identify-your-partnership/([^/]+)/company-registration-number$""".r
          .findFirstMatchIn(journeyStartUrl)
          .map(_.group(1))
      )

  private def updateRegistration(
    formData: PartnerType
  )(implicit req: JourneyRequest[AnyContent]): Future[Either[ServiceError, Registration]] = {

    val partnershipType = formData.answer
    update { registration =>
      registration.organisationDetails.partnershipDetails match {
        case Some(_) =>
          registration.copy(organisationDetails =
            registration.organisationDetails.copy(partnershipDetails =
              Some(
                registration.organisationDetails.partnershipDetails.get.copy(partnershipType = partnershipType)
              )
            )
          )
        case _ =>
          registration.copy(organisationDetails =
            registration.organisationDetails.copy(partnershipDetails =
              Some(PartnershipDetails(partnershipType = partnershipType))
            )
          )
      }

    }
  }

}
