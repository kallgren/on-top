(ns app.notes
  "The Notes file: one global Markdown document holding every Core/Rare task's
   display name and optional note, keyed by task id. `parse` turns the Markdown
   into an `id → {:name :note}` lookup per docs/notes-format.md; the rest mirrors
   app.schedule — a remote gist override (SWR) over a compiled-in seed floor.
   Enrichment, never structure: it warns and degrades, never hard-fails. See
   docs/adr/0005."
  (:require [clojure.string :as str]))

;; ── Parsing ──────────────────────────────────────────────────────────────────

(def ^:private heading-re #"^[ \t]*#[ \t]+(.*)$")
(def ^:private id-re #"^[a-z0-9-]+$")

(def ^:private bom "\uFEFF")

(defn- normalize [s]
  (let [s (or s "")
        s (if (str/starts-with? s bom) (subs s 1) s)]
    (str/replace s "\r\n" "\n")))

(defn- fence-line? [line]
  (let [t (str/trim line)]
    (or (str/starts-with? t "```")
        (str/starts-with? t "~~~"))))

(defn- code-spans
  "The backtick code spans on a heading line, in order, each as
   `{:whole \"`x`\" :group \"x\" :index n}`."
  [text]
  (let [re (js/RegExp. "`([^`]*)`" "g")]
    (loop [acc []]
      (if-let [m (.exec re text)]
        (recur (conj acc {:whole (aget m 0) :group (aget m 1) :index (.-index m)}))
        acc))))

(defn- parse-heading
  "The `{:id :name}` for a heading's text. id = the last code span (when it
   matches the id charset); name = the text with that span removed and trimmed,
   falling back to the id. A heading with no valid id yields `{:id nil}`."
  [text]
  (if-let [{:keys [whole group index]} (last (code-spans text))]
    (if (re-matches id-re group)
      (let [name (str/trim (str (subs text 0 index) (subs text (+ index (count whole)))))]
        {:id group :name (if (str/blank? name) group name)})
      {:id nil})
    {:id nil}))

(defn- note-of
  "The note for a definition's body lines: outer blank lines stripped, interior
   left verbatim. nil when the body is empty or all whitespace."
  [body]
  (let [trimmed (->> body
                     (drop-while str/blank?)
                     reverse
                     (drop-while str/blank?)
                     reverse)]
    (when (seq trimmed)
      (str/join "\n" trimmed))))

(defn- add-definition [m {:keys [heading body]}]
  (let [{:keys [id name]} (parse-heading heading)]
    (if (nil? id)
      (do (js/console.warn "on-top: notes — skipping heading with no valid id —" heading)
          m)
      (let [note (note-of body)]
        (when (contains? m id)
          (js/console.warn "on-top: notes — duplicate id, last wins —" id))
        (assoc m id (cond-> {:name name} note (assoc :note note)))))))

(defn parse
  "Parse a Notes-file Markdown string into `{id {:name :note}}`. Diagnostics warn
   and degrade per docs/notes-format.md; never throws."
  [s]
  (let [lines (vec (.split (normalize s) "\n"))]
    (loop [lines        lines
           in-fence?    false
           open-def     nil
           closed-defs  []]
      (if (empty? lines)
        (reduce add-definition {} (cond-> closed-defs open-def (conj open-def)))
        (let [line    (first lines)
              fence?  (fence-line? line)
              heading (and (not in-fence?) (not fence?) (re-matches heading-re line))]
          (if heading
            (recur (rest lines) in-fence?
                   {:heading (second heading) :body []}
                   (cond-> closed-defs open-def (conj open-def)))
            (recur (rest lines)
                   (if fence? (not in-fence?) in-fence?)
                   (when open-def (update open-def :body conj line))
                   closed-defs)))))))

;; ── Resolution + fetch (mirrors app.schedule) ────────────────────────────────

(defn resolve-notes [cached seed]
  (or cached seed))

(defn notes-source
  "Resolve the Notes source from its inputs: the lookup to paint now — last-good
   cached Markdown (parsed) over the compiled-in seed floor — and the gist `:url`
   to revalidate from, or nil when unconfigured. A cache that yields no
   definitions (corrupt or non-notes text) falls through to the floor rather than
   shadowing it, so the seed always paints names — never raw ids."
  [{:keys [config-url cached seed]}]
  {:initial (resolve-notes (some-> cached parse not-empty) seed)
   :url     (not-empty config-url)})

(defn fetch-notes! [url on-ok]
  (-> (js/fetch url)
      (.then (fn [res]
               (if (.-ok res)
                 (.text res)
                 (throw (js/Error. (str "HTTP " (.-status res)))))))
      (.then (fn [raw]
               (if-let [parsed (not-empty (parse raw))]
                 (on-ok raw parsed)
                 (throw (js/Error. "no task definitions")))))
      (.catch (fn [err]
                (js/console.warn "on-top: ignoring remote notes —"
                                 (.-message err))))))
