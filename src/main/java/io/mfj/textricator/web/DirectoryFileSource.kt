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

import java.io.File
import java.io.InputStream

class DirectoryFileSource( private val dir:File ): FileSource {

  private val dirPath = dir.absoluteFile.canonicalFile.toPath()

  override fun get(fileId:String):InputStream {
    validate(fileId)
    return File( dir, fileId ).inputStream()
  }

  private fun validate( fileId:String ) {
    // make sure [fileId] is not trying to get outside of [dir].
    // make sure [fileId] is a direct child of [dir].
    val path = File( dir, fileId ).absoluteFile.canonicalFile.toPath()
    if ( path.parent != dirPath ) {
      throw IllegalArgumentException( "\"${fileId}\" is not valid" )
    }
  }

  override fun list():List<String> =
      dir.listFiles()
          .filter( File::isFile )
          .map( File::getName )

}