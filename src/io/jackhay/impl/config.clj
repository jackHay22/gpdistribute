(ns io.jackhay.gpdistribute.impl.config)

;default configuration options
;-----------------------------

;if this is true, eval workers will
;request the remainder of the population
;for use during evaluation each generation
; (occurs once per gen)
(def STATEFUL false)

;show distribute specific log messages
(def VERBOSE true)
;status task uses a thread to log information
;given a certain delay
(def STATUS-TASK false)
(def STATUS-DELAY 600) ;seconds

;async channel buffer size (for holding queued individuals)
;note: clojush agents block on evaluation so a
; worker *shouldn't* have this many individuals waiting for
; eval at any given time
(def BUFFER-SIZE 100)

;hostnames (i.e. docker service names)
;clojush hostname (for returning individuals)
(def ENGINE-HOSTNAME "engine")

;service name for eval.  Note: in the future,
; this library may support a list of hosts for simple
; round robin load balancing. For now, the use of docker
; swarm is recommended for cluster orchestration
; this should be the service name of the evaluation containers.
; docker swarm will then take care of ingress load balancing
(def EVAL-HOSTNAME "eval")

;ports
;this is the port that the engine tries to distribute
; individuals to.  Note, this should be open on evaluation
; nodes/containers
(def INDIV-INGRESS-P 9999)
; this is the port that evaluation workers try to send
; evaluated individuals back to.  Note: this should be open
; on the engine host/container
(def INDIV-EGRESS-P 8000)
;this is the port that evaluation workers use in stateful
; mode to request the remainder of the population once per
; generation. Note: this port should be open on the engine
; host/container
(def POP-REQ-P 8888)
