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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.amendment.group

import base.PptTestData.newUser
import base.unit.{ControllerSpec, MockAmendmentJourneyAction}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.inject.NewInstanceInjector.instanceOf
import play.api.mvc.{AnyContent, AnyContentAsEmpty, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, contentAsString, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact._
import uk.gov.hmrc.plasticpackagingtax.registration.forms.group.MemberName
import uk.gov.hmrc.plasticpackagingtax.registration.models.enrolment.PptEnrolment
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Registration
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.AmendmentJourneyAction
import uk.gov.hmrc.plasticpackagingtax.registration.services.CountryService
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.contact._
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.group.{
  member_email_address_page,
  member_name_page,
  member_phone_number_page
}
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import utils.FakeRequestCSRFSupport.CSRFFakeRequest

import scala.concurrent.Future

class AmendMemberContactDetailsControllerSpec
    extends ControllerSpec with MockAmendmentJourneyAction with TableDrivenPropertyChecks {

  private val countryService = instanceOf[CountryService]

  private val mcc = stubMessagesControllerComponents()

  private val amendNamePage         = mock[member_name_page]
  private val amendPhoneNumberPage  = mock[member_phone_number_page]
  private val amendEmailAddressPage = mock[member_email_address_page]
  private val amendAddressPage      = mock[address_page]

  when(amendNamePage.apply(any(), any(), any(), any(), any())(any(), any())).thenReturn(
    HtmlFormat.raw("name amendment")
  )

  when(amendEmailAddressPage.apply(any(), any(), any(), any())(any(), any())).thenReturn(
    HtmlFormat.raw("email address amendment")
  )

  when(amendPhoneNumberPage.apply(any(), any(), any(), any())(any(), any())).thenReturn(
    HtmlFormat.raw("phone number amendment")
  )

  when(amendAddressPage.apply(any(), any(), any(), any())(any(), any())).thenReturn(
    HtmlFormat.raw("address amendment")
  )

  private val controller =
    new AmendMemberContactDetailsController(mockAuthAllowEnrolmentAction,
                                            mcc,
                                            mockAmendmentJourneyAction,
                                            amendNamePage,
                                            amendPhoneNumberPage,
                                            amendEmailAddressPage,
                                            amendAddressPage,
                                            countryService
    )

  private def authorisedUserWithPptSubscription() =
    authorizedUser(user =
      newUser().copy(enrolments =
        Enrolments(
          Set(
            new Enrolment(PptEnrolment.Identifier,
                          Seq(EnrolmentIdentifier(PptEnrolment.Key, "XMPPT0000000123")),
                          "activated"
            )
          )
        )
      )
    )

  private val populatedRegistration = aRegistration(
    withGroupDetail(groupDetail = Some(groupDetails.copy(members = Seq(groupMember))))
  )

  private val memberId = groupMember.id

  override protected def beforeEach(): Unit = {
    inMemoryRegistrationAmendmentRepository.reset()
    reset(mockSubscriptionConnector)
    simulateGetSubscriptionSuccess(populatedRegistration)
  }

  "Amend Member Contact Details Controller" should {

    "show page" when {
      val showPageTestData =
        Table(("Test Name", "Display Call", "Expected Page Content"),
              ("main contact name",
               (req: Request[AnyContent]) => controller.contactName(memberId)(req),
               "name amendment"
              ),
              ("member phone number",
               (req: Request[AnyContent]) => controller.phoneNumber(memberId)(req),
               "phone number amendment"
              ),
              ("member email address",
               (req: Request[AnyContent]) => controller.email(memberId)(req),
               "email address amendment"
              ),
              ("address",
               (req: Request[AnyContent]) => controller.address(memberId)(req),
               "address amendment"
              )
        )

      forAll(showPageTestData) {
        (
          testName: String,
          call: (Request[AnyContent]) => Future[Result],
          expectedContent: String
        ) =>
          s"$testName page requested and registration populated" in {
            authorisedUserWithPptSubscription()

            val resp = call(getRequest())

            status(resp) mustBe OK
            contentAsString(resp) mustBe expectedContent
          }

          s"$testName page requested and registration unpopulated" in {
            simulateGetSubscriptionSuccess(Registration("123"))
            authorisedUserWithPptSubscription()

            val resp = call(getRequest())

            status(resp) mustBe OK
            contentAsString(resp) mustBe expectedContent
          }
      }
    }

    val invalidAddress = Address(addressLine1 = "", townOrCity = "", postCode = Some("XXX"))
    val validAddress = Address(addressLine1 = "2-3 Scala Street",
                               addressLine2 = Some("Soho"),
                               townOrCity = "London",
                               postCode = Some("E17 3RE")
    )
    val updateTestData =
      Table(
        ("TestName",
         "Invalid Form",
         "Valid Form",
         "Update Call",
         "Update Test",
         "Invalid Expected Page Content"
        ),
        ("main contact name",
         () => MemberName("", ""),
         () => MemberName("John", "Johnson"),
         (req: Request[AnyContent]) => controller.updateContactName(memberId)(req),
         (reg: Registration) =>
           reg.groupDetail.get.members.head.contactDetails.get.firstName mustBe "John",
         "name amendment"
        ),
        ("member phone number",
         () => PhoneNumber("xxx"),
         () => PhoneNumber("07123 123456"),
         (req: Request[AnyContent]) => controller.updatePhoneNumber(memberId)(req),
         (reg: Registration) =>
           reg.groupDetail.get.members.head.contactDetails.get.phoneNumber mustBe Some(
             "07123 123456"
           ),
         "phone number amendment"
        ),
        ("member email address",
         () => EmailAddress(""),
         () => EmailAddress("test@test.com"),
         (req: Request[AnyContent]) => controller.updateEmail(memberId)(req),
         (reg: Registration) =>
           reg.groupDetail.get.members.head.contactDetails.get.email mustBe Some("test@test.com"),
         "email address amendment"
        ),
        ("address",
         () => invalidAddress,
         () => validAddress,
         (req: Request[AnyContent]) => controller.updateAddress(memberId)(req),
         (reg: Registration) =>
           reg.groupDetail.get.members.head.contactDetails.get.address mustBe Some(validAddress),
         "address amendment"
        )
      )

    "redisplay page" when {
      forAll(updateTestData) {
        (
          testName: String,
          createInvalidForm: () => AnyRef,
          _,
          call: Request[AnyContent] => Future[Result],
          _,
          expectedPageContent
        ) =>
          s"supplied $testName fails validation" in {
            val registration = aRegistration()
            authorisedUserWithPptSubscription()
            inMemoryRegistrationAmendmentRepository.put("123", registration)

            val resp = call(postRequestEncoded(form = createInvalidForm(), sessionId = "123"))

            status(resp) mustBe BAD_REQUEST
            contentAsString(resp) mustBe expectedPageContent
          }
      }
    }

    "update registration and redirect to registration amendment page" when {
      forAll(updateTestData) {
        (
          testName: String,
          _,
          createValidForm: () => AnyRef,
          call: Request[AnyContent] => Future[Result],
          test: Registration => scalatest.Assertion,
          _
        ) =>
          s"$testName updated" in {
            val registration = aRegistration(
              withGroupDetail(groupDetail = Some(groupDetails.copy(members = Seq(groupMember))))
            )
            authorisedUserWithPptSubscription()
            inMemoryRegistrationAmendmentRepository.put("123", registration)

            await(call(postRequestEncoded(form = createValidForm(), sessionId = "123")))

            val registrationCaptor: ArgumentCaptor[Registration] =
              ArgumentCaptor.forClass(classOf[Registration])
            verify(mockSubscriptionConnector).updateSubscription(any(),
                                                                 registrationCaptor.capture()
            )(any())

            test(registrationCaptor.getValue)
          }
      }
    }
  }

  private def getRequest(): Request[AnyContentAsEmpty.type] =
    FakeRequest("GET", "").withSession((AmendmentJourneyAction.SessionId, "123")).withCSRFToken

}
