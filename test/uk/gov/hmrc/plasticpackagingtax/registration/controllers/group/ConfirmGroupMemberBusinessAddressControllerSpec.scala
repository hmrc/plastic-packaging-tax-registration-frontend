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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.group

import base.unit.ControllerSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.Inspectors.forAll
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.test.Helpers.{redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.Address
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.OrgType.{
  CHARITABLE_INCORPORATED_ORGANISATION,
  OVERSEAS_COMPANY_UK_BRANCH,
  OrgType,
  PARTNERSHIP,
  UK_COMPANY
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.GroupDetail
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.organisation.confirm_business_address
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class ConfirmGroupMemberBusinessAddressControllerSpec extends ControllerSpec {
  private val page = mock[confirm_business_address]
  private val mcc  = stubMessagesControllerComponents()

  private val controller =
    new ConfirmGroupMemberBusinessAddressController(authenticate = mockAuthAction,
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
    forAll(
      Seq(UK_COMPANY, OVERSEAS_COMPANY_UK_BRANCH, PARTNERSHIP, CHARITABLE_INCORPORATED_ORGANISATION)
    ) { organisationType =>
      "display address for " + organisationType when {
        "return 200 " in {
          organisationType match {
            case UK_COMPANY | OVERSEAS_COMPANY_UK_BRANCH | PARTNERSHIP =>
              mockRegistrationBusinessAddress(organisationType)
              val result = controller.displayPage()(getRequest())
              status(result) mustBe OK
            case _ =>
              mockRegistrationBusinessAddress(organisationType)
              val result = controller.displayPage()(getRequest())
              status(result) mustBe SEE_OTHER
              redirectLocation(result) mustBe Some(
                routes.OrganisationListController.displayPage().url
              )
          }
        }
      }
    }
  }

  private def mockRegistrationBusinessAddress(organisationType: OrgType) =
    organisationType match {
      case UK_COMPANY | PARTNERSHIP | OVERSEAS_COMPANY_UK_BRANCH =>
        val registration = aRegistration(
          withGroupDetail(
            Some(
              GroupDetail(membersUnderGroupControl = Some(true),
                          members = Seq(groupMemberForOrganisationType(organisationType)),
                          currentMemberOrganisationType = Some(organisationType),
                          groupError = None
              )
            )
          )
        )
        authorizedUser()
        mockRegistrationFind(registration)
        mockRegistrationUpdate()
      case _ => None
    }

}
