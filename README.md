# clj-signalp

A Clojure library for running SignalP and parsing the results.

## Installation

Leiningren: [clj-signalp "0.1.1"]

To include in your project: (:require [clj-signalp.core :as sp])

## Usage

To run signalP (assuming the signalp binary is in your path) on a
collection of fasta-sequences (see `clj-fasta`):
```clojure
user> (with-open [r (io/reader "/path/to/fasta-file.fasta")]
        (signalp (fasta-seq r) "/path/out-prefix"))
(#object[java.io.File 0x1e6855b2 "/path/out.prefix-1.signalp"])
user>
```
Results are sent to the outfile in the signalP 'short' format.

To run signalP on a file of fasta formatted protein sequences use
`signalp-file` which returns a file object containing the results in
the signalP 'short' format:
```clojure
user> (signalp-file "/path/to/fasta-file.fasta" "/path/out-prefix")
(#object[java.io.File 0x1e6855b2 "/path/out.prefix-1.signalp"])
user>
``` 

To parse a signalP results file that is in the signalP 'short' format
use `signalp-seq` which returns a lazy list of maps with the result of
signalP on each protein:
```clojure
user> (with-open [r (io/reader "path/to/result/file")]
        (doall (->> (signalp-seq r)
                    (take 2))))
({:Cmax "0.116", :Smax "0.146", :Ymax "0.116", :D "0.118",
 :Smean "0.119", :pos "1", :networks "SignalP-noTM", :result "N",
 :accession "c10010_g1_i1|m.3253", :Dmaxcut "0.450"} {:Cmax "0.112",
 :Smax "0.115", :Ymax "0.107", :D "0.105", :Smean "0.102", :pos "50",
 :networks "SignalP-noTM", :result "N", :accession "c10035_g1_i1|m.3256",
 :Dmaxcut "0.450"})
user> 
```

If you have a collection of fasta sequences, `filter-signalp` will the
filter sequences containing a signal sequence:
```clojure
user> (with-open [r (io/reader "/path/to/fasta-file.fasta")]
                    (filter-signalp (fa/fasta-seq r)))
({:accession "c10692_g1_i1|m.3373", :description "c10692_g1_i1|g.3373 
 ORF c10692_g1_i1|g.3373 c10692_g1_i1|m.3373 type:5prime_partial len:153
 (+) c10692_g1_i1:3-461(+)", :sequence "GTGILSIGSALLGADLVFGFDVDLNSIETAQK
SARDRGLLGVEFIRIDVRRVGRLRKFRGTVDTVVMNPPFGTRLRGADFCFIEAAVKISKGNIYSLHKTSTRN
QLVKKIKRNLSRETRALAELRFDLAKSYKFHKMKEKEILVDLLAVLAE"} {:accession 
"c11063_g1_i1|m.3444", :description "c11063_g1_i1|g.3444  ORF
 c11063_g1_i1|g.3444 c11063_g1_i1|m.3444 type:internal len:118 (-)
 c11063_g1_i1:2-352(-)", :sequence "KTKSFFLMLLLLGGDIESNPGPTTCQICKQIQQTEE
ENVCSICQNCMVEGPSQATIVIDDADTRPTYEKQPIEQPEKPPQIFTDTKSINNVYQIDPTHPIIPSEHYSN
YLRQFENIK"})
user> 
```

All three functions take a :params keyword where some arguments to
signalP can be specified ("-s" "-t" "-u" "-U" "-M" "-c"):
```clojure
user> (with-open [r (io/reader "/path/to/fasta-file.fasta")]
                    (filter-signalp (fa/fasta-seq r)
                                    :params {"-t" "euk"}))
({:accession "c10692_g1_i1|m.3373"," ... })
user> 
```

## License

Copyright © 2016 Jason Mulvenna

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
