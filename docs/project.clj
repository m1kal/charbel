(defproject charbel-cljs "0.1.0-SNAPSHOT"
  :description "A project demonstrating use of Charbel with Clojurescript"
  :url "https://github.com/m1kal/charbel"
  :license {:name "MIT"
            :url ""}

  :min-lein-version "2.9.1"

  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/clojurescript "1.10.773"]
                 [reagent "1.1.0"]
                 [cljsjs/react "17.0.2-0"]
                 [cljsjs/react-dom "17.0.2-0"]
                 [com.github.m1kal/charbel "0.1.8"]]

  :plugins [[lein-figwheel "0.5.20"]
            [lein-cljsbuild "1.1.7" :exclusions [[org.clojure/clojure]]]]

  :source-paths ["src"]

  :cljsbuild {:builds
              [{:id "dev"
                :source-paths ["src"]
                :figwheel true
                :compiler {:main charbel-cljs.core
                           :asset-path "js/compiled/out"
                           :output-to "resources/public/js/compiled/charbel_cljs.js"
                           :output-dir "resources/public/js/compiled/out"
                           :source-map-timestamp true}}
               {:id "min"
                :source-paths ["src"]
                :compiler {:output-to "resources/public/js/compiled/charbel_cljs.js"
                           :main charbel-cljs.core
                           :optimizations :advanced
                           :pretty-print false}}]}
  )
