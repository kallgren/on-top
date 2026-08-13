(ns app.core.week-grid
  "Core's Week grid: the whole two-week cycle at once, both Week parity halves in
   Schedule order, seven weekdays across and Categories down. Same Occurrences
   and the same Done-through store as the day face — no new persistence.

   On trial. Entered with `w` on wide viewports, ephemeral, and a full-width
   takeover while it is up: keyboard-only, gated at its own ~1100px threshold,
   and not persisted, so a reload lands back on the daily buttons. Someone who
   never presses `w` cannot reach it, which is why it needs no feature flag."
  (:require [uix.core :refer [defui defhook $ use-state use-effect use-callback]]
            [app.core.store :as core-store]
            [app.date-utils :refer [iso-date iso-week week-parity]]
            [app.keybinding :as keybinding]
            [app.keymap :as keymap]
            [app.notes :as notes]
            [app.shared.store :as store]
            [app.storage :as storage]
            [app.sync :as sync]))

;; ── Dates ────────────────────────────────────────────────────────────────────

(def weekday-order [:monday :tuesday :wednesday :thursday :friday :saturday :sunday])

(def day-letters ["M" "T" "W" "T" "F" "S" "S"])

(defn add-days [date n]
  (js/Date. (.getFullYear date) (.getMonth date) (+ (.getDate date) n)))

(defn monday-of [date]
  (let [g (.getDay date)]
    (add-days date (if (zero? g) -6 (- 1 g)))))

(defn week-dates [monday]
  (mapv #(add-days monday %) (range 7)))

(defn weekday-index
  "Index of `date`'s weekday in `weekday-order`."
  [date]
  (mod (+ 6 (.getDay date)) 7))

(defn blocks
  "The two anchored blocks, top to bottom, always ascending: the upper week is
   never later than the lower one. The block matching today's parity is this
   week; the other is the adjacent week on whichever side keeps Schedule order
   (`:week-odd` above `:week-even`) and ascending order agreeing — next week when
   this week is odd, last week when it is even. At the 53-week boundary the
   adjacent week repeats this week's parity, so the week on the other side stands
   in and the pair is still sorted ascending."
  [today]
  (let [this-monday (monday-of today)
        this-parity (week-parity today)
        wanted (if (= this-parity :week-odd) 7 -7)
        other-monday (add-days this-monday
                               (if (= this-parity (week-parity (add-days this-monday wanted)))
                                 (- wanted)
                                 wanted))
        block (fn [monday current?]
                {:parity (week-parity monday)
                 :dates (week-dates monday)
                 :current? current?})]
    (vec (sort-by #(.getTime (first (:dates %)))
                  [(block this-monday true) (block other-monday false)]))))

(defn block-heading
  "Which real week a block is anchored to — the ISO week number, nothing else."
  [{:keys [dates]}]
  (str "W" (iso-week (first dates))))

;; ── Placement ────────────────────────────────────────────────────────────────

(defn block-bands
  "One band per Category, in Schedule order, present even when the Category has
   nothing that week. A band's height is the busiest day in it."
  [schedule categories parity]
  (for [[cat label] categories
        :let [columns (mapv #(vec (get-in schedule [cat parity %])) weekday-order)]]
    {:cat cat
     :label label
     :columns columns
     :height (apply max 1 (map count columns))}))

(defn placed
  "Assign grid rows to blocks and bands, leaving one separator row between the
   blocks and, when `labelled?`, one label row above each."
  [schedule categories bs labelled?]
  (loop [row 2, out [], remaining bs]
    (if-let [blk (first remaining)]
      (let [start (if labelled? (inc row) row)
            [bands end] (reduce (fn [[acc r] band]
                                  [(conj acc (assoc band :row r)) (+ r (:height band))])
                                [[] start]
                                (block-bands schedule categories (:parity blk)))]
        (recur (inc end)
               (conj out (assoc blk
                                :label-row (when labelled? row)
                                :bands bands
                                :start-row start
                                :end-row end
                                :sep-row (when (seq out) (dec (if labelled? row start)))))
               (rest remaining)))
      out)))

;; ── Store ────────────────────────────────────────────────────────────────────

(defn unset [{:keys [completions outbox]} id]
  {:completions (dissoc completions id)
   :outbox      (sync/mark-dirty outbox id)})

(defn- read-initial []
  {:completions (or (storage/read-completions core-store/completions-key) {})
   :outbox      (or (storage/read-outbox core-store/outbox-key) #{})})

(defn- persist! [{:keys [completions outbox]}]
  (storage/write-completions! core-store/completions-key completions)
  (storage/write-outbox! core-store/outbox-key outbox))

(defhook use-grid-store
  "The same Done-through store the day face uses, exposed as raw completions plus
   a dated toggle. `next-done-through` already takes the date it is asked about,
   so marking at D sets Done-through to D and unmarking at D rolls back to the
   Occurrence before D — no new store operation beyond dropping the key when
   there is no earlier Occurrence.

   INVARIANT — the grid must stay a takeover. This creates a *second* store atom
   over the same localStorage keys as `core.store`. It is correct only because
   the shell unmounts `surfaces` while the grid is up, so exactly one of the two
   is ever alive and handoff happens through localStorage. Nothing enforces
   that. The day this face becomes a pane beside the day face instead of
   replacing it, the two stores diverge silently — so at that point hoist
   `use-store` into the shell and hand both faces the same store."
  [today schedule category-keys]
  (let [[st] (use-state #(store/create (read-initial)))
        snapshot (store/use-subscribe st)]
    (store/use-sync! st snapshot
                     {:persist! persist! :creds store/creds :surface "core"} today)
    [(:completions snapshot)
     (use-callback
      (fn [id date]
        (let [next (core-store/next-done-through (:completions @st) schedule category-keys date id)]
          (if next
            (swap! st store/toggled id next)
            (swap! st unset id))))
      [st schedule category-keys])]))

;; ── Cells ────────────────────────────────────────────────────────────────────

(defui day-header [{:keys [letters offset today-col]}]
  ($ :<>
     (for [[i letter] (map-indexed vector letters)
           :let [today? (= (+ offset i) today-col)]]
       ($ :div {:key i
                :style #js {:gridRow 1 :gridColumn (+ offset i)}
                :class (str "pb-1 text-center text-[13px] font-bold uppercase tracking-[0.2em] "
                            (if today?
                              "-mx-1.5 rounded-xl bg-today pt-1 text-label"
                              "text-heading"))}
          letter))))

(defui band-label [{:keys [label row height]}]
  ($ :div {:style #js {:gridRow (str row " / span " height) :gridColumn 1}
           :class "flex items-center pr-4 text-right text-[13px] font-semibold uppercase tracking-[0.18em] text-heading"}
     label))

(defui today-wash [{:keys [col from to]}]
  ($ :div {:style #js {:gridRow (str from " / " to) :gridColumn col}
           :class "-m-1.5 rounded-2xl bg-today"}))

(defui separator [{:keys [row span]}]
  ($ :div {:style #js {:gridRow row :gridColumn (str "1 / span " span)}
           :class "my-2 border-t border-edge"}))

(defui corner-tick []
  ($ :span {:class "absolute right-1 top-0.5 text-[12px] font-bold leading-none text-white/80"}
     "✓"))

(defui cell
  "The label stays put whether or not the Occurrence is done, so the grid reads
   as the Schedule too and not only as a state of completion. Done is the green
   fill plus a corner tick — the day face keeps its big ✓. Occurrences after
   today are inert: still readable as schedule, not claimable as coverage."
  [{:keys [name done? future? on-click row col]}]
  ($ :button
     {:on-click (when-not future? on-click)
      :disabled future?
      :aria-pressed done?
      :aria-label name
      :style #js {:gridRow row :gridColumn col}
      :class (str "relative flex aspect-[2/1] w-full items-center justify-center "
                  "overflow-hidden select-none rounded-xl border-2 px-2 "
                  "transition-colors duration-100 [container-type:inline-size] "
                  (if done? "bg-done border-done " "bg-surface border-edge ")
                  (if future?
                    "cursor-default opacity-45 "
                    (str "cursor-pointer " (when-not done? "hover:bg-surface-hover "))))}
     ($ :span {:class (str "line-clamp-3 text-center font-bold leading-tight text-label-fluid "
                           (if done? "text-white" "text-label"))}
        name)
     (when done? ($ corner-tick))))

;; ── The grid ─────────────────────────────────────────────────────────────────

(defui two-block-grid [{:keys [today schedule categories notes completions toggle]}]
  (let [bs (placed schedule categories (blocks today) true)
        col-of-today (+ 2 (weekday-index today))
        today-key (iso-date today)]
    ($ :div {:class "grid w-full items-stretch gap-2"
             :style #js {:gridTemplateColumns "max-content repeat(7, minmax(0, 1fr))"}}
       ($ day-header {:letters day-letters :offset 2 :today-col col-of-today})
       (for [{:keys [parity dates current? bands label-row start-row end-row sep-row] :as blk} bs]
         ($ :div {:key (str parity "-" (iso-date (first dates))) :class "contents"}
            (when sep-row ($ separator {:row sep-row :span 8}))
            (when current? ($ today-wash {:col col-of-today :from start-row :to end-row}))
            (when label-row
              ($ :div {:style #js {:gridRow label-row :gridColumn 1}
                       :class (str "flex items-end justify-end pr-4 pt-2 text-[12px] "
                                   "font-bold uppercase tracking-[0.16em] "
                                   (if current? "text-label" "text-muted"))}
                 (block-heading blk)))
            (for [{:keys [cat label columns row height]} bands]
              ($ :div {:key (str cat) :class "contents"}
                 ($ band-label {:label label :row row :height height})
                 (for [[i ids] (map-indexed vector columns)
                       :let [date (nth dates i)
                             key (iso-date date)]
                       [j id] (map-indexed vector ids)]
                   ($ cell {:key (str key "-" id)
                            :name (notes/name-for notes id)
                            :done? (boolean (core-store/covered? completions id key))
                            :future? (pos? (compare key today-key))
                            :on-click #(toggle id date)
                            :row (+ row j)
                            :col (+ 2 i)})))))))))

;; ── Entry ────────────────────────────────────────────────────────────────────

(def grid-query "(min-width: 1100px)")

(defhook use-wide-enough? []
  (let [[wide? set-wide!] (use-state #(.-matches (js/matchMedia grid-query)))]
    (use-effect
     (fn []
       (let [mq (js/matchMedia grid-query)
             on-change #(set-wide! (.-matches mq))]
         (.addEventListener mq "change" on-change)
         #(.removeEventListener mq "change" on-change)))
     [])
    wide?))

(defhook use-week-grid []
  (let [wide? (use-wide-enough?)
        [active? set-active!] (use-state false)]
    (keybinding/use-hotkey (keymap/key-of :toggle-grid) #(when wide? (set-active! not)))
    (keybinding/use-hotkey (keymap/key-of :dismiss) #(set-active! false))
    (and wide? active?)))

(defui view [{:keys [today schedule categories notes]}]
  (let [[completions toggle] (use-grid-store today schedule (mapv first categories))]
    ($ :div {:class "mx-auto w-full max-w-[1500px] px-8 pb-16"}
       ($ two-block-grid {:today today :schedule schedule :categories categories
                          :notes notes :completions completions :toggle toggle}))))
