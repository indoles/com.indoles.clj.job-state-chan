# jobs-state-chan

A Clojure library that provides a core async based job queue

## Usage

(def q (init))
(send-queue-job #(println %1) q)
(state q)
(stop q)

## License

Copyright © 2014 Indoles

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
