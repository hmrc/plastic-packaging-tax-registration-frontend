/*
 * Copyright 2026 HM Revenue & Customs
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

package audit

import play.api.libs.json.{Format, JsError, JsString, JsSuccess, Reads, Writes}

import scala.util.Try

enum UserType:
  case NEW, RESUME

object UserType:
  given Format[UserType] =
    Format(
      Reads {
        case JsString(value) =>
          Try(UserType.valueOf(value))
            .map(JsSuccess(_))
            .getOrElse(JsError(s"Unknown UserType: $value"))
        case _ =>
          JsError("String value expected")
      },
      Writes(userType => JsString(userType.toString))
    )
