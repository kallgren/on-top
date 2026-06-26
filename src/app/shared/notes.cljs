(ns app.shared.notes
  "Cross-cutting Notes hook: paint from the last-good cache over the compiled-in
   seed floor, then revalidate from the gist's notes.md in the background (SWR),
   swapping in a fetched lookup only on a clean parse. Instantiated once in the
   shell and threaded into Core (later Rare too); Day never consults it. See
   docs/adr/0005."
  (:require [uix.core :refer [defhook use-state use-effect]]
            [app.config :as config]
            [app.notes :as notes]
            [app.storage :as storage]))

(def cache-key "on-top/notes-cache")

(defhook use-notes [seed]
  (let [[{:keys [initial url]}]
        (use-state
         (fn []
           (notes/notes-source
            {:config-url (config/gist-file-url (config/parse-config (storage/read-config)) config/notes-file)
             :cached     (storage/read-text-cache cache-key)
             :seed       seed})))
        [notes set-notes!] (use-state initial)]
    (use-effect
     (fn []
       (when url
         (notes/fetch-notes! url
                             (fn [raw parsed]
                               (storage/write-text-cache! cache-key raw)
                               (set-notes! parsed))))
       js/undefined)
     [url])
    notes))
