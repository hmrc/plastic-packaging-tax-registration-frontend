package uk.gov.hmrc.plasticpackagingtax.registration.controllers.deregistration

import base.unit.{ControllerSpec, MockDeregistrationDetailRepository}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.OK
import play.api.test.Helpers.{await, contentAsString, status}
import play.twirl.api.HtmlFormat
import spec.PptTestData
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.deregistration.deregistration_submitted_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class DeregistrationSubmittedControllerSpec
    extends ControllerSpec with MockDeregistrationDetailRepository with PptTestData {

  private val mcc      = stubMessagesControllerComponents()
  private val mockPage = mock[deregistration_submitted_page]
  when(mockPage.apply()(any(), any())).thenReturn(HtmlFormat.raw("Deregistration Submitted"))

  private val deregistrationSubmittedController =
    new DeregistrationSubmittedController(mockAuthAllowEnrolmentAction, mcc, mockPage)

  "Deregistration Submitted Controller" should {
    "display page" when {
      "user authenticated" in {
        authorizedUser()

        val resp = deregistrationSubmittedController.displayPage()(getRequest())

        status(resp) mustBe OK
        contentAsString(resp) mustBe "Deregistration Submitted"
      }
    }

    "throw exception" when {
      "user not authenticated" in {
        unAuthorizedUser()

        intercept[RuntimeException] {
          await(deregistrationSubmittedController.displayPage()(getRequest()))
        }
      }
    }
  }
}
