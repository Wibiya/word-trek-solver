# tomthought/word-trek-solver

[![Current Version](https://img.shields.io/clojars/v/word-trek-solver.svg)](https://clojars.org/word-trek-solver)
[![Circle CI](https://circleci.com/gh/tomthought/word-trek-solver.svg?style=shield)](https://circleci.com/gh/tomthought/word-trek-solver)
[![Dependencies Status](https://jarkeeper.com/tomthought/word-trek-solver/status.svg)](https://jarkeeper.com/tomthought/word-trek-solver)

A fast solver for games in the Boggle family, e.g. WordBrain, Work
Trek Trek, etc. Given a puzzle to solve, this library will compute all
possible words that can be spelled (i.e. it can provide word hints).

## Usage

```clojure
;; solve for all words in the provided matrix that are of length 3, 6 or 7.
word-trek-solver.core> (solve "ezmrnubeslospaon" [3 6 7])
=> ("embolus" "loo" "saloon" "saloons" "sub" "ens" "pal" "plumbs"
"nos" "lap" "alb" "sum" "sob" "sap" "sol" "lob" "reb" "bus" "eon"
"rem" "zen" "emu" "usa" "looser" "plumes" "lbs" "number" "spa" "boa"
"mer" "alp" "sal" "sun" "unloose" "nub" "boo" "nooser" "bon" "pas"
"bum" "salons" "sue" "plumber" "asp" "son" "slumber" "lumber" "bun"
"nob")
```

## License

Copyright © 2017 Tom Goldsmith

Distributed under the Eclipse Public License 1.0
