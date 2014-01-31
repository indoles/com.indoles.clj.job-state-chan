(defproject com.indoles.clj/jobs-state-chan "0.1.2"
  :description "core.async channel based job execution queue"
  :url "http://github.com/indoles/com.indoles.clj.job-state-chan.git"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :repl-options {:init-ns com.indoles.clj.jobs-state-chan}
  :pom-addition [:developers [:developer [:name "Indoles"]]]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                 [com.indoles.clj/state-chan "0.1.0"]])
