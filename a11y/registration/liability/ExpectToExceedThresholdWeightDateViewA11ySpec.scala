package registration.liability

import config.AppConfig
import forms.liability.{ExpectToExceedThresholdWeightDate, LiabilityWeight}
import play.api.data.Form
import support.BaseViewSpec
import views.html.liability.expect_to_exceed_threshold_weight_date_page

import java.time.{Clock, Instant, LocalDate}
import java.util.TimeZone

class ExpectToExceedThresholdWeightDateViewA11ySpec extends BaseViewSpec {

  private val fakeClock = {
    Clock.fixed(Instant.parse("2022-06-01T12:00:00Z"), TimeZone.getDefault.toZoneId)
  }

  private val page = inject[expect_to_exceed_threshold_weight_date_page]
  private val formProvider = new ExpectToExceedThresholdWeightDate(appConfig, fakeClock)
  private val form = formProvider()(messages)

  private def render(form: Form[LocalDate] = form) : String =
    page(form)(journeyRequest, messages).toString()

  "view" should {
    "pass accessibility checks without error" in {
      render() must passAccessibilityChecks
    }

    "pass accessibility checks with error" in {
      val bindForm = form.bind(Map("expect-to-exceed-threshold-weight-date"-> "") )

      render(bindForm) must passAccessibilityChecks
    }
  }

}
