package forms.mappings

import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar.when
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.data.FormError
import play.api.i18n.Messages

import java.time.LocalDate

class LocalDateFormatterSpec extends PlaySpec with TableDrivenPropertyChecks{

  private val message = mock[Messages]
  private  val formatter = new LocalDateFormatter(
    "emptyDateKey",
    "singleRequiredKey",
    "twoRequiredKey",
    "invalidKey"
  )(message)

  "bind" should {
    "return a local date" in {
      val result = formatter.bind("input", Map("input.day" -> "4", "input.month" -> "5", "input.year" -> "2022"))

      result mustBe Right(LocalDate.of(2022, 5, 4))
    }

    "return an error" when {

      val table1 = Table(
        ("description", "partKey", "day", "month", "year", "key"),
        ("day", "day", None, Some(5), Some(2022), "singleRequiredKey"),
        ("month", "month", Some(5), None, Some(2022), "singleRequiredKey"),
        ("year", "year", Some(5), Some(5), None, "singleRequiredKey"),
        ("day and month", "day", None, None, Some(2022), "twoRequiredKey"),
        ("day and year", "day", None, Some(5), None, "twoRequiredKey"),
        ("month and year", "month", Some(5), None, None, "twoRequiredKey")
      )

      forAll(table1) {
        (
          description: String,
          partKey: String,
          day: Option[Int],
          month: Option[Int],
          year: Option[Int],
          keyMessage: String
        ) =>
          s"$description is missing" in {

            val data = Map("input.day" -> day, "input.month" -> month, "input.year" -> year)
              .filterNot(o => o._2 == None)
              .map(o => o._1 -> o._2.get.toString)

            when(message.apply(anyString(), any)).thenReturn("message")

            val result = formatter.bind("input", data)

            result mustBe Left(Seq(FormError(s"input.$partKey", keyMessage, Seq("message"))))
          }
      }
    }

    s"day, month and year are missing" in {
      val result = formatter.bind("input", Map.empty)

      result mustBe Left(Seq(FormError(s"input.day", "emptyDateKey", Seq.empty)))
    }

  }
}
