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

package controllers

import base.unit.ControllerSpec
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, same}
import org.mockito.Mockito._
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.OK
import play.api.mvc.Call
import play.api.test.Helpers.{await, contentAsString, status}
import play.twirl.api.HtmlFormat
import controllers.liability.{routes => liabilityRoutes}
import forms.liability.RegType
import models.registration.{LiabilityDetails, Registration}
import views.html.{task_list_group, task_list_partnership, task_list_single_entity}
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class TaskListControllerSpec extends ControllerSpec {

  private val mcc              = stubMessagesControllerComponents()
  private val singleEntityPage = mock[task_list_single_entity]
  private val groupPage        = mock[task_list_group]
  private val partnershipPage  = mock[task_list_partnership]

  private val controller = new TaskListController(
    authenticate = mockAuthAction,
    mockJourneyAction,
    mcc = mcc,
    singleEntityPage = singleEntityPage,
    groupPage = groupPage,
    partnershipPage = partnershipPage,
    mockRegistrationConnector
  )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(singleEntityPage.apply(any(), any(), any())(any(), any())).thenReturn(
      HtmlFormat.raw("Single Entity Page")
    )
    when(groupPage.apply(any(), any(), any())(any(), any())).thenReturn(HtmlFormat.raw("Group Page"))
    when(partnershipPage.apply(any(), any(), any())(any(), any())).thenReturn(
      HtmlFormat.raw("Partnership Page")
    )
  }

  override protected def afterEach(): Unit = {
    reset(singleEntityPage, mockRegistrationConnector, mockSubscriptionsConnector)
    super.afterEach()
  }

  "Registration Controller" should {

    "return 200" when {
      "display registration page" when {

        "a 'businessPartnerId' exists" in {
          authorizedUser()
          mockRegistrationFind(
            aRegistration(
              withOrganisationDetails(organisationDetails =
                registeredUkCompanyOrgDetails()
              )
            )
          )

          val result = controller.displayPage()(getRequest())

          status(result) mustBe OK
          contentAsString(result) mustBe "Single Entity Page"
        }

        "a 'businessPartnerId' does not exist" in {
          authorizedUser()
          val result = controller.displayPage()(getRequest())

          status(result) mustBe OK
          contentAsString(result) mustBe "Single Entity Page"

          verifyNoInteractions(mockSubscriptionsConnector)
        }

        "a group registration is being performed" in {
          authorizedUser()
          mockRegistrationFind(aRegistration().copy(registrationType = Some(RegType.GROUP)))
          val result = controller.displayPage()(getRequest())

          status(result) mustBe OK
          contentAsString(result) mustBe "Group Page"
        }

        "show partnership task-list when a partnership with partners registration is being preformed" in {
          authorizedUser()
          mockRegistrationFind(
            aRegistration(withPartnershipDetails(Some(generalPartnershipDetails)))
          )

          val result = controller.displayPage()(getRequest())

          status(result) mustBe OK
          contentAsString(result) mustBe "Partnership Page"
        }

        "show single entity task-list when an LLP registration is being preformed" in {
          authorizedUser()
          mockRegistrationFind(aRegistration(withPartnershipDetails(Some(llpPartnershipDetails))))

          val result = controller.displayPage()(getRequest())

          status(result) mustBe OK
          contentAsString(result) mustBe "Single Entity Page"
        }
      }
    }
    
    "old liability answers should be removed" in {
      authorizedUser()
      val registration = mock[Registration]
      mockRegistrationFind(registration)
      when(registration.hasOldLiabilityQuestions).thenReturn(true)
      when(registration.organisationDetails).thenReturn(aRegistration().organisationDetails)
      
      val newRegistration = mock[Registration]
      when(registration.clearOldLiabilityAnswers).thenReturn(newRegistration)
      
      await(controller.displayPage()(getRequest()))
      verify(registration).clearOldLiabilityAnswers
      verify(mockRegistrationConnector).update(same(newRegistration))(any())
    }
    
    "old liability answers should not be removed" in {
      authorizedUser()
      val registration = mock[Registration]
      mockRegistrationFind(registration)
      when(registration.hasOldLiabilityQuestions).thenReturn(false)
      when(registration.organisationDetails).thenReturn(aRegistration().organisationDetails)
      
      await(controller.displayPage()(getRequest()))
      verify(registration, never()).clearOldLiabilityAnswers
      verify(mockRegistrationConnector, never()).update(any())(any())
    }

    "set liability start links" in {
      val partialLiabilityDetails =
        aRegistration(withLiabilityDetails(
          LiabilityDetails(
            newLiabilityStarted = None,
            newLiabilityFinished = None
          ))
        )

      mockRegistrationFind(partialLiabilityDetails)
      authorizedUser()

      val result = controller.displayPage()(getRequest())

      status(result) mustBe OK
      verifyStartLink(liabilityRoutes.ExpectToExceedThresholdWeightController.displayPage().url)
    }

    "liability redirect to check your answer page when task is completed" in {

      mockRegistrationFind(aRegistration())
      authorizedUser()

      val result = controller.displayPage()(getRequest())

      status(result) mustBe OK
      verifyStartLink(liabilityRoutes.CheckLiabilityDetailsAnswersController.displayPage().url)

    }

    "return error" when {
      "user is not authorised" in {
        unAuthorizedUser()
        mockUkCompanyCreateIncorpJourneyId("http://test/redirect/uk-company")
        mockGetSubscriptionStatusFailure(new RuntimeException("error"))
        val result = controller.displayPage()(getRequest())

        intercept[RuntimeException](status(result))
      }
    }

    def verifyStartLink(startLink: String): Unit = {
      val startLinkCaptor: ArgumentCaptor[Call] = ArgumentCaptor.forClass(classOf[Call])
      verify(singleEntityPage).apply(any(), startLinkCaptor.capture(), any())(any(), any())
      startLinkCaptor.getValue.url mustBe startLink
    }
  }
}