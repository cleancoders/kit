(defproject com.cleancoders.c3kit/bucket "1.0.8"

  :description "Clean Coders Clojure (C3) Kit - Bucket: Database API for datomic and in-memory."
  :url "https://cleancoders.com"
  :license {:name "MIT License" :url "https://opensource.org/licenses/MIT"}

  :dependencies [
                 [com.cleancoders.c3kit/apron "1.0.2"]
                 [cljsjs/hashids "1.0.2-0"]
                 [jstrutz/hashids "1.0.1"]
                 [org.clojure/clojure "1.10.3"]
                 ]

  :profiles {:dev {:resource-paths ["dev"]
                   :dependencies   [
                                    [com.cleancoders.c3kit/scaffold "1.0.2" :exclusions [[com.google.code.findbugs/jsr305]]]
                                    [com.datomic/datomic-free "0.9.5697"]
                                    [com.google.guava/guava "25.1-jre" :exclusions [com.google.errorprone/error_prone_annotations]]
                                    [speclj "3.3.2"]
                                    ]}}

  :plugins [[speclj "3.3.2"]]

  :source-paths ["src/clj" "src/cljc" "src/cljs"]
  :test-paths ["spec/clj" "spec/cljc" "spec/cljs"]
  :resource-paths []

  :aliases {"cljs" ["run" "-m" "c3kit.scaffold.cljs"]
            "migrate" ["run" "-m" "c3kit.bucket.migrate"]}
  )
