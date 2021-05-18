# TextricatorWeb

TextricatorWeb is a web UI for [Textricator](https://github.com/measuresforjustice/textricator).

_TextricatorWeb_, like _Textricator_ is released under the
[GNU Affero General Public License Version 3](https://www.gnu.org/licenses/agpl-3.0.en.html).

## Building:

1. `mvn install -Pdocker`

## Running:

### With built-in PDFs:

1. `docker run -p 4567:4567 mfj/textricator-web`
2. http://localhost:4567

### With custom PDFS:

1. `docker run -p 4567:4567 -v /path/to/pdfs:/pdfs mfj/textricator-web`
2. http://localhost:4567

PDFs in the directory `/path/to/pdfs` will be available.
The directory is **not** searched recursively.
