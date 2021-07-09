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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers

import base.unit.ControllerSpec
import org.mockito.ArgumentMatchers.{any, refEq}
import org.mockito.BDDMockito.`given`
import org.mockito.Mockito.{reset, verify}
import org.mockito.{ArgumentMatchers, Mockito}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.json.JsObject
import play.api.test.Helpers.{await, redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.DownstreamServiceError
import uk.gov.hmrc.plasticpackagingtax.registration.forms.OrgType.{SOLE_TRADER, UK_COMPANY}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.{
  Address,
  FullName,
  LiabilityWeight,
  OrgType
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{
  LiabilityDetails,
  MetaData,
  OrganisationDetails,
  PrimaryContactDetails
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.review_registration_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class ReviewRegistrationControllerSpec extends ControllerSpec {
  private val page = mock[review_registration_page]
  private val mcc  = stubMessagesControllerComponents()

  private val controller =
    new ReviewRegistrationController(authenticate = mockAuthAction,
                                     mockJourneyAction,
                                     mcc = mcc,
                                     incorpIdConnector = mockIncorpIdConnector,
                                     registrationConnector = mockRegistrationConnector,
                                     soleTraderInorpIdConnector = mockSoleTraderConnector,
                                     subscriptionsConnector = mockSubscriptionsConnector,
                                     auditor = mockAuditor,
                                     page = page,
                                     metrics = metricsMock
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    val registration = aRegistration()

    mockRegistrationFind(registration)
    given(page.apply(any(), any(), any())(any(), any())).willReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(page)
    super.afterEach()
  }

  "Review registration controller" should {

    "return 200" when {

      "user is authorised and display page method is invoked with uk company" in {
        val registration = aRegistration(
          withOrganisationDetails(OrganisationDetails(organisationType = Some(UK_COMPANY)))
        )
        authorizedUser()
        mockRegistrationFind(registration)
        mockRegistrationUpdate(registration)
        mockGetUkCompanyDetails(incorporationDetails)

        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
      }

      "user is authorised and display page method is invoked with sole trader" in {
        val registration = aRegistration(
          withOrganisationDetails(OrganisationDetails(organisationType = Some(SOLE_TRADER)))
        )
        authorizedUser()
        mockRegistrationFind(registration)
        mockRegistrationUpdate(registration)
        mockGetSoleTraderDetails(soleTraderIncorporationDetails)

        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
      }

    }

    "return 303" when {

      "Journey ID is invalid or does not exist" in {
        authorizedUser()

        val registration = aRegistration(withIncorpJourneyId(None))
        mockRegistrationFind(registration)
        given(
          page.apply(refEq(registration),
                     refEq(Some(incorporationDetails)),
                     refEq(Some(soleTraderIncorporationDetails))
          )(any(), any())
        ).willReturn(HtmlFormat.empty)

        val result = controller.displayPage()(getRequest())

        status(result) mustBe SEE_OTHER
      }

      "get details fails for uk company" in {
        val registration = aRegistration(
          withOrganisationDetails(OrganisationDetails(organisationType = Some(UK_COMPANY)))
        )
        authorizedUser()
        mockRegistrationFind(registration)
        mockGetUkCompanyDetailsFailure(new RuntimeException("error"))

        val result = controller.displayPage()(getRequest())

        intercept[RuntimeException](status(result))
      }

      "get details fails for sole trader" in {
        val registration = aRegistration(
          withOrganisationDetails(OrganisationDetails(organisationType = Some(SOLE_TRADER)))
        )
        authorizedUser()
        mockRegistrationFind(registration)
        mockGetSoleTraderDetailsFailure(new RuntimeException("error"))

        val result = controller.displayPage()(getRequest())

        intercept[RuntimeException](status(result))
      }

      "when form is submitted" in {
        authorizedUser()
        mockRegistrationFind(aCompleteRegistration)
        mockRegistrationUpdate(
          aCompleteRegistration.copy(metaData =
            MetaData(registrationReviewed = true, registrationCompleted = true)
          )
        )
        mockSubscriptionSubmit(subscriptionCreate)

        val result = controller.submit()(postRequest(JsObject.empty))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.ConfirmationController.displayPage().url)
        metricsMock.defaultRegistry.counter(
          "ppt.registration.success.submission.counter"
        ).getCount mustBe 1
      }
    }

    "send audit event" when {
      "submission of audit event" in {
        authorizedUser()
        mockRegistrationFind(aCompleteRegistration)
        val aReviewedRegistration = aCompleteRegistration.copy(metaData =
          MetaData(registrationReviewed = true, registrationCompleted = true)
        )
        mockRegistrationUpdate(aReviewedRegistration)
        mockSubscriptionSubmit(subscriptionCreate)

        await(controller.submit()(postRequest(JsObject.empty)))

        verify(mockAuditor, Mockito.atLeast(1)).registrationSubmitted(
          ArgumentMatchers.eq(aReviewedRegistration)
        )(any(), any())
      }
    }

    "return an error" when {

      "user is not authorised" in {
        unAuthorizedUser()
        val result = controller.displayPage()(getRequest())

        intercept[RuntimeException](status(result))
      }

      "user submits form and the registration update fails" in {
        authorizedUser()
        mockRegistrationFailure()
        val result =
          controller.submit()(postRequest(JsObject.empty))

        intercept[DownstreamServiceError](status(result))
        metricsMock.defaultRegistry.counter(
          "ppt.registration.failed.submission.counter"
        ).getCount mustBe 1
      }

    }
  }

  private def aCompleteRegistration =
    aRegistration(withOrganisationDetails(registeredUkOrgDetails(OrgType.UK_COMPANY)),
                  withLiabilityDetails(
                    LiabilityDetails(weight = Some(LiabilityWeight(Some(1000))), startDate = None)
                  ),
                  withPrimaryContactDetails(
                    PrimaryContactDetails(fullName = Some(FullName("Jack", "Gatsby")),
                                          jobTitle = Some("Developer"),
                                          phoneNumber = Some("0203 4567 890"),
                                          email = Some("test@test.com"),
                                          address = Some(
                                            Address(addressLine1 = "2 Scala Street",
                                                    addressLine2 = Some("Soho"),
                                                    townOrCity = "London",
                                                    postCode = "W1T 2HN"
                                            )
                                          )
                    )
                  ),
                  withMetaData(MetaData(registrationCompleted = true))
    )

}
