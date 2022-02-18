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
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatest.Inspectors.forAll
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.test.Helpers.{contentAsString, redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.organisation.{routes => orgRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.partner.{routes => partnerRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.{routes => pptRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.PartnerTypeEnum
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.PartnerTypeEnum._
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.{
  IncorpEntityGrsCreateRequest,
  PartnershipGrsCreateRequest,
  SoleTraderGrsCreateRequest
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{
  NewRegistrationUpdateService,
  Registration
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.organisation.partner_type
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class PartnerTypeControllerSpec extends ControllerSpec {

  private val page = mock[partner_type]
  private val mcc  = stubMessagesControllerComponents()

  protected val mockNewRegistrationUpdater = new NewRegistrationUpdateService(
    mockRegistrationConnector
  )

  private val controller = new PartnerTypeController(
    authenticate = mockAuthAction,
    journeyAction = mockJourneyAction,
    appConfig = appConfig,
    soleTraderGrsConnector = mockSoleTraderGrsConnector,
    ukCompanyGrsConnector = mockUkCompanyGrsConnector,
    partnershipGrsConnector = mockPartnershipGrsConnector,
    registeredSocietyGrsConnector = mockRegisteredSocietyGrsConnector,
    registrationUpdater =
      mockNewRegistrationUpdater,
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
    when(page.apply(any(), any(), any())(any(), any())).thenReturn(
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

  "Partner Type Controller" should {

    "successfully return partnership partner type selection page" when {
      "no previous partnership partner type in registration" in {
        val registration = aRegistration(
          withPartnershipDetails(
            Some(generalPartnershipDetails.copy(partners = Seq(nominatedPartner(UK_COMPANY))))
          )
        )

        authorizedUser()
        mockRegistrationFind(registration)

        val result = controller.displayNewPartner()(getRequest())

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

        val result = controller.displayExistingPartner("123")(getRequest())

        status(result) mustBe OK
      }
    }

    "redirect to partnership GRS for new partner" when {
      "user selected a type which has a GRS provided name" when {
        forAll(
          Seq(
            (LIMITED_LIABILITY_PARTNERSHIP,
             llpPartnershipDetails.copy(partners =
               Seq(nominatedPartner(LIMITED_LIABILITY_PARTNERSHIP))
             )
            ),
            (SCOTTISH_LIMITED_PARTNERSHIP,
             scottishPartnershipDetails.copy(partners =
               Seq(nominatedPartner(SCOTTISH_LIMITED_PARTNERSHIP))
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
            (REGISTERED_SOCIETY,
             scottishPartnershipDetails.copy(partners =
               Seq(nominatedPartner(PartnerTypeEnum.REGISTERED_SOCIETY))
             )
            ),
            (OVERSEAS_COMPANY_UK_BRANCH,
             scottishPartnershipDetails.copy(partners =
               Seq(nominatedPartner(PartnerTypeEnum.OVERSEAS_COMPANY_UK_BRANCH))
             )
            ),
            (CHARITABLE_INCORPORATED_ORGANISATION, scottishPartnershipDetails)
          )
        ) { partnershipDetails =>
          s"a ${partnershipDetails._1} type was selected" in {
            val registration = aRegistration(withPartnershipDetails(Some(partnershipDetails._2)))

            authorizedUser()
            mockRegistrationFind(registration)
            mockRegistrationUpdate()

            mockCreateSoleTraderPartnershipGrsJourneyCreation("http://test/redirect/soletrader")
            mockCreateUkCompanyPartnershipGrsJourneyCreation("http://test/redirect/ukCompany")
            mockCreateRegisteredSocietyPartnershipGrsJourneyCreation(
              "http://test/redirect/registeredSociety"
            )
            mockCreatePartnershipGrsJourneyCreation("http://test/redirect/partnership")

            val correctForm =
              Seq("answer" -> partnershipDetails._1.toString, saveAndContinueFormAction)
            val result = controller.submitNewPartner()(postJsonRequestEncoded(correctForm: _*))
            partnershipDetails._1 match {
              case SOLE_TRADER =>
                redirectLocation(result) mustBe Some("http://test/redirect/soletrader")
                val soleTraderGrsCreateRequest: ArgumentCaptor[SoleTraderGrsCreateRequest] =
                  ArgumentCaptor.forClass(classOf[SoleTraderGrsCreateRequest])
                val grsUrlCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
                verify(mockSoleTraderGrsConnector).createJourney(
                  soleTraderGrsCreateRequest.capture(),
                  grsUrlCaptor.capture()
                )(any(), any())
                soleTraderGrsCreateRequest.getValue.businessVerificationCheck mustBe false
              case UK_COMPANY | OVERSEAS_COMPANY_UK_BRANCH =>
                redirectLocation(result) mustBe Some("http://test/redirect/ukCompany")
                val incorpEntityGrsCreateRequest: ArgumentCaptor[IncorpEntityGrsCreateRequest] =
                  ArgumentCaptor.forClass(classOf[IncorpEntityGrsCreateRequest])
                val grsUrlCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
                verify(mockUkCompanyGrsConnector).createJourney(
                  incorpEntityGrsCreateRequest.capture(),
                  grsUrlCaptor.capture()
                )(any(), any())
                incorpEntityGrsCreateRequest.getValue.businessVerificationCheck mustBe false
              case REGISTERED_SOCIETY =>
                redirectLocation(result) mustBe Some("http://test/redirect/registeredSociety")
                val incorpEntityGrsCreateRequest: ArgumentCaptor[IncorpEntityGrsCreateRequest] =
                  ArgumentCaptor.forClass(classOf[IncorpEntityGrsCreateRequest])
                val grsUrlCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
                verify(mockRegisteredSocietyGrsConnector).createJourney(
                  incorpEntityGrsCreateRequest.capture(),
                  grsUrlCaptor.capture()
                )(any(), any())
                incorpEntityGrsCreateRequest.getValue.businessVerificationCheck mustBe false
              case LIMITED_LIABILITY_PARTNERSHIP | SCOTTISH_LIMITED_PARTNERSHIP =>
                redirectLocation(result) mustBe Some("http://test/redirect/partnership")
                val partnerGrsCreateRequest: ArgumentCaptor[PartnershipGrsCreateRequest] =
                  ArgumentCaptor.forClass(classOf[PartnershipGrsCreateRequest])
                val grsUrlCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
                verify(mockPartnershipGrsConnector).createJourney(partnerGrsCreateRequest.capture(),
                                                                  grsUrlCaptor.capture()
                )(any(), any())
                partnerGrsCreateRequest.getValue.businessVerificationCheck mustBe false
              case _ =>
                redirectLocation(result) mustBe Some(
                  orgRoutes.OrganisationTypeNotSupportedController.onPageLoad().url
                )
            }
          }
        }
      }
    }

    "redirect to capture partner name for new partner" when {
      "user selected a type which has no GRS provided name" when {
        forAll(
          Seq((GENERAL_PARTNERSHIP, generalPartnershipDetails),
              (SCOTTISH_PARTNERSHIP, scottishPartnershipDetails)
          )
        ) { partnershipDetails =>
          s"a ${partnershipDetails._1} type was selected" in {
            val registration = aRegistration(withPartnershipDetails(Some(partnershipDetails._2)))

            authorizedUser()
            mockRegistrationFind(registration)
            mockRegistrationUpdate()

            mockCreateSoleTraderPartnershipGrsJourneyCreation("http://test/redirect/soletrader")
            mockCreateUkCompanyPartnershipGrsJourneyCreation("http://test/redirect/ukCompany")
            mockCreatePartnershipGrsJourneyCreation("http://test/redirect/partnership")

            val correctForm =
              Seq("answer" -> partnershipDetails._1.toString, saveAndContinueFormAction)

            val result = controller.submitNewPartner()(postJsonRequestEncoded(correctForm: _*))

            redirectLocation(result) mustBe Some(
              partnerRoutes.PartnerNameController.displayNewPartner().url
            )
          }
        }
      }
    }

    "redirect to partnership GRS for existing partner" when {
      "existing partner selects a type which has a GRS supplied name" in {
        val registration =
          aRegistration(withPartnershipDetails(Some(generalPartnershipDetailsWithPartners)))

        authorizedUser()
        mockRegistrationFind(registration)
        mockRegistrationUpdate()
        mockCreateSoleTraderPartnershipGrsJourneyCreation("http://test/redirect/soletrader")

        val correctForm =
          Seq("answer" -> SOLE_TRADER.toString, saveAndContinueFormAction)

        val result =
          controller.submitExistingPartner("123")(postJsonRequestEncoded(correctForm: _*))

        redirectLocation(result) mustBe Some("http://test/redirect/soletrader")
      }
    }

    "redirect to capture partner name for existing partner" when {
      "existing partner selects a type which does not have a have GRS provided name" in {
        val registration =
          aRegistration(withPartnershipDetails(Some(generalPartnershipDetailsWithPartners)))

        authorizedUser()
        mockRegistrationFind(registration)
        mockRegistrationUpdate()

        val correctForm =
          Seq("answer" -> SCOTTISH_PARTNERSHIP.toString, saveAndContinueFormAction)

        val result =
          controller.submitExistingPartner("123")(postJsonRequestEncoded(correctForm: _*))

        redirectLocation(result) mustBe Some(
          partnerRoutes.PartnerNameController.displayExistingPartner("123").url
        )
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
        val result = controller.submitNewPartner()(postJsonRequestEncoded(correctForm: _*))

        redirectLocation(result) mustBe Some(pptRoutes.TaskListController.displayPage().url)
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

      val result = controller.submitNewPartner()(postJsonRequestEncoded(incompleteForm: _*))

      status(result) mustBe BAD_REQUEST
    }

    "display nominated partner type capture page" when {
      "user is authorized" when {
        "registration does not contain partnership partner" in {
          val resp = controller.displayNewPartner()(getRequest())

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

          val resp = controller.displayNewPartner()(getRequest())

          contentAsString(resp) mustBe "Nominated partner type capture"
        }
      }
    }

  }

}
