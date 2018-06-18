/*
This file is part of TextricatorWeb.
Copyright 2018 Measures for Justice Institute.

This program is free software: you can redistribute it and/or modify it under
the terms of the GNU Affero General Public License version 3 as published by the
Free Software Foundation.

This program is distributed in the hope that it will be useful, but WITHOUT ANY
WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License along
with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package io.mfj.textricator.web

import java.io.InputStream

object ResourcesFileSource: FileSource {

  private val fileIds = listOf( "school-employee-list.pdf" )

  override fun get(fileId:String):InputStream {
    if ( ! fileIds.contains( fileId ) ) {
      throw IllegalArgumentException( "\"${fileId}\" is invalid" )
    }

    // The file is on the classpath.
    val key = "/files/${fileId.replace(Regex("/"),"")}"

    try {
      return Main::class.java.getResourceAsStream(key)
    } catch ( e:Exception ) {
      throw Exception( "Missing file: ${fileId}", e )
    }
  }

  override fun list():List<String> = fileIds

}