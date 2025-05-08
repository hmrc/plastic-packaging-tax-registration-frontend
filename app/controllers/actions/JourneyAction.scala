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

package controllers.actions

import com.google.inject.ImplementedBy
import controllers.actions.auth.{AmendAuthAction, RegistrationAuthAction}
import controllers.actions.getRegistration.{GetRegistrationAction, GetRegistrationForAmendmentAction}
import models.request.JourneyRequest
import play.api.mvc.{ActionBuilder, AnyContent}

import javax.inject.Inject

@ImplementedBy(classOf[JourneyActionImpl])
trait JourneyAction {
  def register: ActionBuilder[JourneyRequest, AnyContent]
  def amend: ActionBuilder[JourneyRequest, AnyContent]
}

class JourneyActionImpl @Inject() (
  registrationAuth: RegistrationAuthAction,
  getRegistrationAction: GetRegistrationAction,
  amendAuthAction: AmendAuthAction,
  getRegistrationForAmendmentAction: GetRegistrationForAmendmentAction
) extends JourneyAction {

  override def register: ActionBuilder[JourneyRequest, AnyContent] =
    registrationAuth andThen getRegistrationAction

  override def amend: ActionBuilder[JourneyRequest, AnyContent] =
    amendAuthAction andThen getRegistrationForAmendmentAction

}
