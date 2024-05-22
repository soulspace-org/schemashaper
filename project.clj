(defproject schemashaper "0.1.0-SNAPSHOT"
  :description "Tool for schema conversions"
  :dependencies [[org.clojure/clojure "1.11.3"]
                 [org.clojure/tools.cli "1.0.230"]
                 [org.clojure/data.xml "0.0.8"]            ; EDMX/OData
                 [org.clojure/data.json "2.5.0"]           ; AVRO
                 [clj-commons/clj-yaml "1.0.27"]           ; OpenAPI
                 [com.github.s-expresso/rubberbuf "0.2.1"] ; ProtoBuf
                 ;[instaparse "1.4.14"]
                 ]
  :repl-options {:init-ns org.soulspace.schemashaper.adapter.ui.cli}
  :plugins [[com.github.liquidz/antq "RELEASE"]]
   ;; optional - you can add antq options here:
  :antq {}
  
  :uberjar-name "schemashaper.jar"
  :main org.soulspace.schemashaper.adapter.ui.cli

  :scm {:name "git" :url "https://github.com/soulspace-org/schemashaper"}
;  :deploy-repositories [["clojars" {:sign-releases false :url "https://clojars.org/repo"}]]
  )
