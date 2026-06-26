(ns app.shared.schedule
  "Cross-cutting Schedule hook: each surface paints from its last-good cache over
   the seed floor, then revalidates from its own gist in the background (SWR),
   swapping in a fetched Schedule only on a clean parse. One hook, instantiated
   once per surface with its own config key and cache key. See docs/adr/0010."
  (:require [uix.core :refer [defhook use-state use-effect]]
            [app.config :as config]
            [app.schedule :as sched]
            [app.storage :as storage]))

(defhook use-schedule [config-key cache-key seed]
  (let [[{:keys [initial url]}]
        (use-state
         (fn []
           (sched/schedule-source
            {:config-url (config-key (config/parse-config (storage/read-config)))
             :cached     (storage/read-text-cache cache-key)
             :seed       seed})))
        [schedule set-schedule!] (use-state initial)]
    (use-effect
     (fn []
       (when url
         (sched/fetch-schedule! url
                                (fn [raw parsed]
                                  (storage/write-text-cache! cache-key raw)
                                  (set-schedule! parsed))))
       js/undefined)
     [cache-key url])
    schedule))
