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
import org.scalatest.Ignore
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.json.JsObject
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, contentAsString, redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.plasticpackagingtax.registration.models.enrolment.PptEnrolment
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.amendment.{routes => amendRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Registration
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.AmendmentJourneyAction
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.amendment.group.amend_member_contact_check_answers_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import utils.FakeRequestCSRFSupport.CSRFFakeRequest

class AddGroupMemberContactDetailsCheckAnswersControllerSpec
    extends ControllerSpec with MockAmendmentJourneyAction {

  private val mcc     = stubMessagesControllerComponents()
  private val cyaPage = mock[amend_member_contact_check_answers_page]

  when(cyaPage.apply(any(), any(), any())(any(), any())).thenReturn(
    HtmlFormat.raw("Amend Reg - New Group Member CYA")
  )

  private val pptReference = "XMPPT0000000123"

  private def authorisedUserWithPptSubscription(): Unit =
    authorizedUser(user =
      newUser().copy(enrolments =
        Enrolments(
          Set(
            new Enrolment(PptEnrolment.Identifier,
                          Seq(EnrolmentIdentifier(PptEnrolment.Key, pptReference)),
                          "activated"
            )
          )
        )
      )
    )

  private val controller = new AddGroupMemberContactDetailsCheckAnswersController(
    authenticate = mockAuthAllowEnrolmentAction,
    journeyAction = mockAmendmentJourneyAction,
    mcc = mcc,
    page = cyaPage
  )

  private val groupRegistrationInAmendment = aRegistration(
    withGroupDetail(Some(groupDetailsWithMembers))
  )

  override protected def beforeEach(): Unit = {
    inMemoryRegistrationAmendmentRepository.reset()
    reset(mockSubscriptionConnector)
    simulateGetSubscriptionSuccess(groupRegistrationInAmendment)
    simulateUpdateSubscriptionSuccess()
  }

  "AddGroupMemberContactDetailsCheckAnswersController" when {

    "displaying page" should {
      "display successfully" when {
        "group member present" in {
          authorisedUserWithPptSubscription()
          val mostRecentGroupMember = groupRegistrationInAmendment.groupDetail.get.members.last

          val resp = controller.displayPage(mostRecentGroupMember.id)(getRequest())

          status(resp) mustBe OK
          contentAsString(resp) mustBe "Amend Reg - New Group Member CYA"
        }
      }
      "throw IllegalStateException" when {
        "group member is absent" in {
          authorisedUserWithPptSubscription()

          intercept[IllegalStateException] {
            await(controller.displayPage("XXX")(getRequest()))
          }
        }
      }
    }

    "details confirmed" should {
      "update subscription at the ETMP back end" in {
        authorisedUserWithPptSubscription()

        await(controller.submit()(postRequest(JsObject.empty)))

        val registrationCaptor: ArgumentCaptor[Registration] =
          ArgumentCaptor.forClass(classOf[Registration])
        verify(mockSubscriptionConnector).updateSubscription(any(), registrationCaptor.capture())(
          any()
        )

        registrationCaptor.getValue mustBe groupRegistrationInAmendment
      }

      "redirect to the manage group page when update successful" in {
        authorisedUserWithPptSubscription()

        val resp = controller.submit()(postRequest(JsObject.empty))

        status(resp) mustBe SEE_OTHER
        redirectLocation(resp) mustBe Some(
          routes.ManageGroupMembersController.displayPage().toString
        )
      }
      "redirect to the post reg amend error page" when {
        // TODO : Reinstate when we can work out why this test does not work. There's something amiss with our use of
        //        mocks and futures!
        "update fails due to exception being thrown" ignore {
          authorisedUserWithPptSubscription()
          simulateUpdateSubscriptionFailure(new RuntimeException("BANG!"))

          val resp = controller.submit()(postRequest(JsObject.empty))

          status(resp) mustBe SEE_OTHER
          redirectLocation(resp) mustBe Some(
            amendRoutes.AmendRegistrationController.registrationUpdateFailed().toString
          )
        }
        "update fails due to error returned from ETMP" in {
          authorisedUserWithPptSubscription()
          simulateUpdateSubscriptionFailureReturnedError()

          val resp = controller.submit()(postRequest(JsObject.empty))

          status(resp) mustBe SEE_OTHER
          redirectLocation(resp) mustBe Some(
            amendRoutes.AmendRegistrationController.registrationUpdateFailed().toString
          )
        }
      }
    }
  }

  private def getRequest(): Request[AnyContentAsEmpty.type] =
    FakeRequest("GET", "").withSession((AmendmentJourneyAction.SessionId, "123")).withCSRFToken

}
