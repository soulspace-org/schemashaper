(ns org.soulspace.schemashaper.adapter.cli
  (:require [clojure.string :as str]
            [clojure.data.json :as json]
            [clojure.data.xml :as xml]
            [clojure.tools.cli :as cli]
            [org.soulspace.schemashaper.domain.model :as model]
            [org.soulspace.schemashaper.application.conversion :as conv]
            ; require adapters to register multimethods
            [org.soulspace.schemashaper.adapter.avro :as avro]
            [org.soulspace.schemashaper.adapter.edmx :as edmx]
            [org.soulspace.schemashaper.adapter.overarch :as overarch]
            [org.soulspace.schemashaper.adapter.protobuf :as proto]
            [clojure.edn :as edn])
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
   ["-f" "--filter-file FILENAME" "optional EDN file with a filter definition"]
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
  [{:keys [input-format input-file output-format output-file filter-file] :as options}]
  (if filter-file
    (do (println "Converting" input-file "from" input-format
                 "to" output-format "as" output-file "using filter spec from" filter-file)
        (conv/convert input-format input-file output-format output-file filter-file))
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

  (conv/schema->model :edmx (slurp "dev/sap-sample-edmx.xml"))

  (-main "-i" "dev/sap-sample-edmx.xml" "-o" "sap-sample-edmx.json" "--debug")
  ;
  )
