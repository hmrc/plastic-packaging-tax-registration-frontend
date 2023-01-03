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

package forms.mappings

import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.i18n.Messages
import config.AppConfig

import java.time.format.{DateTimeFormatter, ResolverStyle}
import java.time.{Clock, LocalDate}
import scala.util.Try

trait Constraints {

  private def isDateInFuture(clock: Clock): LocalDate => Boolean =
    date => date.isAfter(LocalDate.now(clock))

  private def isDateBeforeLiveDate(appConfig: AppConfig): LocalDate => Boolean =
    date => date.isBefore(appConfig.goLiveDate)

  private def getMonth(appConfig: AppConfig)(implicit messages: Messages) =
    messages(s"date.month.${appConfig.goLiveDate.getMonthValue}")

  def isInDateRange(errorKey: String, beforeLiveDateErrorKey: String)(implicit
    appConfig: AppConfig,
    clock: Clock,
    messages: Messages
  ): Constraint[LocalDate] = {
    val goLiveDate = appConfig.goLiveDate
    Constraint {
      case request if isDateBeforeLiveDate(appConfig).apply(request) =>
        Invalid(
          ValidationError(
            messages(beforeLiveDateErrorKey,
                     s"${goLiveDate.getDayOfMonth} ${getMonth(appConfig)} ${goLiveDate.getYear}"
            )
          )
        )
      case request if isDateInFuture(clock).apply(request) =>
        Invalid(ValidationError(errorKey))
      case _ => Valid
    }
  }

  protected def firstError[A](constraints: Constraint[A]*): Constraint[A] =
    Constraint {
      input =>
        constraints
          .map(_.apply(input))
          .find(_ != Valid)
          .getOrElse(Valid)
    }

  protected def nonEmptyDate(errKey: String, args: Seq[String] = Seq()): Constraint[(String, String, String)] = Constraint {
    case (_, _, "") | ("", _, _) | (_, "", _) => Invalid(errKey, args: _*)
    case _ => Valid
  }

  protected def validDate(errKey: String, args: Seq[String] = Seq()): Constraint[(String, String, String)] = Constraint {
    input: (String, String, String) =>
      val date = Try {
        tupleToDate(input)
      }.toOption
      date match {
        case Some(_) => Valid
        case None => Invalid(errKey, args: _*)
      }
  }

  private def tupleToDate(dateTuple: (String, String, String)) = {
    LocalDate.parse(s"${dateTuple._1}-${dateTuple._2}-${dateTuple._3}", DateTimeFormatter.ofPattern("d-M-uuuu").withResolverStyle(ResolverStyle.STRICT))
  }

}
