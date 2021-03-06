(ns untangled-spec.tests-to-run
  (:require untangled-spec.async-spec
            untangled-spec.stub-spec
            untangled-spec.provided-spec
            untangled-spec.timeline-spec
            untangled-spec.dom.events-spec
            untangled-spec.dom.util-spec
            untangled-spec.reporters.impl.base-reporter-spec
            untangled-spec.reporters.impl.diff-spec
            untangled-spec.reporters.browser-spec
            untangled-spec.assertions-spec
            untangled-spec.contains-spec))

;********************************************************************************
; IMPORTANT:
; For cljs tests to work in CI, we want to ensure the namespaces for all tests are included/required. By placing them
; here (and depending on them in user.cljs for dev), we ensure that the all-tests namespace (used by CI) loads
; everything as well.
;********************************************************************************


