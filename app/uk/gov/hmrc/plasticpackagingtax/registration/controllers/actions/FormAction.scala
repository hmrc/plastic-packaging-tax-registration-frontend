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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions

import play.api.mvc.{AnyContent, Request}

sealed trait FormAction

object FormAction {
  private val saveAndContinueLabel      = "SaveAndContinue"
  private val saveAndComeBackLaterLabel = "SaveAndComeBackLater"
  private val continueLabel             = "Continue"

  def bindFromRequest()(implicit request: Request[AnyContent]): FormAction =
    request.body.asFormUrlEncoded.flatMap { body =>
      body.flatMap {
        case (`saveAndContinueLabel`, _)      => Some(SaveAndContinue)
        case (`saveAndComeBackLaterLabel`, _) => Some(SaveAndComeBackLater)
        case (`continueLabel`, _)             => Some(Continue)
        case _                                => None
      }.headOption
    }.getOrElse(Unknown)

}

case object Unknown              extends FormAction
case object SaveAndContinue      extends FormAction
case object SaveAndComeBackLater extends FormAction
case object Continue             extends FormAction
