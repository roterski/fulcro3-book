(ns app.ui
  (:require
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.dom :as dom]
    [com.fulcrologic.fulcro.algorithms.denormalize :as fdn]
    [com.fulcrologic.fulcro.data-fetch :as df]
    [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr :refer [defrouter]]
    [taoensso.timbre :as log]
    [app.mutations :as api]))


(defn make-comment
  "Make a comment data map with optional children."
  [id name children]
  (cond-> {:db/id id :comment/name name}
    children (assoc :comment/children children)))

(declare ui-comment)

; The ... in the query means there will be children of the same type, of arbitrary depth
; it is equivalent to (comp/get-query Comment), but calling get query on yourself would
; lead to infinite compiler recursion.
(defsc Comment [this {:keys [:comment/name :comment/children]}]
  {:query         (fn [] [:db/id :comment/name {:comment/children '...}])
   :initial-state (fn [p]
                    (make-comment 1 "Joe"
                      [(make-comment 2 "Suzy" [])
                       (make-comment 3 "Billy" [])
                       (make-comment 4 "Rae"
                         [(make-comment 5 "Ian"
                            [(make-comment 6 "Zoe" [])])])]))
   :ident         [:comment/id :db/id]}
  (dom/div
    (dom/h4 name)
    (when (seq children)
      (dom/div
        (dom/ul
          (map (fn [p]
                 (ui-comment p))
            children))))))

(def ui-comment (comp/factory Comment {:keyfn :db/id}))

(defsc Person [this {:person/keys [id name age] :as props} {:keys [onDelete]}]
  {:query [:person/id :person/name :person/age]
   :ident (fn [] [:person/id (:person/id props)])}
  (dom/li
    (dom/h5 (str name " (age: " age ")")
      (dom/button {:onClick #(dr/change-route this ["person" id])} "Show")
      (dom/button {:onClick #(onDelete id)} "X"))))

(def ui-person (comp/factory Person {:keyfn :person/id}))

(defsc PersonList [this {:person-list/keys [id label people] :as props}]
       {:query         [:person-list/id :person-list/label {:person-list/people (comp/get-query Person)}]
        :ident         :person-list/id
        :route-segment ["list" :person-list/id]
        ;:will-enter (fn [_ {:person-list/keys [id]}]
        ;              (dr/route-immediate [:person-list/id (keyword id)]))}
        :will-enter    (fn [app {:person-list/keys [id]}]
                         (let [id (keyword id)]
                           (dr/route-deferred [:person-list/id id]
                             #(df/load app [:person-list/id id] PersonList
                                       {:post-mutation `dr/target-ready
                                        :post-mutation-params {:target [:person-list/id id]}}))))}
  (let [delete-person (fn [person-id] (comp/transact! this [(api/delete-person {:person-list/id id :person/id person-id})]))]
    (dom/div
      (dom/div
        (dom/h4 label)
        (dom/ul
          (map (fn [p] (ui-person (comp/computed p {:onDelete delete-person}))) people))))))


(defsc Welcome [this {:keys [comments]}]
  {:query         [{:comments (comp/get-query Comment)}]
   :initial-state {:comments {}}
   :ident         (fn [] [:component/id :main])
   :route-segment ["home"]
   :will-enter    (fn [_ _] (dr/route-immediate [:component/id :main]))}
  (dom/div :.ui.container.segment
    (dom/h3 "Home")
    (dom/p "Hello there!")
    (ui-comment comments)))

(defsc PersonShow [this {:person/keys  [id name age]}]
  {:query           [:person/id :person/name :person/age]
   :ident           :person/id
   :route-segment   ["person" :person/id]
   :will-enter      (fn [app {:person/keys [id] :as route-params}]
                      (let [id (if (string? id) (js/parseInt id) id)]
                        (dr/route-deferred [:person/id id]
                          #(df/load app [:person/id id] Person
                             {:post-mutation `dr/target-ready
                              :post-mutation-params
                                             {:target [:person/id id]}}))))}
  (dom/div
    (dom/h3 (str "Person " id))
    (dom/div (str name))
    (dom/div (str "age: " age))))

(defrouter Router [this props]
  {:router-targets [Welcome PersonList PersonShow]})

(def ui-router (comp/factory Router))

(defsc Root [this {:root/keys [router]}]
  {:query [{:root/router (comp/get-query Router)}]
   :initial-state {:root/router {}}}
  (dom/div
    (dom/button {:onClick #(dr/change-route this ["home"])} "Home")
    (dom/button {:onClick #(dr/change-route this ["list" "friends"])} "Friends")
    (dom/button {:onClick #(dr/change-route this ["list" "enemies"])} "Enemies")
    (ui-router router)))
