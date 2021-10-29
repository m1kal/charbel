(ns charbel-cljs.core
  (:require [charbel.core :refer [module-from-string build]]
            [reagent.core :as r]
            [reagent.dom :as d]))

(def example
  (str
    "(module delay_line\n        {:clocks [[clk]] :parameters [WIDTH 32]}\n"
    "        [[:in data WIDTH]    [:in valid 1]\n        [:out data_o WIDTH] [:out valid_o 1]]\n"
    "        (register data_d data)\n        (register valid_d (init 0 valid))\n"
    "        (assign valid_o valid_d)\n        (assign data_o data_d))"))

(defonce input-form (r/atom example))
(def output-form (r/atom "<empty>"))

(defn main []
  [:div {:style {:background-color "rgb(200,240,200)" :width 1280 :padding-left 20 :padding-top 1 :padding-bottom 20}}
   [:h1 "Charbel online"]
   [:div "Write synthesizable FPGA code using Clojure syntax. The code is translated to SystemVerilog."]
   [:div "Project source code:" [:a {:href "https://github.com/m1kal/charbel"} "https://github.com/m1kal/charbel"] "."]
   [:h4 "Write your code below"]
    [:table
      [:tbody
        [:tr
           [:td {:style {:vertical-align "top"}}
             [:textarea
               {:id "i" :style {:width 600 :height 400} :value @input-form
                :on-change #(reset! input-form (.-value (.-target %)))}]]
          [:td
            {:style {:position "relative" :left 30 :width 600
                     :vertical-align "top" :background-color "rgb(200,220,220)"}}
            [:pre @output-form]]]
        [:tr
          [:td
            [:button
              {:id "run" :on-click #(reset! output-form (build (module-from-string @input-form)))}
              "Convert"]]
          [:td ""]]]]])

(defn mount-root []
  (d/render [main] (.getElementById js/document "main")))

(defn init! []
  (mount-root))

(init!)
