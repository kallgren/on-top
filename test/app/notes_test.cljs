(ns app.notes-test
  (:require [cljs.test :refer [deftest is]]
            [app.notes :as notes]))

(defn- with-captured-warnings
  "Run `f`, returning the vector of arg-lists passed to console.warn while it ran."
  [f]
  (let [warnings (atom [])
        original js/console.warn]
    (set! js/console.warn (fn [& args] (swap! warnings conj (vec args))))
    (try (f) (finally (set! js/console.warn original)))
    @warnings))

;; ── Parsing: names ───────────────────────────────────────────────────────────

(deftest parse-reads-multiple-definitions
  (is (= {"gmail"    {:name "Gmail inbox"}
          "calendar" {:name "Calendar (2w/4w)"}}
         (notes/parse "# Gmail inbox `gmail`\n# Calendar (2w/4w) `calendar`\n"))))

(deftest parse-name-is-the-heading-minus-the-last-span
  (is (= {"gmail" {:name "Gmail inbox"}}
         (notes/parse "# Gmail inbox `gmail`\n"))))

(deftest parse-id-only-heading-falls-back-to-the-id-as-name
  (is (= {"downloads" {:name "downloads"}}
         (notes/parse "# `downloads`\n"))))

(deftest parse-keeps-earlier-spans-in-the-name-and-takes-the-last-as-id
  (is (= {"deploy" {:name "Run `make` then `deploy`"}}
         (notes/parse "# Run `make` then `deploy` `deploy`\n")))
  (is (= {"id" {:name "keep `this`"}}
         (notes/parse "# keep `this` `id`\n"))))

;; ── Parsing: notes ───────────────────────────────────────────────────────────

(deftest parse-attaches-a-two-line-note
  (is (= {"gmail" {:name "Gmail inbox"
                   :note "Alternativ:\n- Snooze eller Todoist"}}
         (notes/parse "# Gmail inbox `gmail`\nAlternativ:\n- Snooze eller Todoist\n"))))

(deftest parse-preserves-a-subheading-inside-a-note
  (is (= {"calendar" {:name "Calendar"
                      :note "Syfte.\n\n## Var\n- Google Calendar"}}
         (notes/parse "# Calendar `calendar`\nSyfte.\n\n## Var\n- Google Calendar\n"))))

(deftest parse-preserves-a-comment-line-inside-a-fenced-block
  (is (= {"deploy" {:name "Deploy script"
                    :note "Run this:\n\n```bash\n# build first\nmake build\n```"}}
         (notes/parse (str "# Deploy script `deploy`\n"
                           "Run this:\n\n```bash\n# build first\nmake build\n```\n")))))

(deftest parse-does-not-split-on-a-heading-inside-an-unterminated-fence
  (is (= {"a" {:name "A" :note "```\n# Not a heading `b`"}}
         (notes/parse "# A `a`\n```\n# Not a heading `b`\n"))))

(deftest parse-strips-outer-blank-lines-but-keeps-interior-blanks
  (is (= {"x" {:name "X" :note "first\n\nlast"}}
         (notes/parse "# X `x`\n\n\nfirst\n\nlast\n\n\n"))))

(deftest parse-empty-or-whitespace-body-yields-no-note
  (is (= {"x" {:name "X"}} (notes/parse "# X `x`\n")))
  (is (= {"x" {:name "X"}} (notes/parse "# X `x`\n   \n\t\n"))))

;; ── Parsing: structure ───────────────────────────────────────────────────────

(deftest parse-ignores-preamble-before-the-first-heading
  (is (= {"x" {:name "X"}}
         (notes/parse "A title or comment.\n- still preamble\n# X `x`\n"))))

(deftest parse-of-an-empty-file-or-no-headings-is-an-empty-map
  (is (= {} (notes/parse "")))
  (is (= {} (notes/parse nil)))
  (is (= {} (notes/parse "just prose\nno headings here\n"))))

;; ── Normalisation ────────────────────────────────────────────────────────────

(deftest parse-normalises-crlf-line-endings
  (is (= {"gmail" {:name "Gmail" :note "line one\nline two"}}
         (notes/parse "# Gmail `gmail`\r\nline one\r\nline two\r\n"))))

(deftest parse-strips-a-leading-bom
  (is (= {"gmail" {:name "Gmail"}}
         (notes/parse "﻿# Gmail `gmail`\n"))))

;; ── Diagnostics ──────────────────────────────────────────────────────────────

(deftest parse-skips-a-heading-with-no-or-malformed-id
  (let [warnings (with-captured-warnings
                   (fn []
                     (is (= {} (notes/parse "# No code span here\n")))
                     (is (= {} (notes/parse "# Bad `Not_An_Id`\n")))
                     (is (= {} (notes/parse "# Empty span ``\n")))))]
    (is (= 3 (count warnings)))))

(deftest parse-last-definition-wins-on-a-duplicate-id-with-a-warning
  (let [warnings (with-captured-warnings
                   (fn []
                     (is (= {"gmail" {:name "Second"}}
                            (notes/parse "# First `gmail`\n# Second `gmail`\n")))))]
    (is (= 1 (count warnings)))))

(deftest parse-multiple-spans-is-silent-and-the-last-is-the-id
  (let [warnings (with-captured-warnings
                   (fn []
                     (is (= {"id" {:name "a `b`"}} (notes/parse "# a `b` `id`\n")))))]
    (is (empty? warnings))))

;; ── Resolution (mirrors schedule-source) ─────────────────────────────────────

(deftest notes-source-paints-cached-markdown-over-the-seed-floor
  (let [seed {"gmail" {:name "Seed"}}]
    (is (= {"gmail" {:name "Cached"}}
           (:initial (notes/notes-source
                      {:config-url "https://gist.example/notes"
                       :cached     "# Cached `gmail`\n"
                       :seed       seed}))))))

(deftest notes-source-falls-back-to-the-seed-floor-without-a-cache
  (let [seed {"gmail" {:name "Seed"}}]
    (is (= seed (:initial (notes/notes-source
                           {:config-url "https://gist.example/notes" :cached nil :seed seed})))
        "no cache → the seed floor")
    (is (= seed (:initial (notes/notes-source
                           {:config-url "https://gist.example/notes" :cached "no headings here" :seed seed})))
        "a cache yielding no definitions never poisons the painted lookup")))

(deftest notes-source-targets-the-configured-gist-url
  (is (= "https://gist.example/notes"
         (:url (notes/notes-source
                {:config-url "https://gist.example/notes" :cached nil :seed {}})))))

(deftest notes-source-has-no-url-when-unconfigured
  (is (nil? (:url (notes/notes-source {:config-url nil :cached nil :seed {}}))))
  (is (nil? (:url (notes/notes-source {:config-url "" :cached nil :seed {}})))))
