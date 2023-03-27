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
  val add: views.html.add,
  val deleteComplete: views.html.delete_complete,
  val deleteConfirmation: views.html.delete_confirmation,
  val deleteForm: views.html.delete_form,
  val errorTemplate: views.html.error_template,
  val fileUpload: views.html.file_upload,
  val fileUploadTime: views.html.file_upload_time,
  val home: views.html.home,
  val showAll: views.html.show_all,
  val showAllSorted: views.html.show_all_sorted
)
