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

import base.unit.ControllerSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.Inspectors.forAll
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.test.Helpers.{await, contentAsString, redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.DownstreamServiceError
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.{routes => pptRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.PartnerTypeEnum
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.PartnerTypeEnum._
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.organisation.partner_type
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.organisation.{routes => orgRoutes}

class NominatedPartnerTypeControllerSpec extends ControllerSpec {

  private val page = mock[partner_type]
  private val mcc  = stubMessagesControllerComponents()

  private val controller = new NominatedPartnerTypeController(
    authenticate = mockAuthAction,
    journeyAction = mockJourneyAction,
    appConfig = appConfig,
    soleTraderGrsConnector = mockSoleTraderGrsConnector,
    ukCompanyGrsConnector = mockUkCompanyGrsConnector,
    partnershipGrsConnector = mockPartnershipGrsConnector,
    registrationConnector = mockRegistrationConnector,
    mcc = mcc,
    page = page
  )

  private val partnershipRegistration = aRegistration(
    withPartnershipDetails(
      Some(generalPartnershipDetails.copy(partners = Seq(nominatedPartner(UK_COMPANY))))
    )
  )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    authorizedUser()
    when(page.apply(any())(any(), any())).thenReturn(
      HtmlFormat.raw("Nominated partner type capture")
    )
    mockRegistrationFind(partnershipRegistration)
    mockRegistrationUpdate()
    mockCreatePartnershipGrsJourneyCreation("/partnership-grs-journey")

    when(appConfig.generalPartnershipJourneyUrl).thenReturn(
      "/general-partnership-grs-journey-creation"
    )
    when(appConfig.scottishPartnershipJourneyUrl).thenReturn(
      "/scottish-partnership-grs-journey-creation"
    )
  }

  "Partnership Partner Type Controller" should {

    "successfully return partnership partner type selection page" when {
      "no previous partnership partner type in registration" in {
        val registration = aRegistration(
          withPartnershipDetails(
            Some(generalPartnershipDetails.copy(partners = Seq(nominatedPartner(UK_COMPANY))))
          )
        )

        authorizedUser()
        mockRegistrationFind(registration)

        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
      }

      "previous partnership partner type in registration" in {
        val registration = aRegistration(
          withPartnershipDetails(
            Some(generalPartnershipDetails.copy(partners = Seq(nominatedPartner(UK_COMPANY))))
          )
        )

        authorizedUser()
        mockRegistrationFind(registration)

        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
      }
    }

    "redirect to partnership GRS" when {
      forAll(
        Seq(
          (SCOTTISH_LIMITED_PARTNERSHIP,
           scottishPartnershipDetails.copy(partners =
             Seq(nominatedPartner(SCOTTISH_LIMITED_PARTNERSHIP))
           )
          ),
          (LIMITED_LIABILITY_PARTNERSHIP,
           llpPartnershipDetails.copy(partners =
             Seq(nominatedPartner(LIMITED_LIABILITY_PARTNERSHIP))
           )
          ),
          (SOLE_TRADER,
           scottishPartnershipDetails.copy(partners =
             Seq(nominatedPartner(PartnerTypeEnum.SOLE_TRADER))
           )
          ),
          (UK_COMPANY,
           scottishPartnershipDetails.copy(partners =
             Seq(nominatedPartner(PartnerTypeEnum.UK_COMPANY))
           )
          ),
          (OVERSEAS_COMPANY_UK_BRANCH,
           scottishPartnershipDetails.copy(partners =
             Seq(nominatedPartner(PartnerTypeEnum.OVERSEAS_COMPANY_UK_BRANCH))
           )
          ),
          (SCOTTISH_PARTNERSHIP, scottishPartnershipDetails),
          (CHARITABLE_INCORPORATED_ORGANISATION, scottishPartnershipDetails)
        )
      ) { partnershipDetails =>
        s"a ${partnershipDetails._1} type was selected" in {
          val registration = aRegistration(withPartnershipDetails(Some(partnershipDetails._2)))

          authorizedUser()
          mockRegistrationFind(registration)
          mockRegistrationUpdate()
          partnershipDetails._1 match {
            case SOLE_TRADER =>
              mockCreateSoleTraderPartnershipGrsJourneyCreation("http://test/redirect/soletrader")
            case UK_COMPANY | OVERSEAS_COMPANY_UK_BRANCH =>
              mockCreateUkCompanyPartnershipGrsJourneyCreation("http://test/redirect/ukCompany")
            case LIMITED_LIABILITY_PARTNERSHIP | SCOTTISH_PARTNERSHIP |
                SCOTTISH_LIMITED_PARTNERSHIP =>
              mockCreatePartnershipGrsJourneyCreation("http://test/redirect/partnership")
            case _ => None
          }

          val correctForm =
            Seq("answer" -> partnershipDetails._1.toString, saveAndContinueFormAction)
          val result = controller.submit()(postJsonRequestEncoded(correctForm: _*))
          partnershipDetails._1 match {
            case SOLE_TRADER =>
              redirectLocation(result) mustBe Some("http://test/redirect/soletrader")
            case UK_COMPANY | OVERSEAS_COMPANY_UK_BRANCH =>
              redirectLocation(result) mustBe Some("http://test/redirect/ukCompany")
            case LIMITED_LIABILITY_PARTNERSHIP | SCOTTISH_PARTNERSHIP |
                SCOTTISH_LIMITED_PARTNERSHIP =>
              redirectLocation(result) mustBe Some("http://test/redirect/partnership")
            case _ =>
              redirectLocation(result) mustBe Some(
                orgRoutes.OrganisationTypeNotSupportedController.onPageLoad().url
              )
          }
        }
      }
    }

    "redirect to registration main page" when {
      "save and come back later button is used" in {
        val registration = aRegistration(
          withPartnershipDetails(
            Some(llpPartnershipDetails.copy(partners = Seq(nominatedPartner(UK_COMPANY))))
          )
        )

        authorizedUser()
        mockRegistrationFind(registration)
        mockRegistrationUpdate()

        val correctForm =
          Seq("answer" -> LIMITED_LIABILITY_PARTNERSHIP.toString, saveAndComeBackLaterFormAction)
        val result = controller.submit()(postJsonRequestEncoded(correctForm: _*))

        redirectLocation(result) mustBe Some(pptRoutes.TaskListController.displayPage().url)
      }
    }

    "throw errors resulting from failed registration updates for LLP" in {
      val registration = aRegistration(
        withPartnershipDetails(
          Some(llpPartnershipDetails.copy(partners = Seq(nominatedPartner(UK_COMPANY))))
        )
      )

      authorizedUser()
      mockRegistrationFind(registration)
      mockRegistrationUpdateFailure()

      val correctForm =
        Seq("answer" -> LIMITED_LIABILITY_PARTNERSHIP.toString, saveAndContinueFormAction)

      intercept[DownstreamServiceError] {
        await(controller.submit()(postJsonRequestEncoded(correctForm: _*)))
      }
    }

    "returns bad request when empty radio button submitted" in {
      val registration = aRegistration(
        withPartnershipDetails(
          Some(generalPartnershipDetails.copy(partners = Seq(nominatedPartner(UK_COMPANY))))
        )
      )

      authorizedUser()
      mockRegistrationFind(registration)
      mockRegistrationUpdateFailure()

      val incompleteForm = Seq(saveAndContinueFormAction)

      val result = controller.submit()(postJsonRequestEncoded(incompleteForm: _*))

      status(result) mustBe BAD_REQUEST
    }

    "display nominated partner type capture page" when {
      "user is authorized" when {
        "registration does not contain partnership partner" in {
          val resp = controller.displayPage()(getRequest())

          contentAsString(resp) mustBe "Nominated partner type capture"
        }
        "registration does contain nominated partnership type" in {
          mockRegistrationFind(
            aRegistration(
              withPartnershipDetails(
                Some(
                  generalPartnershipDetails.copy(partners =
                    Seq(nominatedPartner(PartnerTypeEnum.UK_COMPANY))
                  )
                )
              )
            )
          )

          val resp = controller.displayPage()(getRequest())

          contentAsString(resp) mustBe "Nominated partner type capture"
        }
      }
    }

  }

}