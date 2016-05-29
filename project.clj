(defproject navis/untangled-spec "0.3.7-SNAPSHOT"
  :description "A Behavioral specification system for clj and cljs stacked on clojure.test"
  :url ""
  :license {:name "MIT Public License"
            :url  ""}
  :dependencies [[org.clojure/clojure "1.9.0-alpha3" :scope "provided"]
                 [org.clojure/clojurescript "1.9.2" :scope "provided"]
                 [colorize "0.1.1" :exclusions [org.clojure/clojure]]
                 [com.lucasbradstreet/cljs-uuid-utils "1.0.2"]
                 [cljsjs/react-with-addons "0.14.0-1" :scope "provided"]
                 [org.omcljs/om "1.0.0-alpha32" :scope "provided"]
                 [io.aviso/pretty "0.1.23"]
                 [lein-doo "0.1.6" :scope "test"]]

  :plugins [[lein-cljsbuild "1.1.2"]
            [lein-doo "0.1.6"] ; for cljs CI tests
            [lein-figwheel "0.5.0-2" :exclusions [ring/ring-core commons-fileupload clj-time joda-time org.clojure/clojure org.clojure/tools.reader]]
            [com.jakemccrary/lein-test-refresh "0.15.0"]]

  :source-paths ["src"]
  :test-paths ["test"]
  :resource-paths ["src" "resources"]

  ; CI tests: Set up to support karma runner. Recommend running against chrome. See README
  :doo {:build "automated-tests"
        :paths {:karma "node_modules/karma/bin/karma"}}

  :clean-targets ^{:protect false} [:target-path "target" "resources/public/js" "resources/private/js"]

  :cljsbuild {:test-commands {"unit-tests" ["phantomjs" "run-tests.js" "resources/private/unit-tests.html"]}
              :builds        [{:id           "test"
                               :jar          true
                               :source-paths ["src" "dev" "test"]
                               :figwheel     {:on-jsload "cljs.user/on-load"}
                               :compiler     {:main                 cljs.user
                                              :output-to            "resources/public/js/test/test.js"
                                              :output-dir           "resources/public/js/test/out"
                                              :recompile-dependents true
                                              :asset-path           "js/test/out"
                                              :optimizations        :none}}
                              ;; FOR CI tests. Runs via doo
                              {:id           "automated-tests"
                               :source-paths ["src" "test"]
                               :compiler     {:output-to     "resources/private/js/unit-tests.js"
                                              :main          untangled-spec.all-tests
                                              :optimizations :none}}]}

  :jvm-opts ["-XX:-OmitStackTraceInFastThrow"]

  :figwheel {:nrepl-port 7888
             :server-port 3457}
  :profiles {:dev {:source-paths ["src" "test" "dev"]
                   :repl-options {:init-ns clj.user
                                  :port    7001}
                   :env          {:dev true}}}

  :test-refresh {:report untangled-spec.reporters.terminal/untangled-report
                 :changes-only true
                 :with-repl true}
  )
