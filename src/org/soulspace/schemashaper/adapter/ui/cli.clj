(ns org.soulspace.schemashaper.adapter.ui.cli
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.data.json :as json]
            [clojure.data.xml :as xml]
            [clojure.tools.cli :as cli]
            [org.soulspace.schemashaper.domain.model :as model]
            [org.soulspace.schemashaper.application.conversion :as conv]
            ; require adapters to register multimethods
            ; input adapters
            [org.soulspace.schemashaper.adapter.input.avro :as avro-in]
            [org.soulspace.schemashaper.adapter.input.edmx :as edmx-in]
            [org.soulspace.schemashaper.adapter.input.overarch :as overarch-in]
            [org.soulspace.schemashaper.adapter.input.protobuf :as proto-in]
            ; output adapters
            [org.soulspace.schemashaper.adapter.output.avro :as avro-out]
            [org.soulspace.schemashaper.adapter.output.edmx :as edmx-out]
            [org.soulspace.schemashaper.adapter.output.overarch :as overarch-out]
            [org.soulspace.schemashaper.adapter.output.protobuf :as proto-out]
            [org.soulspace.schemashaper.adapter.output.graphql :as graphql-out]
            [org.soulspace.schemashaper.adapter.output.openapi :as openapi-out])
  (:gen-class))

(def appname "schemashaper")
(def description
  "SchemaShaper Schema Conversion CLI
   
   Reads a schema in input format and writes it in output format.")

(def cli-opts
  [["-I" "--input-format FORMAT"  "Input format (edmx, overarch)" :default :edmx :parse-fn keyword]
   ["-i" "--input-file FILENAME"  "Input file"]
   ["-O" "--output-format FORMAT" "Output format (avro, overarch)" :default :avro :parse-fn keyword]
   ["-o" "--output-file FILENAME" "Output file"]
   ["-c" "--config-file FILENAME" "optional EDN file with a conversion configuration"]
   ["-h" "--help"                 "Print help"]
   [nil  "--debug"                "Print debug information" :default false]])

;;;
;;; Output messages
;;;
(defn usage-msg
  "Returns a message containing the program usage."
  ([summary]
   (usage-msg (str "java --jar " appname ".jar [options]") "" summary))
  ([name summary]
   (usage-msg name "" summary))
  ([name description summary]
   (str/join "\n\n"
             [description
              (str "Usage: java -jar " name ".jar [options].")
              "Options:"
              summary])))

(defn error-msg
  "Returns a message containing the parsing errors."
  [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))

(defn exit
  "Exits the process."
  [status msg]
  (println msg)
  (System/exit status))

;;;
;;; Args validation
;;;
(defn validate-args
  "Validate command line arguments `args` according to the given `cli-opts`.
   Either returns a map indicating the program should exit
   (with an error message and optional success status), or a map
   indicating the options provided."
  [args cli-opts]
  (try
    (let [{:keys [options arguments errors summary]} (cli/parse-opts args cli-opts)]
      (cond
        errors ; errors => exit with description of errors
        {:exit-message (error-msg errors)}
        (:help options) ; help => exit OK with usage summary
        {:exit-message (usage-msg appname description summary) :success true}
        (= 0 (count arguments)) ; no args
        {:options options}
        (seq options)
        {:options options}
        :else ; failed custom validation => exit with usage summary
        {:exit-message (usage-msg appname description summary)}))
    (catch Exception e
      (.printStacktrace e))))

;;;
;;; Handler logic
;;;
(defn handle
  "Handle the `options`."
  [{:keys [input-format input-file output-format output-file config-file] :as options}]
  (if config-file
    (do (println "Converting" input-file "from" input-format
                 "to" output-format "as" output-file "using config from" config-file)
        (conv/convert input-format input-file output-format output-file config-file))
    (do (println "Converting" input-file "from" input-format
                 "to" output-format "as" output-file) 
        (conv/convert input-format input-file output-format output-file))))

;;;
;;; CLI entry 
;;;
(defn -main
  "Main function as CLI entry point."
  [& args]
  (let [{:keys [options exit-message success]} (validate-args args cli-opts)]
    (when (:debug options)
      (println options))
    (if exit-message
      ; exit with message
      (exit (if success 0 1) exit-message)
      ; handle options and generate the requested outputs
      (handle options))))

(comment
  (json/json-str (conv/model->schema :avro [{:el :class :name "TestClass" :ct [{:el :field :name "id" :type :int}]}]))

  (conv/schema->model :edmx (slurp "examples/sap-sample-edmx.xml"))

  (-main)
  (-main "-i" "dev/sap-sample-edmx.xml" "-o" "sap-sample-edmx.json" "--debug")
  (-main "-i" "dev/sap-sample-edmx.xml" "-O" "graphql" "-o" "sap-sample.graphql" "--debug")
  ;
  )
