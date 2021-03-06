(ns untangled-spec.reporters.impl.base-reporter
  (:require #?@(:cljs ([cljs-uuid-utils.core :as uuid]
                        [cljs.stacktrace :refer [parse-stacktrace]]))
    [untangled-spec.reporters.impl.diff :refer [diff]]))

(defn make-testreport
  ([] (make-testreport []))
  ([initial-items]
   {#?@(:cljs [:id (uuid/uuid-string (uuid/make-random-uuid))])
    :summary       ""
    :namespaces    []
    #?@(:clj [:tested 0])
    :passed        0
    :failed        0
    :error         0}))

(defn make-testitem
  [name]
  {#?@(:cljs [:id (uuid/uuid-string (uuid/make-random-uuid))])
   :name         name
   :status       :pending
   :test-items   []
   :test-results []})

(defn make-manual [name] (make-testitem (str name " (MANUAL TEST)")))

#?(:cljs (defn- stack->trace [st] (parse-stacktrace {} st {} {})))

(defn merge-in-diff-results
  [{:keys [actual expected assert-type] :as test-result}]
  (cond-> test-result (#{'eq} assert-type)
    (assoc :diff (diff expected actual))))

(defn make-test-result
  [result result-detail]
  (-> result-detail
      (merge {#?@(:cljs [:id (uuid/uuid-string (uuid/make-random-uuid))])
              :status result})
      (merge-in-diff-results)
      #?(:cljs (#(if (some-> % :actual .-stack)
                   (assoc % :stack (-> % :actual .-stack stack->trace))
                   %)))))

(defn make-tests-by-namespace
  [name]
  {#?@(:cljs [:id (uuid/uuid-string (uuid/make-random-uuid))])
   :name       name
   :test-items []
   :status     :pending})

(defn set-test-result [test-state path status & [f]]
  (loop [current-test-result-path path]
    (if (> (count current-test-result-path) 1)
      (let [target (get-in @test-state current-test-result-path)
            current-status (:status target)]
        (if-not (#{:manual :error :failed} current-status)
          (swap! test-state #(assoc-in % (concat current-test-result-path [:status])
                                        status)))
        (recur (drop-last 2 current-test-result-path))))))

(defn begin [n test-state path]
  (let [test-item (make-testitem n)
        test-items-count (count (get-in @test-state (concat path [:test-items])))]
    (swap! test-state #(assoc-in % (concat path [:test-items test-items-count])
                                 test-item))
    [test-item test-items-count]))

(defn get-namespace-location [namespaces nsname]
  (let [namespace-index (first (keep-indexed (fn [idx val]
                                               (when (= (:name val) nsname)
                                                 idx))
                                             namespaces))]
    (if namespace-index namespace-index
      (count namespaces))))

(defn internal [failure-type]
  (fn [detail test-state path]
    (let [{:keys [test-results]} (get-in @test-state path)
          test-result (make-test-result failure-type detail)
          test-result-path (concat path [:test-results (count test-results)])]
      (set-test-result test-state path failure-type)
      (swap! test-state #(assoc-in % test-result-path test-result))
      test-result)))

(def error (internal :error))
(def fail  (internal :failed))

(defn summary [stats path test-state]
  (if (empty? path)
    (swap! test-state merge stats)
    (swap! test-state update-in path merge stats)))
