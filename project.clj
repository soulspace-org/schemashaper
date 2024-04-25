(defproject schemashaper "0.1.0-SNAPSHOT"
  :description "Tool for schema conversions"
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/tools.cli "1.0.206"]
                 [org.clojure/data.xml "0.0.8"]            ; EDMX/OData
                 [org.clojure/data.json "2.5.0"]           ; AVRO
                 [clj-commons/clj-yaml "1.0.27"]           ; OpenAPI
                 [com.github.s-expresso/rubberbuf "0.2.1"] ; ProtoBuf
                 ;[instaparse "1.4.14"]
                 ]
  :repl-options {:init-ns org.soulspace.schemashaper.adapter.ui.cli}

  :profiles {:dev {:dependencies [[djblue/portal "0.49.1"]
                                  [criterium/criterium "0.4.6"]
                                  [com.clojure-goes-fast/clj-java-decompiler "0.3.4"]
                                    ; [expound/expound "0.9.0"]
                                  ]
                   :global-vars {*warn-on-reflection* true}}}

  :uberjar-name "schemashaper.jar"
  :main org.soulspace.schemashaper.adapter.ui.cli

;  :scm {:name "git" :url "https://github.com/soulspace-org/schemashaper"}
;  :deploy-repositories [["clojars" {:sign-releases false :url "https://clojars.org/repo"}]]
)
