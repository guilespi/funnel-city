(ns funnel-city.core
  (:require [funnel-city.ab-trainer :as trainer]
            [clojure.java.io]
            [clojure.data.json :as json])
  (:use [incanter.core :only [view]]
        [incanter.charts :only [line-chart]]
        [clojure.tools.cli :only [cli]]))

(defn sim-user-input
  "Given one line of the data file selects the best page,
   trains the model and returns if it was a succesful selection"
  [sample]
  (let [{datum 0, outcomes 1} (json/read-json sample)
        selected-page (trainer/select-best-page datum)]
    (do (trainer/train-page datum selected-page ((keyword selected-page) outcomes))
        (if ((keyword selected-page) outcomes) 1 0))))

(defn run-simulation
  "Run simulation and chart evolution of success rate"
  [file]
  (let [user-samples (line-seq (clojure.java.io/reader file))
        results (map sim-user-input user-samples)
        slots (partition 100 results)
        avg-by-slot (map #(/ (apply + %) (count %)) slots)]
    (view (line-chart (range (count avg-by-slot))
                      avg-by-slot
                      :x-label "iterations"
                      :y-label "% success"))))

(defn -main
  "Run me"
  [& args]
  (let [[options args banner] (cli args
                     ["-f" "--data-file" "data file" :default "data/q1_data.json"])]
    (run-simulation (clojure.java.io/file (:data-file options)))))
