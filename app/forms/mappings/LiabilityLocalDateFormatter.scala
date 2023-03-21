package forms.mappings

import play.api.data.FormError
import play.api.data.format.Formatter
import play.api.i18n.Messages

import java.time.LocalDate

private[mappings] class LiabilityLocalDateFormatter
(
  emptyDateKey: String,
  singleRequiredKey: String,
  twoRequiredKey: String,
  invalidKey: String,
  dateOutOfRangeError: String,
  args: Seq[String] = Seq.empty
)(implicit messages: Messages) extends Formatter[LocalDate] with Formatters {

  private val dateFormatter  = new LocalDateFormatter(emptyDateKey, singleRequiredKey, twoRequiredKey, invalidKey, args)
  override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], LocalDate] = {

   // if(date.isAfter(LocalDate.now(clock))

    dateFormatter.bind(key, data).fold(
      error => Left(error),
      date => validateDate(date)
    )
  }

  override def unbind(key: String, value: LocalDate): Map[String, String] = ???

  private def isInTheFuture(date: LocalDate) = {
    date.isAfter(LocalDate.now)
  }
  private def validateDate(date: LocalDate): Either[Seq[FormError], LocalDate] = {

    if(date.isAfter(LocalDate.))

  }
}
