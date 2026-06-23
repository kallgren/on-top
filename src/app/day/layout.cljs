(ns app.day.layout
  "Pure water-fill geometry for the Day timetable. Block heights are
   duration-proportional but floored at 44px so short blocks stay readable; the
   floored blocks are fixed and the remaining height is split proportionally
   among the rest, so the whole day fills the available height. The now-line
   offset is interpolated within the block spanning the current minute, and is
   absent when that minute falls outside the day's span.")

(def min-h 44)

(defn ->min [s]
  (let [[h m] (map js/parseInt (.split s ":"))]
    (+ (* 60 h) m)))

(defn min-str [m]
  (let [h (quot m 60) mm (mod m 60)]
    (str h ":" (when (< mm 10) "0") mm)))

(defn total-h [laid]
  (if (seq laid) (+ (:top (last laid)) (:height (last laid))) 0))

(defn offset-at [laid m]
  (some (fn [b]
          (when (and (>= m (:s b)) (<= m (:e b)))
            (+ (:top b) (* (/ (- m (:s b)) (- (:e b) (:s b))) (:height b)))))
        laid))

(defn fit-layout [bs avail]
  (let [base (map (fn [b]
                    (let [s (->min (:start b)) e (->min (:end b))]
                      (assoc b :s s :e e :dur (- e s))))
                  bs)
        floored (loop [fixed #{}]
                  (let [free (remove #(fixed (:id %)) base)
                        a (max 0 (- avail (* min-h (count fixed))))
                        free-dur (reduce + (map :dur free))
                        newly (keep (fn [b]
                                      (when (< (/ (* (:dur b) a) (max 1 free-dur)) min-h)
                                        (:id b)))
                                    free)]
                    (if (and (seq newly) (< (count fixed) (count base)))
                      (recur (into fixed newly))
                      fixed)))
        a (max 0 (- avail (* min-h (count floored))))
        free-dur (reduce + (map :dur (remove #(floored (:id %)) base)))
        height-of (fn [b] (if (floored (:id b))
                            min-h
                            (/ (* (:dur b) a) (max 1 free-dur))))]
    (loop [bs base top 0 acc []]
      (if (empty? bs)
        acc
        (let [b (first bs) h (height-of b)]
          (recur (rest bs) (+ top h) (conj acc (assoc b :top top :height h))))))))
