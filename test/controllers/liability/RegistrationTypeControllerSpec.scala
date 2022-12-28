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

package controllers.liability

import base.unit.ControllerSpec
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.data.Form
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.mvc.Call
import play.api.test.Helpers.{redirectLocation, status}
import play.twirl.api.Html
import forms.liability.RegType.GROUP
import forms.liability.RegistrationType
import views.html.liability.registration_type_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class RegistrationTypeControllerSpec extends ControllerSpec {

  private val mcc      = stubMessagesControllerComponents()
  private val mockPage = mock[registration_type_page]

  private val registrationTypeController = new RegistrationTypeController(mockAuthAction,
                                                                          mockJourneyAction,
                                                                          mockRegistrationConnector,
                                                                          mcc,
                                                                          mockPage
  )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(mockPage.apply(any[Form[RegistrationType]], any())(any(), any())).thenReturn(
      Html("Registration Type Page")
    )
  }

  override protected def afterEach(): Unit = {
    super.afterEach()
    reset(mockRegistrationConnector, mockPage)
  }

  "Registration Type Controller" should {

    authorizedUser()

    "display page" when {

      "no existing registration type" in {
        mockRegistrationFind(aRegistration(withRegistrationType(None)))

        val result = registrationTypeController.displayPage()(getRequest())

        status(result) mustBe OK

        val captor: ArgumentCaptor[Form[RegistrationType]] =
          ArgumentCaptor.forClass(classOf[Form[RegistrationType]])
        verify(mockPage).apply(captor.capture(), any())(any(), any())
        captor.getValue.value mustBe None
      }

      "registration type previously supplied" in {
        mockRegistrationFind(aRegistration(withRegistrationType(Some(GROUP))))

        val result = registrationTypeController.displayPage()(getRequest())

        status(result) mustBe OK

        val captor: ArgumentCaptor[Form[RegistrationType]] =
          ArgumentCaptor.forClass(classOf[Form[RegistrationType]])
        verify(mockPage).apply(captor.capture(), any())(any(), any())
        captor.getValue.value mustBe Some(RegistrationType(Some(GROUP)))
      }

    }

    "display page with appropriate backlink" in {
      authorizedUser()
      verifyExpectedBackLink(routes.LiabilityWeightController.displayPage().url)
    }

    "update registration" when {

      "group selected" in {
        val initialRegistration = aRegistration()
        mockRegistrationFind(initialRegistration)
        mockRegistrationUpdate()

        val groupSelection = Seq("value" -> GROUP.toString, saveAndContinueFormAction)
        val result         = registrationTypeController.submit()(postJsonRequestEncoded(groupSelection: _*))

        redirectLocation(result) mustBe Some(
          routes.MembersUnderGroupControlController.displayPage().url
        )

        val expectedRegistration = initialRegistration.copy(registrationType = Some(GROUP))
        verify(mockRegistrationConnector).update(eqTo(expectedRegistration))(any())
      }
    }

    "display validation error" when {
      "no selection made" in {
        val initialRegistration = aRegistration()
        mockRegistrationFind(initialRegistration)
        mockRegistrationUpdate()

        val emptySelection = Seq()
        val result         = registrationTypeController.submit()(postJsonRequestEncoded(emptySelection: _*))

        status(result) mustBe BAD_REQUEST
      }
    }
  }

  private def verifyExpectedBackLink(backLink: String): Unit = {
    mockRegistrationFind(aRegistration(withRegistrationType(None)))

    val result = registrationTypeController.displayPage()(getRequest())

    status(result) mustBe OK

    val captor: ArgumentCaptor[Call] = ArgumentCaptor.forClass(classOf[Call])
    verify(mockPage).apply(any(), captor.capture())(any(), any())
    captor.getValue.url mustBe backLink
  }

}
