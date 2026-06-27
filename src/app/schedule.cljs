(ns app.schedule
  "Runtime resolution of the Schedule: a remote gist override over the
   compiled-in seed schedule. See docs/adr/0005."
  (:require [cljs.reader :as reader]
            [clojure.string :as str]))

(defn- title-case [kw]
  (->> (str/split (name kw) #"-")
       (map str/capitalize)
       (str/join " ")))

(defn schedule->categories
  "A Schedule's Categories: ordered [key label] pairs from its top-level keys, in
   file order, each label title-cased from the key (:home-office → \"Home
   Office\"). The single source of truth for both surfaces; callers needing only
   keys take the firsts. See docs/adr/0012."
  [schedule]
  (mapv (fn [k] [k (title-case k)]) (keys schedule)))

(defn parse-schedule [s]
  (try
    (let [parsed (reader/read-string s)]
      (when (or (map? parsed) (vector? parsed)) parsed))
    (catch :default _ nil)))

(defn resolve-schedule [cached seed]
  (or cached seed))

(defn schedule-source
  "Resolve one surface's Schedule source from its inputs: the Schedule to paint
   now — last-good cached EDN over the compiled-in seed floor — and the gist
   `:url` to revalidate from, or nil when unconfigured so the surface simply
   stays on cache/seed. See docs/adr/0010."
  [{:keys [config-url cached seed]}]
  {:initial (resolve-schedule (parse-schedule cached) seed)
   :url     (not-empty config-url)})

(defn fetch-schedule! [url on-ok]
  (-> (js/fetch url)
      (.then (fn [res]
               (if (.-ok res)
                 (.text res)
                 (throw (js/Error. (str "HTTP " (.-status res)))))))
      (.then (fn [raw]
               (if-let [parsed (parse-schedule raw)]
                 (on-ok raw parsed)
                 (throw (js/Error. "not a valid schedule")))))
      (.catch (fn [err]
                (js/console.warn (str "on-top: ignoring remote schedule (" url ") —")
                                 (.-message err))))))
