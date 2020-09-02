(defproject clj_esmesmpp "2.1.0-SNAPSHOT"
            :description "ESME Application clojure."
            :author "yilori"
            :omit-source true
            :dependencies  [[org.clojure/clojure "1.10.1"]
                            [org.clojure/tools.cli "0.2.4"]
                            [org.clojure/data.json "0.2.7"]
                            [org.clojure/core.async "0.7.559"]
                            [clj-http "3.10.0"]
                            [ch.qos.logback/logback-classic "1.2.3"]
                            [org.clojure/tools.logging "0.2.6"]
                            [jenkins-hash/jenkins-hash "0.0.4"]
                            ]
  :min-lein-version "2.0.0"

  :profiles  {:uberjar {:omit-source true
                        :aot     :all
                        :uberjar-name "esmeapp.jar"}}
  :global-vars {*warn-on-reflection* false}
  :main ^:skip-aot esme.core
  :source-paths ["src" "lib/opensmpp-core-3.0.0.jar"]
  :java-source-paths ["src" "lib/opensmpp-core-3.0.0.jar"]
  :jvm-opts  ["-Dlogback.configurationFile=/Users/yimika/Documents/IdeaProjects/clj-esme-ng/resources/logback.xml"
              "-Dld.esme=/Users/yimika/Documents/IdeaProjects/clj-esme-ng/resources/esmeprop.conf"]
  )

