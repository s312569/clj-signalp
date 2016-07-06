(ns clj-signalp.core
  (:require [clj-fasta.core :as fa]
            [biodb.core :as bdb]
            [clj-commons-exec :refer [sh]]
            [me.raynes.fs :as fs]
            [clojure.string :as st]
            [clojure.java.io :as io]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; running signalp
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- signal-command
  [params infile]
  (let [ak ["-s" "-t" "-u" "-U" "-M" "-c"]
        up (flatten (vec (select-keys params ak)))]
    (doseq [w (keys (apply dissoc params ak))]
      (println (str "Warning: " w " parameter ignored.")))
    (-> (cons "signalp" up)
        vec
        (conj (str infile)))))

(defn signalp
  "Runs signalp on a collection of protein fasta sequences (see
  clj-fasta) and returns a collection of result files. Processes the
  sequences 10,000 at a time and outputs the results to outfile in the
  signalp 'short' format."
  [coll outfile & {:keys [params] :or {params {}}}]
  (let [c (atom 0)
        fl (atom [])]
    (try
      (doall
       (map #(let [in (fs/absolute (fa/fasta->file % (fs/temp-file "signalp-")))
                   o (fs/absolute (str outfile "-" (swap! c inc) ".signalp"))]
               (swap! fl conj o)
               (try
                 (with-open [out (io/output-stream o)]
                   (let [sp @(sh (signal-command params (str in))
                                 {:out out} :close-err? false)]
                     (if (and (= 0 (:exit sp)) (nil? (:err sp)))
                       o
                       (if (:err sp)
                         (throw (Exception. (str "SignalP error: " (:err sp))))
                         (throw (Exception. (str (:exception sp))))))))
                 (finally (fs/delete in))))
            (partition-all 10000 coll)))
      (catch Exception e
        (doseq [f @fl] (fs/delete f))
        (throw e)))))

(defn signalp-file
  "Runs signalp on each protein in a file of fasta formatted protein
  sequences. Process the sequences 10,000 at a time and outputs the
  results to outfile in the signalp 'short' format."
  [file outfile & {:keys [params] :or {params {}}}]
  (with-open [r (io/reader file)]
    (signalp (fa/fasta-seq r) outfile :params params)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; parsing
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- parse-line
  [line]
  (let [ks [:accession :Cmax :pos :Ymax :pos :Smax :pos :Smean :D
            :result :Dmaxcut :networks]
        f (st/split line #"\s+")]
    (into {} (map (fn [k v] (vector k v)) ks f))))

(defn signalp-seq
  "Takes a reader opened on a signalp results file (in the 'short'
  format) and returns and lazy list of maps representing the signalp
  result for each protein."
  [reader]
  (->> (line-seq reader)
       (drop 2)
       (map parse-line)))

(defn filter-signalp
  "Takes a collection of fasta protein sequences (see clj-fasta) and
  filters those sequences with a signal sequence. Is 'semi-lazy' as it
  processes 1000 sequence chunks of the collection at a time."
  [coll & {:keys [params] :or {params {}}}]
  (->> (pmap #(let [o (fs/temp-file "signal-p-out")
                    rf (first (signalp % o :params params))
                    rset (with-open [r (io/reader rf)]
                           (->> (signalp-seq r)
                                (filter (fn [x] (= (:result x) "Y")))
                                (map :accession)
                                set))]
                (try
                  (doall (filter (fn [x] (rset (:accession x))) %))
                  (finally
                    (fs/delete o))))
             (partition-all 1000 coll))
       (reduce concat)))
