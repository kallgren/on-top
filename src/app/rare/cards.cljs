(ns app.rare.cards)

(defn partition-tasks [tasks]
  (let [sorted (sort-by :sort-key tasks)]
    {:completed (filter :done? sorted)
     :current   (remove #(or (:upcoming? %) (:done? %)) sorted)
     :upcoming  (filter :upcoming? sorted)}))

(defn build-cards
  "Per-category card data for Rare. `by-category` is the store's category->rows
   fn, `categories` the ordered [cat label] pairs, `expanded` the fold-state map
   {cat {:completed? bool :upcoming? bool}}. Drops empty categories."
  [by-category categories expanded]
  (vec (for [[cat label] categories
             :let [cat-rows (by-category cat)]
             :when (seq cat-rows)]
         (let [{:keys [completed current upcoming]} (partition-tasks cat-rows)
               exp (get expanded cat)]
           {:cat cat :label label
            :completed completed :current current :upcoming upcoming
            :show-completed? (:completed? exp)
            :show-upcoming?  (:upcoming? exp)}))))

(defn visible-rows
  "The flat row vector the keyboard Cursor walks, in traversal order: each card's
   `current` rows, plus an expanded fold's rows (Completed above, Upcoming below).
   A collapsed fold contributes nothing — keyboard nav never auto-expands."
  [cards]
  (vec (mapcat (fn [{:keys [completed current upcoming show-completed? show-upcoming?]}]
                 (concat (when show-completed? completed)
                         current
                         (when show-upcoming? upcoming)))
               cards)))
