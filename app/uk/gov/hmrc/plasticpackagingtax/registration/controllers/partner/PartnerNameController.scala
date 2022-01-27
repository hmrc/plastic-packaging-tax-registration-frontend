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

import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.connectors._
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.grs.PartnershipGrsConnector
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.{
  AuthAction,
  FormAction,
  SaveAndContinue
}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.organisation.{
  routes => organisationRoutes
}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.partner.{routes => partnerRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.{routes => commonRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.PartnerTypeEnum
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.PartnerTypeEnum.{
  LIMITED_LIABILITY_PARTNERSHIP,
  PartnerTypeEnum,
  SCOTTISH_LIMITED_PARTNERSHIP,
  SCOTTISH_PARTNERSHIP
}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.partner.PartnerName
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.{
  Partner,
  PartnershipGrsCreateRequest
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{Cacheable, Registration}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.partner.partner_name_page
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PartnerNameController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  override val registrationConnector: RegistrationConnector,
  appConfig: AppConfig,
  partnershipGrsConnector: PartnershipGrsConnector,
  mcc: MessagesControllerComponents,
  page: partner_name_page
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport {

  private val partnerTypesWhichPermitUserSuppliedNames = Set(
    PartnerTypeEnum.SCOTTISH_PARTNERSHIP,
    PartnerTypeEnum.SCOTTISH_LIMITED_PARTNERSHIP,
    PartnerTypeEnum.LIMITED_LIABILITY_PARTNERSHIP
  )

  def displayNewPartner(): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      request.registration.inflightPartner.map { partner =>
        renderPageFor(partner,
                      partnerRoutes.PartnerTypeController.displayNewPartner(),
                      partnerRoutes.PartnerNameController.submitNewPartner()
        )
      }.getOrElse(throw new IllegalStateException("Expected partner missing"))
    }

  def displayExistingPartner(partnerId: String): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      request.registration.findPartner(partnerId).map { partner =>
        renderPageFor(partner,
                      partnerRoutes.PartnerCheckAnswersController.displayExistingPartner(partnerId),
                      partnerRoutes.PartnerNameController.submitExistingPartner(partnerId)
        )
      }.getOrElse(throw new IllegalStateException("Expected partner missing"))
    }

  def submitNewPartner(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      request.registration.inflightPartner.map { partner =>
        partner.partnerType.map { partnerType =>
          handleSubmission(partnerType,
                           None,
                           partnerRoutes.PartnerTypeController.displayNewPartner(),
                           partnerRoutes.PartnerNameController.submitNewPartner(),
                           commonRoutes.TaskListController.displayPage(),
                           updateInflightPartner
          )
        }.getOrElse(
          Future.successful(throw new IllegalStateException("Expected partner type missing"))
        )
      }.getOrElse(Future.successful(throw new IllegalStateException("Expected partner missing")))
    }

  def submitExistingPartner(partnerId: String): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      request.registration.findPartner(partnerId).map { partner =>
        partner.partnerType.map { partnerType =>
          def updateAction(partnerName: PartnerName): Future[Either[ServiceError, Registration]] =
            updateExistingPartner(partnerName, partnerId)
          handleSubmission(partnerType,
                           Some(partner.id),
                           partnerRoutes.PartnerTypeController.displayExistingPartner(partnerId),
                           partnerRoutes.PartnerNameController.submitExistingPartner(partnerId),
                           commonRoutes.TaskListController.displayPage(),
                           updateAction
          )
        }.getOrElse(
          Future.successful(throw new IllegalStateException("Expected partner type missing"))
        )
      }.getOrElse {
        Future.successful(throw new IllegalStateException("Expected partner missing"))
      }
    }

  private def renderPageFor(partner: Partner, backCall: Call, submitCall: Call)(implicit
    request: JourneyRequest[AnyContent]
  ): Result =
    if (partner.partnerType.exists(isPartnerTypeWhichPermitsUserSuppliedNames)) {
      val form = partner.userSuppliedName match {
        case Some(data) =>
          PartnerName.form().fill(PartnerName(data))
        case None =>
          PartnerName.form()
      }
      Ok(page(form, backCall, submitCall))
    } else
      throw new IllegalStateException("Partner type does not permit user supplied names")

  private def handleSubmission(
    partnerType: PartnerTypeEnum,
    existingPartnerId: Option[String],
    backCall: Call,
    submitCall: Call,
    dropoutCall: Call,
    updateAction: PartnerName => Future[Either[ServiceError, Registration]]
  )(implicit request: JourneyRequest[AnyContent]): Future[Result] =
    if (isPartnerTypeWhichPermitsUserSuppliedNames(partnerType))
      PartnerName.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[PartnerName]) =>
            Future.successful(BadRequest(page(formWithErrors, backCall, submitCall))),
          partnerName =>
            updateAction(partnerName).flatMap {
              case Right(_) =>
                FormAction.bindFromRequest match {
                  case SaveAndContinue =>
                    // Select GRS journey type based on selected partner type
                    partnerType match {
                      // TODO deduplicate this with PartnerTypeController
                      case LIMITED_LIABILITY_PARTNERSHIP =>
                        getPartnershipRedirectUrl(existingPartnerId,
                                                  appConfig.limitedLiabilityPartnershipJourneyUrl
                        ).map(journeyStartUrl => SeeOther(journeyStartUrl).addingToSession())
                      case SCOTTISH_PARTNERSHIP =>
                        getPartnershipRedirectUrl(existingPartnerId,
                                                  appConfig.scottishPartnershipJourneyUrl
                        ).map(journeyStartUrl => SeeOther(journeyStartUrl).addingToSession())
                      case SCOTTISH_LIMITED_PARTNERSHIP =>
                        getPartnershipRedirectUrl(existingPartnerId,
                                                  appConfig.scottishLimitedPartnershipJourneyUrl
                        ).map(journeyStartUrl => SeeOther(journeyStartUrl).addingToSession())
                      case _ =>
                        //TODO later CHARITABLE_INCORPORATED_ORGANISATION & OVERSEAS_COMPANY_NO_UK_BRANCH will have their own not supported page
                        Future(
                          Redirect(
                            organisationRoutes.OrganisationTypeNotSupportedController.onPageLoad()
                          )
                        )
                    }
                  case _ =>
                    Future.successful(Redirect(dropoutCall))
                }
              case Left(error) => throw error
            }
        )
    else
      throw new IllegalStateException("Partner type does not permit user supplied names")

  private def updateInflightPartner(
    formData: PartnerName
  )(implicit req: JourneyRequest[AnyContent]): Future[Either[ServiceError, Registration]] =
    update { registration =>
      registration.inflightPartner.map { partner =>
        val withName = partner.copy(userSuppliedName = Some(formData.value))
        registration.withInflightPartner(Some(withName))
      }.getOrElse {
        registration
      }
    }

  private def updateExistingPartner(formData: PartnerName, partnerId: String)(implicit
    req: JourneyRequest[AnyContent]
  ): Future[Either[ServiceError, Registration]] =
    update { registration =>
      registration.withUpdatedPartner(
        partnerId,
        partner => partner.copy(userSuppliedName = Some(formData.value))
      )
    }

  private def getPartnershipRedirectUrl(
    existingPartnerId: Option[String],
    createJourneyUrlForPartnershipType: String
  )(implicit request: JourneyRequest[AnyContent]): Future[String] = {
    val callbackUrl = appConfig.partnerGrsCallbackUrl(existingPartnerId)
    partnershipGrsConnector.createJourney(
      PartnershipGrsCreateRequest(callbackUrl,
                                  Some(request2Messages(request)("service.name")),
                                  appConfig.serviceIdentifier,
                                  appConfig.signOutLink,
                                  appConfig.grsAccessibilityStatementPath
      ),
      createJourneyUrlForPartnershipType
    )
  }

  private def isPartnerTypeWhichPermitsUserSuppliedNames(partnerType: PartnerTypeEnum): Boolean =
    partnerTypesWhichPermitUserSuppliedNames.contains(partnerType)

}
