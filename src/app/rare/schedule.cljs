(ns app.rare.schedule
  (:require [app.date-utils :refer [parse-month-day iso-date iso->date days-between]]))

(defn- interval->step [interval]
  (case interval
    "2 weeks"  {:days 14}
    "monthly"  {:months 1}
    "2 months" {:months 2}
    "quarterly" {:months 3}
    "6 months" {:months 6}
    "yearly"   {:months 12}
    {:months 1}))

(defn- step-date [date {:keys [months days]}]
  (let [d (js/Date. (.getTime date))]
    (if months
      (.setMonth d (+ (.getMonth d) months))
      (.setDate d (+ (.getDate d) days)))
    d))

(defn- negate-step [{:keys [months days]}]
  (if months {:months (- months)} {:days (- days)}))

;; ── Occurrence helpers ───────────────────────────────────────────────────────

(defn- latest-due
  "Largest occurrence ≤ ceiling. Phase 1 steps back while above the ceiling;
  phase 2 advances while the next occurrence is still within it."
  [anchor-d step ceiling]
  (let [ceil-t (.getTime ceiling)
        back   (negate-step step)
        d      (loop [d anchor-d]
                 (if (> (.getTime d) ceil-t)
                   (recur (step-date d back))
                   d))]
    (loop [d d]
      (let [nxt (step-date d step)]
        (if (<= (.getTime nxt) ceil-t)
          (recur nxt)
          d)))))

(defn- pending-after
  "Occurrences in (from-d, ceiling], ascending. `from-d` is itself an occurrence
  (a done-through date), so we just step forward from it."
  [from-d step ceiling]
  (let [ceil-t (.getTime ceiling)]
    (loop [d (step-date from-d step) acc []]
      (if (<= (.getTime d) ceil-t)
        (recur (step-date d step) (conj acc d))
        acc))))

;; ── Collapse derivation ──────────────────────────────────────────────────────

(defn- due-label
  "Countdown shown on a deadline occurrence `days` out, or nil outside the
  0–7 day window — further out reads as not-yet-urgent."
  [days]
  (when (<= 0 days 7)
    (cond
      (= days 0) "Due today"
      (= days 1) "Due tomorrow"
      :else      (str "Due in " days " days"))))

(defn- due?
  "Whether a deadline occurrence `days` out is pressing: inside the 0–7 day
  countdown or already overdue (negative). Ordinary tasks (zero lead) never are."
  [lead-days days]
  (and (pos? lead-days) (<= days 7)))

(defn task-rows
  "Collapse one task to at most two rows (one per visible section) given its
  `done-through-iso` (or nil) and today. Placement by done-through (dt) vs today:

  | done-through        | current                          | completed (inline) | upcoming      |
  |---------------------|----------------------------------|--------------------|---------------|
  | nil (never done)    | latest occ ≤ today, no missed    | —                  | —             |
  | behind (dt < due)   | earliest pending occ + X missed  | the dt occurrence  | —             |
  | caught up (dt ≥ due)| —                                | the dt occurrence  | next future occ |

  Lead time (`:lead-days N`) shifts only the current-ness threshold: occurrences
  are measured against an effective ceiling of `today + lead-days`, so a deadline
  task surfaces as current once it enters its lead window. The displayed date is the
  occurrence date itself; `:due-label` carries the ready-to-render countdown string
  and `:due?` flags the pressing (countdown-or-overdue) rows the badge counts.

  Each row declares the `:set-done-through` it writes on click, so the toggle handler
  is a plain `(assoc completions id set-done-through)` with no branching on row type."
  [category freq task done-through-iso today]
  (let [{:keys [id anchor lead-days]} task
        lead-days (or lead-days 0)
        step      (interval->step freq)
        anchor-d  (parse-month-day anchor (.getFullYear today))
        lead-ceil (js/Date. (+ (.getTime today) (* lead-days 86400000)))
        ld        (latest-due anchor-d step lead-ceil)
        dt        (when done-through-iso (iso->date done-through-iso))
        behind?   (and dt (< (.getTime dt) (.getTime ld)))
        caught?   (and dt (>= (.getTime dt) (.getTime ld)))
        pending   (when behind? (pending-after dt step lead-ceil))
        row       (fn [role occ extra]
                    (let [days-out (days-between today occ)]
                      (merge
                       {:id          id
                        :category    category
                        :freq        freq
                        :display-iso (iso-date occ)
                        :sort-key    (.getTime occ)
                        :due-label   (when (pos? lead-days) (due-label days-out))
                        :due?        (due? lead-days days-out)
                        :done?       false
                        :upcoming?   false
                        :missed      0
                        :key         (str id "/" role)}
                       extra)))]
    (cond-> []
      ;; completed (inline) — emitted whenever a done-through exists
      dt        (conj (row "done" dt
                           {:done?            true
                            :due-label        nil
                            :due?             false
                            :set-done-through (iso-date (step-date dt (negate-step step)))}))
      ;; current — nil floor or behind
      (nil? dt) (conj (row "cur" ld
                           {:missed           0
                            :set-done-through (iso-date ld)}))
      behind?   (conj (row "cur" (first pending)
                           {:missed           (dec (count pending))
                            :set-done-through (iso-date ld)}))
      ;; upcoming — caught up
      caught?   (conj (row "up" (step-date dt step)
                           {:upcoming?        true
                            :set-done-through (iso-date (step-date dt step))})))))

(defn derive-schedule
  "Flat seq of collapsed rows over every category/interval/task in `schedule`,
  looking up each task's done-through in `completions` by id. Rows carry no
  display name; names are joined on at the Rare boundary. See `task-rows` for the
  placement model."
  [schedule completions today]
  (for [[category intervals] schedule
        [freq tasks] intervals
        task tasks
        row (task-rows category freq task (get completions (:id task)) today)]
    row))
