(defproject clj_esmesmpp "2.1.1.g-SNAPSHOT"
            :description "ESME Application clojure."
            :author "yilori"
            :omit-source true :min-lein-version "2.0.0"
            :dependencies  [[org.clojure/clojure "1.10.1"]
                            [org.clojure/tools.cli "0.2.4"]
                            [org.clojure/data.json "0.2.7"]
                            [org.clojure/core.async "0.7.559"]
                            [http-kit "2.5.0"]
                            [log4j/log4j "1.2.17" :exclusions [javax.mail/mail
                                                               javax.jms/jms
                                                               com.sun.jdmk/jmxtools
                                                               com.sun.jmx/jmxri]]
                            [org.clojure/tools.logging "0.2.6"]
                            [ch.qos.logback/logback-classic "1.2.3"]]
            :profiles  {:uberjar {:aot :all
                                  :uberjar-name  "clj_esmesmpp-2.1.1.g-SNAPSHOT-standalone.jar"}}
  :global-vars {*warn-on-reflection* false}
  :main ^:skip-aot esme.core
  :source-paths ["src"]
  :java-source-paths ["src/javacompile"])

