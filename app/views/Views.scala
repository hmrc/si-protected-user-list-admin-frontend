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

package views

import javax.inject.Inject

class Views @Inject() (
  val home: views.html.Home,
  val add: views.html.add,
  val edit: views.html.edit,
  val editSuccess: views.html.edit_success,
  val view: views.html.view_entry,
  val deleteConfirmation: views.html.delete_entry_confirmation,
  val deleteSuccess: views.html.delete_entry_success,
  val errorTemplate: views.html.error_template,
  val somethingWentWrong: views.html.something_went_wrong
)
