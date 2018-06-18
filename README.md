# TextricatorWeb

Web UI for [Textricator](https://textricator.mfj.io).

[online demo](https://textricator-demo.mfj.io)

[Textricator source code](https://github.com/measuresforjustice/textricator)

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
