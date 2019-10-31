(ns vegafx.core
  (:require
   [fn-fx.fx-dom :as dom]
   [fn-fx.controls :as ui]
   [fn-fx.diff :refer [component defui render should-update?]]
   [fn-fx.util :refer [run-later]]
   [vegafx.template :as template]))

(def initial-state {:current-url nil})

(defonce app-state (atom initial-state))
(defonce app-ui-state (agent nil))

(defmulti handle-event (fn [state {:keys [event]}] event))

(defn get-engine! []
  (.getEngine (.lookup (.getScene @(:root @app-ui-state)) "#web-browser")))

(defn load! [url]
  (fn-fx.util/run-and-wait
   (.load (get-engine!) url)))

;; engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) ->
;;{if (newState == Worker.State.SUCCEEDED)
;;    {// new page has loaded, process:
;;     testMethod();}})

(defn change-fn [f]
  (reify javafx.beans.value.ChangeListener
    (changed [this obs oldv newv]
      (f obs oldv newv))))

(defn watch-load! [f]
  (fn-fx.util/run-and-wait
   (-> (get-engine!)
       (.getLoadWorker)
       (.stateProperty)
       (.addListener
        (change-fn (fn [obs old-state new-state]
                     (when (= new-state javafx.concurrent.Worker$State/SUCCEEDED) 
                       (f obs old-state new-state))))))))

(defn load-url [url]
  (let [engine (get-engine!)]
    (run-later
     (.load engine url))))

(defmethod handle-event :load-url
  [state {:keys [fn-fx/includes]}]
  (let [new-url (get-in includes [:url-field :text])]
    (load-url new-url)
    (assoc-in state [:current-url] new-url)))

#_(defui WebBrowser
  (render [this args]
          (ui/v-box
           ;;:style "-fx-base: rgb(30, 30, 35);"
           :padding (ui/insets
                     :top-right-bottom-left 25)
           :children [(ui/text-field
                       :id :url-field
                       :prompt-text "Url: "
                       :font (ui/font :family "Helvetica" :size 20)
                       :on-action {:event :load-url
                                   :fn-fx/include {:url-field #{:text}}})
                      (ui/web-view :id "web-browser")])))

(defui WebBrowser
  (render [this args]
    (ui/web-view :id "web-browser")))

(defui MainStage
  (render [this args]
          (ui/stage :title "Web Browser Main Stage"
                    :min-width (get args :min-width 1024)
                    :min-height (get args :min-height 768)
                    :shown true
                    :scene (ui/scene :root (web-browser args)))))

;;some of this is extraneous, likely chopped since we're not using
;;a browser.
(defn ui-event-handler [event]
  (try
    (swap! app-state handle-event event)
    (catch Throwable ex
      (println (str "Error updating app data state! " ex)))))

(defn update-ui-state-agent [old-ui]
  (try
    (dom/update-app old-ui (main-stage @app-state))
  (catch Throwable ex
    (println (str "Error updating app UI state! " ex)))))

(defn run-webview-app
  ([args]
   (let [u (main-stage args)
         ui-state (dom/app (main-stage @app-state) ui-event-handler)]
     (send app-ui-state (fn [old-state]  ui-state))
     (add-watch app-state :ui (fn [key atom old-state new-state]
                                (send app-ui-state #'update-ui-state-agent)))
     (watch-load! (fn [& args] (println :loaded-page!)))))
  ([] (run-webview-app {})))

(defn view-chart [spec & {:keys [min-width min-height] :or {min-width 1024 min-height 768} :as args}]
  (run-webview-app args)
  (load! (str (clojure.java.io/as-url (template/spit-chart spec)))))

(defn -main [& args]
  (run-webview-app))
