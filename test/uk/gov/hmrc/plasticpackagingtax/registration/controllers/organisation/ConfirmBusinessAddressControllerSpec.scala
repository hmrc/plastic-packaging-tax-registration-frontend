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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.organisation

import base.unit.ControllerSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.Inspectors.forAll
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.json.JsObject
import play.api.test.Helpers.{redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.{routes => pptRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.Address
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.OrgType.{
  OVERSEAS_COMPANY_UK_BRANCH,
  OrgType,
  PARTNERSHIP,
  REGISTERED_SOCIETY,
  SOLE_TRADER,
  UK_COMPANY
}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.PartnershipTypeEnum.{
  GENERAL_PARTNERSHIP,
  LIMITED_LIABILITY_PARTNERSHIP,
  LIMITED_PARTNERSHIP,
  PartnershipTypeEnum,
  SCOTTISH_LIMITED_PARTNERSHIP,
  SCOTTISH_PARTNERSHIP
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.OrganisationDetails
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.organisation.confirm_business_address
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class ConfirmBusinessAddressControllerSpec extends ControllerSpec {
  private val page = mock[confirm_business_address]
  private val mcc  = stubMessagesControllerComponents()

  private val controller =
    new ConfirmBusinessAddressController(authenticate = mockAuthAction,
                                         mockJourneyAction,
                                         mockRegistrationConnector,
                                         mcc = mcc,
                                         page = page
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(page.apply(any[Address], any(), any())(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(page)
    super.afterEach()
  }

  "Confirm Company Address Controller" should {

    "return 200" when {

      "user is authorised and display page method is invoked" in {
        authorizedUser()
        mockRegistrationFind(
          aRegistration(
            withOrganisationDetails(
              OrganisationDetails(organisationType = Some(UK_COMPANY),
                                  incorporationDetails = Some(incorporationDetails)
              )
            )
          )
        )
        mockRegistrationUpdate()
        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
      }
    }

    "display company address" when {

      forAll(
        Seq(UK_COMPANY, SOLE_TRADER, REGISTERED_SOCIETY, OVERSEAS_COMPANY_UK_BRANCH, PARTNERSHIP)
      ) { organisationType =>
        "display address for " + organisationType when {
          "return 200 " in {
            organisationType match {
              case UK_COMPANY | REGISTERED_SOCIETY | OVERSEAS_COMPANY_UK_BRANCH | PARTNERSHIP =>
                mockRegistrationBusinessAddress(organisationType)
                val result = controller.displayPage()(getRequest())
                status(result) mustBe OK
              case _ =>
                mockRegistrationBusinessAddress(organisationType)
                val result = controller.displayPage()(getRequest())
                status(result) mustBe SEE_OTHER
                redirectLocation(result) mustBe Some(pptRoutes.TaskListController.displayPage().url)
            }
          }
        }
      }
      forAll(
        Seq(LIMITED_LIABILITY_PARTNERSHIP,
            LIMITED_PARTNERSHIP,
            GENERAL_PARTNERSHIP,
            SCOTTISH_PARTNERSHIP,
            SCOTTISH_LIMITED_PARTNERSHIP
        )
      ) { partnershipType =>
        "display address for " + partnershipType when {
          "return 200 " in {
            partnershipType match {
              case LIMITED_LIABILITY_PARTNERSHIP | LIMITED_PARTNERSHIP |
                  SCOTTISH_LIMITED_PARTNERSHIP =>
                mockRegistrationBusinessAddressForPartnership(partnershipType)
                val result = controller.displayPage()(getRequest())
                status(result) mustBe OK
              case _ =>
                mockRegistrationBusinessAddressForPartnership(partnershipType)
                val result = controller.displayPage()(getRequest())
                status(result) mustBe SEE_OTHER
                redirectLocation(result) mustBe Some(pptRoutes.TaskListController.displayPage().url)
            }
          }
        }
      }
    }
  }

  private def mockRegistrationBusinessAddress(organisationType: OrgType) =
    organisationType match {
      case UK_COMPANY | REGISTERED_SOCIETY | OVERSEAS_COMPANY_UK_BRANCH =>
        val registration = aRegistration(
          withOrganisationDetails(
            OrganisationDetails(organisationType = Some(organisationType),
                                incorporationDetails = Some(incorporationDetails)
            )
          )
        )
        authorizedUser()
        mockRegistrationFind(registration)
        mockRegistrationUpdate()
      case PARTNERSHIP =>
        val registration = aRegistration(
          withOrganisationDetails(
            OrganisationDetails(organisationType = Some(organisationType),
                                partnershipDetails =
                                  Some(partnershipDetailsWithBusinessAddress(LIMITED_PARTNERSHIP))
            )
          )
        )
        authorizedUser()
        mockRegistrationFind(registration)
        mockRegistrationUpdate()
      case _ => None
    }

  private def mockRegistrationBusinessAddressForPartnership(partnershipType: PartnershipTypeEnum) =
    partnershipType match {
      case LIMITED_LIABILITY_PARTNERSHIP | LIMITED_PARTNERSHIP | SCOTTISH_LIMITED_PARTNERSHIP =>
        val registration = aRegistration(
          withOrganisationDetails(
            OrganisationDetails(organisationType = Some(PARTNERSHIP),
                                partnershipDetails =
                                  Some(partnershipDetailsWithBusinessAddress(partnershipType))
            )
          )
        )
        authorizedUser()
        mockRegistrationFind(registration)
        mockRegistrationUpdate()
      case _ => None
    }

}
