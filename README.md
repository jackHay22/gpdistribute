# GP Distribute

A Clojure library designed to distribute Clojush Genetic Programming Evaluations to multiple hosts

## Usage

### Adding gpdistribute to Clojush
```clojure

;RECOMMENDED NAMESPACE: clojush.pushgp.pushgp

;get distribution function
; (starts services and returns distribution "hook")
;optionally takes config overrides
(let [distributor (gpdistribute/get-distribution-fn)]
  (dorun (map #((if use-single-thread swap! send)
                %1 evaluate-individual distributor %2
                        (assoc argmap :reuse-errors false))
              pop-agents
              rand-gens)))

(when-not use-single-thread (apply await pop-agents)) ;; SYNCHRONIZE

```

### Configuring gpdistribute on eval workers
```clojure

;define some evaluation function that takes a Clojush individual
; and returns that individual with errors

(let [my-eval-fn #(eval-indiv %)]
    ;start distribution service, optionally pass config overrides
    (gpdistribute/start-eval-worker my-eval-fn))
```

### Using gpdistribute with a stateful population
- In certain cases, the evaluation of individuals in a population relies on the rest of the population for a generation. In these cases, gpdistribute will handle the distribution of individuals as well as the rest of the population to evaluation workers
- Here is an example usage:

### gpdistribute configuration options
- Here are the various configuration options available to override.  See `io.jackhay.gpdistribute.impl.config` for more details.
  - `stateful?` Evaluation requires the rest of the population, use population distribution
  - `verbose?`  Print log messages
  - `status-task?`  Log the system status (threads, population size, progress)
  - `status-task-delay` Delay between status messages in seconds
  - `buffer-size` Async channel buffer size
  - `engine-hostname` Hostname where clojush is running.  For docker swarm (recommended) this is the service name.
  - `eval-hostname` Hostname for eval workers. For docker swarm (running eval replicas), this is the service name.  Docker swarm handles ingress load balancing based on this hostname.
  - `indiv-ingress-p` Port for sending individuals to workers
  - `indiv-egress-p`  Port for returning individuals from workers
  - `pop-req-p` Port for requesting the rest of the population

## License

Copyright © 2019 Jack Hay

Distributed under the Eclipse Public License 1.0
