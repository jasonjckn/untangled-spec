(ns smooth-spec.provided-spec
  #?(:clj
     (:require [smooth-spec.core :as c :refer [specification behavior provided with-timeline async tick assertions]]
               [clojure.test :as t :refer (are is deftest with-test run-tests testing do-report)]
               [smooth-spec.provided :as p]
               ))
  #?(:cljs (:require-macros [cljs.test :refer (are is deftest run-tests testing)]
             [smooth-spec.provided :as p]
             [smooth-spec.core :refer [specification behavior provided with-timeline async tick assertions]]
             ))
  #?(:cljs (:require [cljs.test :refer [do-report]]
             )
     )
  #?(:clj
     (:import clojure.lang.ExceptionInfo))
  )

#?(:clj
   (specification "parse-arrow-count"
                  (behavior "requires the arrow start with an ="
                            (is (thrown? AssertionError (p/parse-arrow-count '->)))
                            )
                  (behavior "requires the arrow end with =>"
                            (is (thrown? AssertionError (p/parse-arrow-count '=2x>)))
                            )
                  (behavior "derives a :many count for general arrows"
                            (assertions
                              (p/parse-arrow-count '=>) => :many
                              )
                            )
                  (behavior "throws an assertion error if count is zero"
                            (is (thrown? AssertionError (p/parse-arrow-count '=0x=>)))
                            )
                  (behavior "derives a numeric count for numbered arrows"
                            (assertions
                              (p/parse-arrow-count '=1x=>) => 1
                              (p/parse-arrow-count '=7=>) => 7
                              (p/parse-arrow-count '=234x=>) -> 234
                              )
                            )
                  ))

#?(:clj
   (specification "parse-mock-triple"
                  (let [result (p/parse-mock-triple ['(f a b) '=2x=> '(+ a b)])]
                    (behavior "includes a call count"
                              (assertions
                                (contains? result :ntimes) => true
                                (:ntimes result) => 2
                                )
                              )
                    (behavior "includes a stubbing function"
                              (assertions
                                (contains? result :stub-function) => true
                                (:stub-function result) => '(clojure.core/fn [a b] (+ a b))
                                )
                              )
                    (behavior "includes the symbol to mock"
                              (assertions
                                (contains? result :symbol-to-mock) => true
                                (:symbol-to-mock result) => 'f
                                )
                              )
                    )
                  )
   )

#?(:clj
   (specification "convert-groups-to-symbolic-triples"
                  (let [grouped-data {'a [
                                          {:ntimes 2 :symbol-to-mock 'a :stub-function '(fn [] 22)}
                                          {:ntimes 1 :symbol-to-mock 'a :stub-function '(fn [] 32)}
                                          ]
                                      'b [
                                          {:ntimes 1 :symbol-to-mock 'b :stub-function '(fn [] 42)}
                                          ]}
                        scripts (p/convert-groups-to-symbolic-triples grouped-data)
                        ]
                    (behavior "creates a vector of triples (each in a vector)"
                              (is (vector? scripts))
                              (is (every? vector? scripts))
                              )


                    (behavior "nested vectors each contain the symbol to mock as their first element"
                              (assertions
                                (first (first scripts)) => 'a
                                (first (second scripts)) => 'b
                                )
                              )

                    (behavior "nested vectors each contain a unique script symbol in their second element"
                              (assertions
                                (count (reduce (fn [acc ele] (conj acc (second ele))) #{} scripts)) => 2
                                ))

                    (behavior "nested vectors' last member is a syntax-quoted call to make-script"
                              (assertions
                                (last (first scripts)) =>
                                '(smooth-spec.stub/make-script "a" [(smooth-spec.stub/make-step (fn [] 22) 2) (smooth-spec.stub/make-step (fn [] 32) 1)])
                                (last (second scripts)) =>
                                '(smooth-spec.stub/make-script "b" [(smooth-spec.stub/make-step (fn [] 42) 1)])
                                ))
                  ))
)

#?(:clj
   (specification "provided-macro"
                  (behavior "Outputs a syntax-quoted block"
                            (let [expanded (p/provided-fn '(f n) '=> '(+ n 1) '(f n) '=2x=> '(* 3 n) '(is (= 1 2)))
                                  let-defs (second expanded)
                                  script-steps (last (second let-defs))
                                  redef-block (last expanded)
                                  ]
                              (behavior "with a let of the scripted stubs"
                                        (assertions (first expanded) => 'clojure.core/let
                                                    (count let-defs) => 2
                                                    (vector? let-defs) => true
                                                    )
                                        )
                              (behavior "containing a script with the number proper steps"
                                        (assertions
                                          (count script-steps) => 2)
                                        )
                              (behavior "that surrounds the final assertions with a redef"
                                        (assertions
                                          (first redef-block) => 'clojure.core/with-redefs
                                          (last redef-block) => '(is (= 1 2))
                                          )
                                        )
                              )
                            )
                  )
   )

(defn my-square [x] (* x x))

(specification "provided-macro"
               (behavior "actually causes stubbing to work"
                         (provided "that functions are mocked the correct number of times, with the correct output values."
                                   (my-square n) =1x=> 1
                                   (my-square n) =2x=> 1
                                   (assertions
                                     (+ (my-square 7) (my-square 7) (my-square 7)) => 3
                                     )
                                   )
                         (provided "a mock for 2 calls"
                                   (my-square n) =1x=> (+ n 5)
                                   (my-square n) =1x=> (+ n 7)
                                   (behavior "throws an exception if the mock is called 3 times"
                                             (is (thrown? ExceptionInfo
                                                          (+ (my-square 1) (my-square 1) (my-square 1))))
                                             )
                                   )


                         (provided "a mock for 3 calls with 2 different return values"
                                   (my-square n) =1x=> (+ n 5)
                                   (my-square n) =2x=> (+ n 7)
                                   (behavior "all 3 mocked calls return the mocked values"
                                             (assertions
                                               (+ (my-square 1) (my-square 1) (my-square 1)) => 22
                                               ))
                                   )
                         )

               (behavior "allows any number of trailing forms"
                         (let [detector (atom false)]
                           (provided "mocks that are not used"
                                     (my-square n) =1x=> (+ n 5)
                                     (my-square n) => (+ n 7)

                                     (+ 1 2)
                                     (+ 1 2)
                                     (+ 1 2)
                                     (+ 1 2)
                                     (* 3 3)
                                     (* 3 3)
                                     (* 3 3)
                                     (* 3 3)
                                     (my-square 2)
                                     (my-square 2)
                                     (reset! detector true)
                                     )
                           (is (= true @detector))
                           ))
               )