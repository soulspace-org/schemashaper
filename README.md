# SchemaShaper

SchemaShaper is a commandline tool for schema conversions.
It reads a schema in input format and writes it in the specified output format.

SchemaShaper is targeted to schemas used for data transfer (e.g. messaging).

## Features

Supported input schemas:
 * edmx (MS Entity Framework/[OData](https://docs.oasis-open.org/odata/odata/v4.0/os/part3-csdl/odata-v4.0-os-part3-csdl.html))
 * overarch ([Overarch](https://github.com/soulspace-org/overarch) Class Model)

Supported output schemas:
 * avro ([Apache AVRO](https://avro.apache.org/))
 * overarch ([Overarch](https://github.com/soulspace-org/overarch) Class Model)

### Limitations
SchemaShaper may use only parts of the input schema relevant to data transfer.

## Build
SchemaShaper is written in [Clojure](https://clojure.org) and gets built with
[leiningen](https://leiningen.org/). To build it, you need to have Java 11 or higher
and leiningen installed.

In the cloned SchemaShaper repository, run

```
lein uberjar
```

to build a JAR file with all dependencies. This JAR file is created in the *target* folder and is named *schemashaper.jar*

## Usage
SchemaShaper is a commandline tool to convert one data schema into another data schema.

```
SchemaShaper Schema Conversion CLI

   Reads a schema in input format and writes it in output format.

Usage: java -jar schemashaper.jar [options].

Options:

  -I, --input-format FORMAT   :edmx  Input format (edmx, overarch)
  -i, --input-file FILENAME          Input file
  -O, --output-format FORMAT  :avro  Output format (avro, overarch)
  -o, --output-file FILENAME         Output file
  -f, --filter-file FILENAME         optional EDN file with filter criteria
  -h, --help                         Print help
      --debug                        Print debug information
```

### Examples
```
java -jar schemashaper.jar -i examples/sap-sample-edmx.xml -o generated/sample-avro.json
```

## Copyright
© 2024 Ludger Solbach

## License

Eclipse Public License 1.0 (EPL1.0)

