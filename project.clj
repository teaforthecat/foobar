(defproject foobar "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [duct/core "0.7.0"]
                 [duct/module.logging "0.4.0"]
                 [duct/module.web "0.7.0"]
                 [duct/server.http.aleph "0.1.2"]
                 [org.apache.kafka/kafka-clients "2.3.0"]
                 [com.datomic/datomic-pro "0.9.5930"]
                 [com.taoensso/carmine "2.19.1"]
                 [metosin/compojure-api "2.0.0-alpha30"]
                 ]

  :repositories {"my.datomic.com" {:url "https://my.datomic.com/repo"
                                   :creds :gpg}}
  :plugins [[duct/lein-duct "0.12.1"]]
  :main ^:skip-aot foobar.main
  :resource-paths ["resources" "target/resources"]
  :prep-tasks     ["javac" "compile" ["run" ":duct/compiler"]]
  :middleware     [lein-duct.plugin/middleware]
  :profiles
  {:dev  [:project/dev :profiles/dev]
   :repl {:prep-tasks   ^:replace ["javac" "compile"]
          :repl-options {:init-ns user}}
   :uberjar {:aot :all}
   :profiles/dev {}
   :project/dev  {:source-paths   ["dev/src"]
                  :resource-paths ["dev/resources"]
                  :dependencies   [[integrant/repl "0.3.1"]
                                   [com.gearswithingears/shrubbery "0.4.1"]
                                   [org.clojure/test.check "0.10.0-alpha4"]
                                   [provisdom/spectomic "0.7.9"]
                                   [eftest "0.5.7"]
                                   [kerodon "0.9.0"]]}})
