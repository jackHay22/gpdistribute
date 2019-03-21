(ns io.jackhay.gpdistribute
  (:require [io.jackhay.gpdistribute.impl.engine :as engine]
            [io.jackhay.gpdistribute.impl.eval :as eval]))

(defn distribute-population
  "Takes population and optionally
  any arg overrides in a map, distributes
  population based on configuration"
  [pop & args]
  (engine/distribute-population pop
    (engine/get-default-config args)))

(defn start-eval-worker
  "Takes evaluation function and
  optionally any arg overrides in a map,
  listens for individuals, evaluates using
  hook, and returns individual to engine"
  [eval-hook & args]
  (eval/start-eval-worker eval-hook
    (engine/get-default-config args)))
