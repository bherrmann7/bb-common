;; #!/usr/bin/env bb

;; A wee mult-threaded web server

;; Usage
;;
;; (start-web-server (fn [path] (str "Yo, " path)))
;;
;; Note: server runs in own thread, good for using emacs/cider to eval
;; handler function when running, but in a script need await forever
;; at end.
;; (while true (Thread/sleep 1000))

(ns wee-httpd)

(import (java.net ServerSocket))
(require '[clojure.string :as string]
  '[clojure.java.io :as io])

(defn create-worker-thread [client-socket request-handler]
  (.start
    (Thread.
      (fn [] (println "Worker: I'm off and working..." client-socket)
        (with-open [out (io/writer (.getOutputStream client-socket))
                    in (io/reader (.getInputStream client-socket))
                    ]
          (loop []
            (let [[req-line & headers] (loop [headers []]
                                         (let [line (.readLine in)]
                                           (if (string/blank? line)
                                             headers
                                             (recur (conj headers line)))))]
              (if (not (nil? req-line))
                (let [[_ _ path _] (re-find #"([^\s]+)\s([^\s]+)\s([^\s]+)" req-line)
                      status 200
                      body (request-handler path)
                      ]
                  (.write out (format "HTTP/1.1 %s OK\r\nContent-Length: %s\r\n\r\n%s"
                                status
                                (count body)
                                body))
                  (.flush out)
                  (recur)))))
          (println "Worker: I'm all done with this one" client-socket) 
          )))))

(defn start-web-server [request-handler]
  (.start (Thread. #(with-open
                     [server-socket (new ServerSocket 7777)]
                     (while true
                       (let [_ (println "Serv: Awaiting connection...")
                             client-socket (.accept server-socket)]
                         (println "Serv: Accepted connection!")
                         (create-worker-thread client-socket request-handler )))))))


;; Tells emacs to jump into clojure mode.
;; Local Variables:
;; mode: clojure
;; End:
