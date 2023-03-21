package forms.mappings

import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar.when
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.data.FormError
import play.api.i18n.Messages

import java.time.LocalDate

class LiabilityLocalDateFormatterSpec extends PlaySpec {
  private val message = mock[Messages]
  private  val formatter = new LiabilityLocalDateFormatter(
    "emptyDateKey",
    "singleRequiredKey",
    "twoRequiredKey",
    "invalidKey",
    "dateOutOfRangeError"
  )(message)
  "bind" should {
    "return a date" in {

      val result = formatter.bind("input", Map("input.day" -> "4", "input.month" -> "5", "input.year" -> "2022"))

      result mustBe Right(LocalDate.of(2022, 5, 4))
    }

    "error" when {

      when(message.apply(anyString(), any)).thenReturn("message")

      val date = LocalDate.now.plusMonths(1)
      "date in the future" in {
        val result = formatter.bind("input",
          Map(
            "input.day" -> date.getDayOfMonth.toString,
            "input.month" -> date.getMonthValue.toString,
            "input.year" -> date.getYear.toString)
        )

        result mustBe Left(Seq(FormError(s"input.day", "dateOutOfRangeError", Seq(message))))
      }
    }
  }

}
