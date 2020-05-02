#!/usr/bin/env bb

;; A wee multi-threaded web server

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
                                    (count (.getBytes body))
                                    body))
                (.flush out)
                (recur)))))
        (println "Worker: I'm all done with this one" client-socket) 
        )))))

(defn start-web-server [request-handler]
  (.start (Thread. #(with-open
                      [server-socket (new ServerSocket 4444)]
                      (while true
                        (let [_ (println "Serv: Awaiting connection on port " (.getLocalPort server-socket))
                              client-socket (.accept server-socket)]
                          (println "Serv: Accepted connection!")
                          (create-worker-thread client-socket request-handler )))))))


;; I use bootstrap, it's pretty.
(def header "
<html>
  <head>
    <link rel='stylesheet' href='https://stackpath.bootstrapcdn.com/bootstrap/4.4.1/css/bootstrap.min.css' integrity='sha384-Vkoo8x4CGsO3+Hhxv8T/Q5PaXtkKtu6ug5TOeNV6gBiFeWPGFN9MuhOf23Q9Ifjh' crossorigin='anonymous'>
    <script src='https://code.jquery.com/jquery-3.4.1.slim.min.js' integrity='sha384-J6qa4849blE2+poT4WnyKhv5vZF5SrPo0iEjwBvKU7imGFAV0wwj1yYfoRSJoZ+n' crossorigin='anonymous'></script>
    <script src='https://cdn.jsdelivr.net/npm/popper.js@1.16.0/dist/umd/popper.min.js' integrity='sha384-Q6E9RHvbIyZFJoft+2mJbHaEWldlvI9IOYy5n3zV9zzTtmI3UksdQRVvoxMfooAo' crossorigin='anonymous'></script>
    <script src='https://stackpath.bootstrapcdn.com/bootstrap/4.4.1/js/bootstrap.min.js' integrity='sha384-wfSDF2E50Y2D1uUdj0O3uMBJnjuUD4Ih7YwaYd1iqfktj0Uod8GCExl3Og8ifwB6' crossorigin='anonymous'></script>
  </head>
  <body>
<nav class='navbar navbar-expand-lg navbar-dark bg-primary'>
  <a class='navbar-brand' href='#'>Dev Console</a>
  <button class='navbar-toggler' type='button' data-toggle='collapse' data-target='#navbarSupportedContent' aria-controls='navbarSupportedContent' aria-expanded='false' aria-label='Toggle navigation'>
    <span class='navbar-toggler-icon'></span>
  </button>

  <div class='collapse navbar-collapse' id='navbarSupportedContent'>
    <ul class='navbar-nav mr-auto'>
      <li class='nav-item active'>
        <a class='nav-link' href='/'>Home <span class='sr-only'>(current)</span></a>
      </li>
      <li class='nav-item active'>
        <a class='nav-link' href='/about'>About <span class='sr-only'>(current)</span></a>
      </li>
     </ul>
   </div>
</nav>    
<br>
<div class='container-fluid'>
    <div class='row'>
      <div class='col-lg-12'>

")


(def footer  "
</div> <!-- class='col-lg-12'> -->
</div> <!--class='row' -->
  </div> <!--  class='container' -->
</body>
</html>")


(defn page [ & content ]
  (string/join (concat [header] content [footer])))

(defn go-away-page [ & args ]
  "Kindly, go away."
  )

(defn about-page [ & args ]
  (page  "About this site"))

(defn home-page [ & args ]
  (page  "A simple web server written in babashka")) 


;; A thing for matching a path request with a path expression;
;; ie (path-mather "/j*" "/joe") => true
(defn path-matcher [ path-expr path ]
  (println "path-matcher" path-expr path )
  (let [match 
        (if (.endsWith path-expr "*")
          (if (>= (count path) (dec (count path-expr)))
            (let [ sm-path-expr (.substring path-expr 0 (dec (count path-expr)))
                  sm-path (.substring path 0 (count sm-path-expr))
                  _ (println "comparing expr:" sm-path-expr " with sm-path:" sm-path)
                  ]
              (= sm-path-expr sm-path))
            false)
          (= path path-expr))]
    (println "path-matcher" path-expr path match)
    match))

(defn router [path]
  ((condp path-matcher path
     "/" home-page
     "/about" about-page
     "/favicon.ico" go-away-page
     go-away-page
     )))


(wee-httpd/start-web-server (fn[path] (router path)))

;; Keep the shell script from returning.  When evaling the whole buffer, comment this out.
(while true (Thread/sleep 1000))

;; Tells emacs to jump into clojure mode.
;; Local Variables:
;; mode: clojure
;; End:
