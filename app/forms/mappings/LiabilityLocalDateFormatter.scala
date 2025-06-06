/*
 * Copyright 2025 HM Revenue & Customs
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

package forms.mappings

import config.AppConfig
import play.api.data.FormError
import play.api.data.format.Formatter
import play.api.i18n.Messages

import java.time.LocalDate

private[mappings] class LiabilityLocalDateFormatter(
  emptyDateKey: String,
  singleRequiredKey: String,
  twoRequiredKey: String,
  invalidKey: String,
  dateOutOfRangeError: String,
  beforeLiveDateErrorKey: String,
  appConfig: AppConfig,
  args: Seq[String] = Seq.empty
)(implicit messages: Messages)
    extends Formatter[LocalDate]
    with Formatters {

  private val dateFormatter = new LocalDateFormatter(emptyDateKey, singleRequiredKey, twoRequiredKey, invalidKey, args)

  override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], LocalDate] =
    dateFormatter.bind(key, stripWhiteSpaces(data)).fold(error => Left(error), date => validateDate(key, date))

  private def stripWhiteSpaces(data: Map[String, String]) =
    data.map(o => o._1 -> o._2.replace(" ", ""))

  private def validateDate(key: String, date: LocalDate): Either[Seq[FormError], LocalDate] =
    if (date.isAfter(LocalDate.now))
      Left(List(FormError(s"$key.day", dateOutOfRangeError, Seq(messages(s"general.day")))))
    else if (date.isBefore(appConfig.goLiveDate))
      Left(List(FormError(s"$key.day", beforeLiveDateErrorKey, Seq(goLiveDateArgs))))
    else
      Right(date)

  private def goLiveDateArgs = {
    val goLiveDate = appConfig.goLiveDate
    s"${goLiveDate.getDayOfMonth} ${messages(s"date.month.${goLiveDate.getMonthValue}")} ${goLiveDate.getYear}"
  }

  override def unbind(key: String, value: LocalDate): Map[String, String] = dateFormatter.unbind(key, value)
}
