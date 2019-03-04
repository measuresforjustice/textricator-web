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

import com.fasterxml.jackson.databind.ObjectMapper
import io.mfj.textricator.Textricator
import io.mfj.textricator.form.FormParseEventListener
import io.mfj.textricator.form.LoggingEventListener
import io.mfj.textricator.form.WriterEventListener
import io.mfj.textricator.form.config.FormParseConfigUtil
import io.mfj.textricator.text.toPageFilter

import java.io.*
import javax.activation.MimetypesFileTypeMap

import org.slf4j.LoggerFactory

import spark.Spark.*
import spark.utils.IOUtils
import java.net.URLEncoder

object Main {

  private val log = LoggerFactory.getLogger(Main::class.java)

  private val mimeTypeMap = MimetypesFileTypeMap.getDefaultFileTypeMap()

  /**
   * @param args If not blank, first argument is a path to a directory containing PDFs to use. Subdirectories ignored.
   */
  @JvmStatic
  fun main( args:Array<String> ) {

    staticFiles.location("/static")

    val dir:File? = if ( args.isEmpty() ) null else File( args.first() )
    val fileSource:FileSource =
        when {
          dir == null -> ResourcesFileSource
          dir.isDirectory -> {
            log.info( "PDFs from \"${dir}\"" )
            DirectoryFileSource(dir)
          }
          else -> {
            log.warn( "\"${dir}\" is not a directory. Using built-in PDFs" )
            ResourcesFileSource
          }
        }

    // Get the help page
    get( "/" ) { req, res ->
      res.type("text/html")
      Main::class.java.getResourceAsStream("/static/index.html").use { i ->
        res.raw().outputStream.use { o ->
          IOUtils.copy(i,o)
        }
      }
    }

    redirect.get("/files", "/files/")
    get( "/files/" ) { req, res ->
      val files = fileSource.list().sorted()
      val accept = req.headers("Accept")
      when ( accept ) {
        "application/json" -> {
          res.type("application/json")
          res.header("Content-Disposition","filename=\"files.json\"")
          ObjectMapper().writeValueAsString( files.map { file -> "${req.url()}${file}" }
          )
        }
        else -> {
          res.type("text/html")
          """<!DOCTYPE html>
          <html>
          <body>
          <ul>
          ${
          files.map { file ->
            """
              <li>
              <a href="${URLEncoder.encode(file,"UTF-8")}">${URLEncoder.encode(file,"UTF-8")}</a>
              (<a href="${URLEncoder.encode(file,"UTF-8")}/ui">web ui</a>)
              </li>
            """
          }.joinToString("")
          }
          </ul>
          </body>
          </html>"""
        }
      }
    }

    // TODO: a way to upload files

    // Run the parser
    // /files/{foo.pdf}/process <- yml
    post("/files/:fileId/form/:format") { req, res ->
      val fileId = req.params(":fileId")
      val format = req.params(":format")
      val pages = req.queryParams("pages")
      val yaml = req.body()

      val inputFormat = File(fileId).extension

      val (outputFormat,contentType) = when ( format.toUpperCase() ) {
        "CSV" -> Textricator.RECORD_OUTPUT_FORMAT_CSV to "text/csv"
        "JSON" -> Textricator.RECORD_OUTPUT_FORMAT_JSON to "application/json"
        "JSONFLAT" -> Textricator.RECORD_OUTPUT_FORMAT_JSON_FLAT to "application/json"
        "LOG" -> Textricator.RECORD_OUTPUT_FORMAT_NULL to "text/plain"
        else -> throw Exception( "Format \"${format}\" unsupported. Use \"csv\", \"json\", \"jsonflat\", or \"log\"." )
      }
      res.type( contentType )

      val status = res.raw().outputStream.use { outputStream ->

        val (eventListener:FormParseEventListener,eventListenerWriter:Writer?) =
            if ( format.toUpperCase() == "LOG" ) {
              val w = OutputStreamWriter(outputStream)
              WriterEventListener(w) to w
            } else {
              LoggingEventListener to null
            }

        try {
          val config = FormParseConfigUtil.parseYaml(yaml)

          if ( ! pages.isNullOrBlank() ) config.pages = pages

          fileSource.get(fileId).use { fileStream ->

            Textricator.parseForm(
                input = fileStream,
                inputFormat = inputFormat,
                output = outputStream,
                outputFormat = outputFormat,
                config = config,
                eventListener = eventListener )

          }

          200
        } catch (e:Exception) {

          eventListenerWriter?.flush()

          log.error(e.message,e)

          // this will not work if the response was already partially sent.
          // HTTP/1.1 supports a trailer, and java servlet spec 4.0 supports it, but spark uses 3.1.
          // the client will have to notice bad json.

          BufferedWriter( OutputStreamWriter( outputStream ) ).use { w ->
            w.write("\n\nError:\n")
            w.write(e.message)
            //e.printStackTrace(w)
          }
          500
         } finally {
          if ( eventListenerWriter != null ) {
            try { eventListenerWriter.close() } catch ( e:Exception ) {}
          }
        }
      }
      res.status(status)
    }

    // Get the parse UI
    get("/files/:fileId/ui") { req, res ->
      res.type("text/html")
      Main::class.java.getResourceAsStream("/static/ui.html").use { i ->
        res.raw().outputStream.use { o ->
          IOUtils.copy(i,o)
        }
      }
      res.status(200)
    }

    // Download the file
    get("/files/:fileId") { req, res ->
      val fileId = req.params(":fileId")
      val contentType = mimeTypeMap.getContentType(fileId)
      res.type(contentType)
      fileSource.get(fileId).use { stream ->
        res.raw().outputStream.use { os ->
          IOUtils.copy(stream,os)
        }
      }

    }

    // Extract text
    get("/files/:fileId/text") { req, res ->
      val fileId = req.params(":fileId")
      val pages = req.queryParams("pages")
      val inputFormat = req.queryParams("format") ?: File(fileId).extension
      val pageFilter = pages.toPageFilter()

      res.type("application/json")

      fileSource.get(fileId).use { fileStream ->

        res.raw().outputStream.use { os ->
          Textricator.extractText(
              input = fileStream,
              inputFormat = inputFormat,
              output = os,
              outputFormat = "json",
              pageFilter = pageFilter
          )
        }
      }

      res.status(200)
    }

    // return source jars for agpl-licensed dependencies

    val sourceJarDir = System.getenv()["SOURCE_JAR_DIR"]?.let { File(it) }
    if ( sourceJarDir != null && sourceJarDir.isDirectory ) {
      log.info( "source jars in ${sourceJarDir}" )
      get( "/source/:file" ) { req, res ->
        val jar = req.params(":file").replace("/","")
        if ( jar.endsWith( "-sources.jar" ) ) {
          val jarFile = File( sourceJarDir, jar )
          if ( jarFile.exists() ) {
            res.type("application/java-archive")
            jarFile.inputStream().use { i ->
              res.raw().outputStream.use { o ->
                IOUtils.copy(i,o)
              }
            }
          } else {
            log.warn( "Jar file \"${jarFile}\" is missing." )
            res.status(404)
            null
          }
        } else {
          log.warn( "Requested jar \"${jar}\" does not end in \"-sources.jar\"." )
          res.status(404)
          null
        }
      }
    } else {
      log.warn( "\$SOURCE_JAR_DIR \"${sourceJarDir}\" does not exist." )
    }

  }

}
