package uk.gov.hmrc.plasticpackagingtax.registration.controllers

import akka.http.scaladsl.model.StatusCodes.OK
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.test.FakeRequest
import play.api.test.Helpers.status
import play.twirl.api.HtmlFormat
import spec.ControllerSpec
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.start_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class StartControllerSpec extends ControllerSpec {

  private val fakeRequest = FakeRequest("GET", "/")
  private val mcc = stubMessagesControllerComponents()
  private val startPage: start_page = mock[start_page]
  private val controller = new StartController(mcc, startPage)

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(startPage.apply()(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(startPage)
    super.afterEach()
  }

  "Start Controller" should {

    "return 200" when {

      "display page method is invoked" in {

        val result = controller.displayStartPage()(fakeRequest)

        status(result) mustBe OK.intValue
      }
    }
  }
}
