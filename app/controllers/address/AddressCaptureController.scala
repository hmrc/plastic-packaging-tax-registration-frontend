/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.address

import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.http.HeaderCarrier
import config.AppConfig
import connectors.addresslookup.AddressLookupFrontendConnector
import controllers.AddressLookupIntegration
import controllers.actions.auth.BasicAuthAction
import forms.address.UkAddressForm
import forms.contact.Address
import models.addresslookup.{AddressLookupOnRamp, MissingAddressIdException}
import models.request.AuthenticatedRequest
import services.{AddressCaptureConfig, AddressCaptureService, CountryService}
import utils.AddressConversionUtils
import views.html.address.{address_page, uk_address_page}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddressCaptureController @Inject() (
                                           authenticate: BasicAuthAction,
                                           mcc: MessagesControllerComponents,
                                           addressCaptureService: AddressCaptureService,
                                           addressLookupFrontendConnector: AddressLookupFrontendConnector,
                                           appConfig: AppConfig,
                                           addressInUkPage: uk_address_page,
                                           addressPage: address_page,
                                           countryService: CountryService,
                                           addressConversionUtils: AddressConversionUtils
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport with AddressLookupIntegration {

  def initialiseAddressCapture(): Action[AnyContent] =
    authenticate.async { implicit request =>
      getAddressCaptureConfig().flatMap(
        addressCaptureConfig =>
          if (addressCaptureConfig.forceUkAddress)
            initialiseAddressLookup(addressCaptureConfig).map(onRamp => Redirect(onRamp.redirectUrl))
          else
            Future.successful(Redirect(routes.AddressCaptureController.addressInUk()))
      )
    }

  def addressInUk(): Action[AnyContent] =
    authenticate.async { implicit request =>
      getAddressCaptureConfig().map { config =>
        Ok(addressInUkPage(UkAddressForm.form(), config.entityName, config.pptHeadingKey + ".isUK"))
      }
    }

  def submitAddressInUk(): Action[AnyContent] =
    authenticate.async { implicit request =>
      getAddressCaptureConfig().flatMap { config =>
        UkAddressForm
          .form()
          .bindFromRequest()
          .fold(
            error => Future.successful(BadRequest(addressInUkPage(error, config.entityName, config.pptHeadingKey + ".isUK"))),
            ukAddress =>
              if (ukAddress)
                initialiseAddressLookup(config).map(onRamp => Redirect(onRamp.redirectUrl))
              else
                Future.successful(Redirect(routes.AddressCaptureController.captureAddress()))
          )
      }
    }

  def captureAddress() =
    authenticate.async { implicit request =>
      getAddressCaptureConfig().map { config =>
        Ok(buildAddressPage(Address.form(), config))
      }
    }

  def submitAddress() =
    authenticate.async { implicit request =>
      getAddressCaptureConfig().flatMap { config =>
        Address.form()
          .bindFromRequest()
          .fold(
            (formWithErrors: Form[Address]) => Future.successful(BadRequest(buildAddressPage(formWithErrors, config))),
            address =>
              addressCaptureService.setCapturedAddress(address).map { _ =>
                Redirect(config.successLink)
              }
          )
      }
    }

  private def initialiseAddressLookup(addressCaptureConfig: AddressCaptureConfig)(implicit hc: HeaderCarrier): Future[AddressLookupOnRamp] =
    initialiseAddressLookup(
      addressLookupFrontendConnector,
      appConfig,
      controllers.address.routes.AddressCaptureController.alfCallback(None),
      addressCaptureConfig.alfHeadingsPrefix,
      entityName = addressCaptureConfig.entityName
    )

  private def buildAddressPage(form: Form[Address], config: AddressCaptureConfig)(implicit request: Request[_]) =
    addressPage(
      form,
      countryService.getAll(),
      routes.AddressCaptureController.submitAddress(),
      config.pptHeadingKey,
      config.entityName,
      config.pptHintKey
    )

  def alfCallback(id: Option[String]): Action[AnyContent] =
    authenticate.async { implicit request =>
      getAddressCaptureConfig().flatMap { config =>
        addressLookupFrontendConnector.getAddress(id.getOrElse(throw new MissingAddressIdException)).flatMap {
          confirmedAddress =>
            val pptAddress  = addressConversionUtils.toPptAddress(confirmedAddress)
            val addressForm = Address.validateAsInput(pptAddress)

            if (addressForm.errors.nonEmpty)
              Future.successful(BadRequest(buildAddressPage(addressForm, config)))
            else
              addressCaptureService.setCapturedAddress(pptAddress).map {
                _ => Redirect(config.successLink)
              }
        }
      }
    }

  private def getAddressCaptureConfig()(implicit request: AuthenticatedRequest[Any]): Future[AddressCaptureConfig] =
    addressCaptureService.getAddressCaptureDetails().map(
      acd => acd.map(_.config).getOrElse(throw new IllegalStateException("Address capture config absent"))
    )

}
