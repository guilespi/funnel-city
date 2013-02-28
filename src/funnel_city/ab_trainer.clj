(ns funnel-city.ab-trainer)

;;the models are indexed by page name
;;so, each page has its own positive/negative bayes classes with
;;its own probabilities

;;using an atom for the page models is NOT thread-safe
(def models (atom {}))

;;the datum may have other attributes we
;;want to consider when building our model
(def training-attributes [:referer_domain
                          :visited_share_page])

;; each model structure
;; {
;; :positives 0
;; :negatives 0
;; :pos_attrs {
;;     attr : {
;;        v1: 4
;;        v2: 9
;;     }
;;   }
;; :neg_attrs {}
;; :value_attrs {
;;      attr: #{v1 v2 v3}
;;  }
;;}
(def default-model {:positives 0
                    :negatives 0
                    :pos_attrs {}
                    :neg_attrs {}
                    :value_attrs {}
                    })

(defn- inc-registrations
  "Update the number of positive or negative
  registrations in a specific model"
  [model result]
  (let [key (if result :positives :negatives)]
    (assoc model key (inc (key model)))))

(defn- update-attr-value
  "Updates the known values for an attribute
  in a specific model"
  [model name value]
  (let [s (get-in model [:value_attrs name] #{})]
    (assoc-in model [:value_attrs name] (conj s value))))

(defn- update-reg-value
  "Update the total values for an attribute
  for a specific model which had a positive
  or negative result (registered or not)"
  [model name value result]
  (let [key (if result :pos_attrs :neg_attrs)
        current (-> model key (get name) (get value))]
    (assoc-in model [key name value] (inc (or current 0)))))

(defn- update-model
  "Updates a model with the current attributes and
  registration result"
  [model attrs registered]
  (reduce
   (fn [m attr]
     (let [{name 0, value 1} attr]
       (-> m
           (update-attr-value name value)
           (update-reg-value name value registered))))
   (inc-registrations model registered) attrs))

(defn- page-model
  "Retrieves the model for a page"
  [page]
  (or ((keyword page) @models)
      default-model))

(defn train-page
  "Trains the model with a specific result"
  [datum page registered]
  (let [attrs (select-keys datum training-attributes)
        model (page-model page)]
    (swap! models #(assoc % (keyword page)
                          (update-model model attrs registered)))))


;;classification functions

(defn- calc-m-estimator
  "This could be done using an equivalent sample size
  and calculating each attribute probability"
  [class-attrs total name value]
  (let [e (or (-> class-attrs (get name) (get value)) 0)]
    (/ (+ e 1) (+ total 1))))


(defn- class-probability
  "Iterate over each attribute and calculate the
  conditional probability for the class"
  [class-attrs attrs start-value total]
  (reduce #(let [{name 0, value 1} %2]
             (* %1 (calc-m-estimator class-attrs total name value)))
          start-value attrs))


(defn- page-registration-score
  "For a given page, calculate the probability
  of the page resulting in registration with the
  given attributes"
  [attrs page]
  (let [model (page-model page)
        pos (:positives model)
        neg (:negatives model)]
    (if (and (= 0 pos) (= 0 neg))
      1/2 ;;if I have no samples assume 50% probability for this page
      (let [prob-pos (class-probability (:pos_attrs model) attrs (/ pos (+ pos neg)) pos)
            prob-neg (class-probability (:neg_attrs model) attrs (/ neg (+ pos neg)) neg)]
        (/ prob-pos (+ prob-pos prob-neg))))))


(defn- sort-pages
  [page-scores]
  (into (sorted-map-by (fn [k1 k2]
                     (let [c (compare (get page-scores k2) (get page-scores k1))]
                       (if (= 0 c) (rand-nth [1 -1]) c)))) page-scores))

(defn select-best-page
  "Select the best page from copy_choices according
  to each page probability to convert the user.
  If many pages share the best score, choose randomly one of those"
  [datum]
  (let [pages (:copy_choices datum)
        attrs (select-keys datum training-attributes)
        page-scores (reduce #(merge %1 {%2 (page-registration-score attrs %2)}) {} pages)]
    (-> (sort-pages page-scores) first (get 0))))